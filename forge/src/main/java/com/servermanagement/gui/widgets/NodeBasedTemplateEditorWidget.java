package com.servermanagement.gui.widgets;

import com.servermanagement.features.economy.TaskComponent;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NodeBasedTemplateEditorWidget extends AbstractWidget {

    // Node dimensions
    private static final int NODE_WIDTH = 160;
    private static final int NODE_HEADER_HEIGHT = 22;
    private static final int NODE_MIN_HEIGHT = 140;
    private static final int CONNECTOR_RADIUS = 5;
    
    // Colors
    private static final int NODE_BG = 0xFF1A1A2E;
    private static final int NODE_BORDER = 0xFF333333;
    private static final int NODE_HEADER_BG = 0xFF2A2A48;
    private static final int GOLD = 0xFFFFD700;
    private static final int CONNECTOR_DEFAULT = 0xFF666666;
    private static final int CONNECTOR_HOVER = 0xFFFFD700;
    private static final int CONNECTOR_CONNECTED = 0xFF4A9EFF;
    private static final int CONNECTION_LINE = 0xFF4A9EFF;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int GRID_DOT = 0xFF222233;
    private static final int GRID_SPACING = 24;
    private static final int GRID_SPACING_LOD = 48; // 2x spacing for far-field dots
    private static final int MOUSE_INFLUENCE_RADIUS_SQ = 120 * 120;
    private static final float MOUSE_INFLUENCE_INV = 1.0f / 120.0f;
    private static final int LOD_NEAR_RADIUS_SQ = 180 * 180; // Full-density grid zone
    
    // 3D world-projection constants (fullscreen mode)
    private static final float WORLD_DOT_MAX_DISTANCE = 40.0f; // blocks
    private static final int WORLD_GRID_SPACING = 24;           // screen-px between raycasts
    private static final int WORLD_GRID_SPACING_LOD = 48;       // coarse far-field spacing
    private static final float WORLD_DOT_NEAR_DIST = 8.0f;     // full brightness within 8 blocks
    private static final float WORLD_DOT_FAR_FADE = 32.0f;     // fade out beyond 32 blocks
    
    // Pre-computed sin lookup table (256 entries, one full cycle)
    private static final float[] SIN_TABLE = new float[256];
    static {
        for (int i = 0; i < 256; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * (2.0 * Math.PI / 256.0));
        }
    }
    
    private final Font font;
    private int tickCount = 0;
    private float precomputedPinPulse = 0.5f;
    private Set<Node> connectedOutputNodes = new HashSet<>();
    private Set<Node> connectedInputNodes = new HashSet<>();
    private boolean fullscreen = false;
    
    // Core Graph Data
    private List<Node> nodes = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();
    
    // Hardcoded Node limits
    private static final int MAX_TASK_NODES = 5;
    private RootNode rootNode;
    private RewardNode rewardNode;
    
    // Drag/Drop Interaction State
    private Node draggingFromNode = null;
    private boolean draggingFromOutput = false; // true if output pin, false if input pin
    private double dragMouseX, dragMouseY;
    private Pin hoveredPin = null;
    private Runnable onCameraToggle;
    private Connection previewInsertion = null;
    
    public void setOnCameraToggle(Runnable callback) {
        this.onCameraToggle = callback;
    }
    
    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public NodeBasedTemplateEditorWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Node Editor"));
        this.font = Minecraft.getInstance().font;
        
        // Initialize Base Nodes
        rootNode = new RootNode(x + 20, y + height / 2 - 40);
        rewardNode = new RewardNode(x + width - 180, y + height / 2 - 40);
        
        nodes.add(rootNode);
        nodes.add(rewardNode);
    }
    
    private void clearOtherFocus(Node activeNode) {
        for (Node n : nodes) {
            if (n != activeNode) {
                n.clearFocus();
            }
        }
    }

    public void tick() {
        tickCount++;
        // Pre-compute pin pulse animation value once per tick
        precomputedPinPulse = (float) (0.5 + 0.5 * SIN_TABLE[(tickCount * 6) & 255]);
    }
    
    // --- Data Management for Save/Load ---
    
    public List<TaskComponent> getComponents() {
        List<TaskComponent> components = new ArrayList<>();
        
        // Trace graph starting from root
        Node current = getConnectedOutput(rootNode);
        int infiniteLoopGuard = 0;
        while (current != null && current instanceof TaskNode && infiniteLoopGuard < MAX_TASK_NODES) {
            TaskNode tn = (TaskNode) current;
            components.add(new TaskComponent(tn.getSelectedTaskType(), tn.getGoal(), tn.getDescription()));
            current = getConnectedOutput(tn);
            infiniteLoopGuard++;
        }
        
        return components;
    }
    
    private Node getConnectedOutput(Node node) {
        for (Connection c : connections) {
            if (c.from == node) {
                return c.to;
            }
        }
        return null;
    }
    
    public List<net.minecraft.world.item.ItemStack> getRewardItems() {
        return rewardNode.getRewardItems();
    }
    
    public double getRewardAmount() {
        return rewardNode.getRewardAmount();
    }
    
    public void setTemplateData(List<TaskComponent> components, double rewardAmount, List<net.minecraft.world.item.ItemStack> rewardItems) {
        nodes.clear();
        connections.clear();
        
        rootNode = new RootNode(getX() + 20, getY() + 20);
        nodes.add(rootNode);
        
        Node previous = rootNode;
        int spacing = 200;
        int cx = getX() + spacing;
        
        for (TaskComponent comp : components) {
            if (nodes.size() - 2 >= MAX_TASK_NODES) break;
            
            TaskNode tn = new TaskNode(cx, getY() + 20);
            tn.setTaskType(comp.getType());
            tn.setGoal(comp.getTargetAmount());
            tn.setDescription(comp.getCustomDescription());
            
            nodes.add(tn);
            connections.add(new Connection(previous, tn));
            previous = tn;
            cx += spacing;
        }
        
        rewardNode = new RewardNode(cx, getY() + 20);
        rewardNode.setRewardAmount(rewardAmount);
        rewardNode.setRewardItems(rewardItems);
        nodes.add(rewardNode);
        
        connections.add(new Connection(previous, rewardNode));
    }

    // --- Rendering ---
    
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (fullscreen) {
            renderWorldProjectedDots(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            renderGridDots(guiGraphics, mouseX, mouseY);
        }
        
        hoveredPin = null;
        
        // Rebuild connectivity cache once per frame
        connectedOutputNodes.clear();
        connectedInputNodes.clear();
        for (Connection c : connections) {
            connectedOutputNodes.add(c.from);
            connectedInputNodes.add(c.to);
        }
        
        // Update hovered pin
        for (Node n : nodes) {
            if (n.hasInput && isNearConnectorSq(mouseX, mouseY, n.getInputX(), n.getInputY())) {
                hoveredPin = new Pin(n, false);
            }
            if (n.hasOutput && isNearConnectorSq(mouseX, mouseY, n.getOutputX(), n.getOutputY())) {
                hoveredPin = new Pin(n, true);
            }
        }
        
        // Draw standard connections
        for (Connection c : connections) {
            renderBezierLine(guiGraphics, c.from.getOutputX(), c.from.getOutputY(), c.to.getInputX(), c.to.getInputY(), CONNECTION_LINE);
        }
        
        // Draw dragging connection
        if (draggingFromNode != null) {
            int startX = draggingFromOutput ? draggingFromNode.getOutputX() : draggingFromNode.getInputX();
            int startY = draggingFromOutput ? draggingFromNode.getOutputY() : draggingFromNode.getInputY();
            
            if (draggingFromOutput) {
                renderBezierLine(guiGraphics, startX, startY, mouseX, mouseY, 0x88FFD700);
            } else {
                renderBezierLine(guiGraphics, mouseX, mouseY, startX, startY, 0x88FFD700);
            }
        }
        
        // Draw preview insertion
        if (previewInsertion != null) {
            for(Node n : nodes) {
                if(n.isDragging) {
                    renderBezierLine(guiGraphics, previewInsertion.from.getOutputX(), previewInsertion.from.getOutputY(), n.getInputX(), n.getInputY(), 0xFF00FF00);
                    renderBezierLine(guiGraphics, n.getOutputX(), n.getOutputY(), previewInsertion.to.getInputX(), previewInsertion.to.getInputY(), 0xFF00FF00);
                    break;
                }
            }
        }
        
        // Draw nodes
        for (Node n : nodes) {
            n.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Draw add node button if space exists
        int taskNodeCount = nodes.size() - 2;
        if (taskNodeCount < MAX_TASK_NODES) {
            int btnX = getX() + getWidth() / 2 - 50;
            int btnY = getY() + getHeight() - 30;
            guiGraphics.fill(btnX, btnY, btnX + 100, btnY + 20, NODE_HEADER_BG);
            guiGraphics.drawString(font, Component.literal("+ Add Task Node"), btnX + 10, btnY + 6, GOLD, false);
        }
    }
    
    /**
     * Fullscreen mode: project dots onto the 3D world by raycasting from the
     * camera through a screen-space grid. Each hit block surface gets a dot
     * rendered at the projected 2D screen position. Distance-based LOD and
     * alpha fade create a holographic wireframe overlay that follows terrain
     * contours, letting the player see the world geometry behind the editor.
     */
    private void renderWorldProjectedDots(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            // Fallback to flat grid if no world context
            renderGridDots(guiGraphics, mouseX, mouseY);
            return;
        }
        
        Entity cameraEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        Vec3 eyePos = cameraEntity.getEyePosition(partialTick);
        
        // Camera rotation vectors
        float yawRad = (float) Math.toRadians(cameraEntity.getViewYRot(partialTick));
        float pitchRad = (float) Math.toRadians(cameraEntity.getViewXRot(partialTick));
        
        float cosYaw = (float) Math.cos(-yawRad - Math.PI);
        float sinYaw = (float) Math.sin(-yawRad - Math.PI);
        float cosPitch = (float) Math.cos(-pitchRad);
        float sinPitch = (float) Math.sin(-pitchRad);
        
        // Forward, right, up vectors in world space
        Vector3f forward = new Vector3f(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        Vector3f right = new Vector3f(cosYaw, 0, -sinYaw);
        Vector3f up = new Vector3f();
        right.cross(forward, up); // up = right x forward
        
        // Screen dimensions and FOV
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        double fov = mc.options.fov().get();
        float tanHalfFov = (float) Math.tan(Math.toRadians(fov * 0.5));
        float aspect = (float) screenW / (float) screenH;
        
        int animOffset = tickCount * 3;
        
        // Iterate screen-space grid and raycast each point
        int startX = getX();
        int startY = getY();
        int endX = startX + getWidth();
        int endY = startY + getHeight();
        
        for (int gx = startX; gx < endX; gx += WORLD_GRID_SPACING) {
            int dxMouse = gx - mouseX;
            int dxMouseSq = dxMouse * dxMouse;
            
            // Normalized device coordinate X: [-1, 1]
            float ndcX = (2.0f * gx / screenW - 1.0f) * tanHalfFov * aspect;
            
            for (int gy = startY; gy < endY; gy += WORLD_GRID_SPACING) {
                int dyMouse = gy - mouseY;
                int distSqMouse = dxMouseSq + dyMouse * dyMouse;
                
                // LOD: skip every-other dot in far field from mouse
                if (distSqMouse > LOD_NEAR_RADIUS_SQ) {
                    if (((gx - startX) % WORLD_GRID_SPACING_LOD != 0) || ((gy - startY) % WORLD_GRID_SPACING_LOD != 0)) {
                        continue;
                    }
                }
                
                // Normalized device coordinate Y: [-1, 1] (inverted for screen)
                float ndcY = -(2.0f * gy / screenH - 1.0f) * tanHalfFov;
                
                // Ray direction in world space
                float rdx = forward.x() + right.x() * ndcX + up.x() * ndcY;
                float rdy = forward.y() + right.y() * ndcX + up.y() * ndcY;
                float rdz = forward.z() + right.z() * ndcX + up.z() * ndcY;
                
                // Normalize
                float len = (float) Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
                if (len < 1e-6f) continue;
                rdx /= len; rdy /= len; rdz /= len;
                
                // Raycast endpoint
                Vec3 rayEnd = eyePos.add(rdx * WORLD_DOT_MAX_DISTANCE, rdy * WORLD_DOT_MAX_DISTANCE, rdz * WORLD_DOT_MAX_DISTANCE);
                
                BlockHitResult hit = mc.level.clip(new ClipContext(
                    eyePos, rayEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    cameraEntity
                ));
                
                if (hit.getType() == HitResult.Type.MISS) continue;
                
                // Distance-based alpha (near=bright, far=faded)
                double hitDist = hit.getLocation().distanceTo(eyePos);
                if (hitDist > WORLD_DOT_MAX_DISTANCE) continue;
                
                float distAlpha;
                if (hitDist <= WORLD_DOT_NEAR_DIST) {
                    distAlpha = 1.0f;
                } else if (hitDist >= WORLD_DOT_FAR_FADE) {
                    distAlpha = Math.max(0.0f, 1.0f - (float)(hitDist - WORLD_DOT_FAR_FADE) / (WORLD_DOT_MAX_DISTANCE - WORLD_DOT_FAR_FADE));
                } else {
                    distAlpha = 1.0f - 0.4f * (float)((hitDist - WORLD_DOT_NEAR_DIST) / (WORLD_DOT_FAR_FADE - WORLD_DOT_NEAR_DIST));
                }
                
                // Subtle time fluctuation
                int sinIndex = ((animOffset + gx * 2 + gy * 3) >> 2) & 255;
                float timeFluctuation = SIN_TABLE[sinIndex] * 0.1f + 0.9f;
                
                // Mouse proximity highlight
                float mouseHighlight;
                if (distSqMouse >= MOUSE_INFLUENCE_RADIUS_SQ) {
                    mouseHighlight = 0.0f;
                } else {
                    float distNorm = distSqMouse * (MOUSE_INFLUENCE_INV * MOUSE_INFLUENCE_INV);
                    mouseHighlight = 1.0f - (float) Math.sqrt(distNorm);
                }
                
                float alpha = distAlpha * timeFluctuation * 0.6f + mouseHighlight * 0.4f;
                if (alpha < 0.05f) continue;
                alpha = Math.min(1.0f, alpha);
                
                // Color based on hit face direction for depth perception
                int baseColor;
                switch (hit.getDirection()) {
                    case UP:    baseColor = 0x7AC5FF; break; // bright sky-blue for top faces
                    case DOWN:  baseColor = 0x2A5A8F; break; // deep blue for bottom
                    default:    baseColor = 0x4A9EFF; break; // medium blue for sides
                }
                int color = ((int)(alpha * 255) << 24) | baseColor;
                
                // Dot size: close hits get 2px, far get 1px
                if (hitDist < 12.0 && mouseHighlight > 0.3f) {
                    guiGraphics.fill(gx, gy, gx + 2, gy + 2, color);
                } else {
                    guiGraphics.fill(gx, gy, gx + 1, gy + 1, color);
                }
            }
        }
    }
    
    private void renderGridDots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = getX();
        int startY = getY();
        int endX = startX + getWidth();
        int endY = startY + getHeight();
        int animOffset = tickCount * 3; // Tick-based animation instead of System.currentTimeMillis()
        
        for (int gx = startX; gx < endX; gx += GRID_SPACING) {
            int dxMouse = gx - mouseX;
            int dxMouseSq = dxMouse * dxMouse;
            
            for (int gy = startY; gy < endY; gy += GRID_SPACING) {
                int dyMouse = gy - mouseY;
                int distSq = dxMouseSq + dyMouse * dyMouse;
                
                // LOD: skip every-other dot in far field (beyond LOD_NEAR_RADIUS_SQ)
                if (distSq > LOD_NEAR_RADIUS_SQ) {
                    // Only render dots on the coarse 48px sub-grid
                    if (((gx - startX) % GRID_SPACING_LOD != 0) || ((gy - startY) % GRID_SPACING_LOD != 0)) {
                        continue;
                    }
                }
                
                // Sin lookup table for time fluctuation (no Math.sin)
                int sinIndex = ((animOffset + gx * 2 + gy * 3) >> 2) & 255;
                float timeFluctuation = SIN_TABLE[sinIndex] * 0.2f + 0.8f;
                
                // Squared distance mouse highlight (no Math.sqrt)
                float mouseHighlight;
                if (distSq >= MOUSE_INFLUENCE_RADIUS_SQ) {
                    mouseHighlight = 0.0f;
                } else {
                    // Fast inverse sqrt approximation via linear falloff on squared distance
                    float distNorm = distSq * (MOUSE_INFLUENCE_INV * MOUSE_INFLUENCE_INV);
                    mouseHighlight = 1.0f - (float) Math.sqrt(distNorm);
                }
                
                float alpha = 0.3f * timeFluctuation + 0.7f * mouseHighlight;
                if (alpha < 0.08f) continue; // Cull invisible dots early
                alpha = Math.min(1.0f, alpha);
                int color = ((int)(alpha * 255) << 24) | 0x4A9EFF;
                
                if (mouseHighlight > 0.8f) {
                    guiGraphics.fill(gx, gy, gx + 2, gy + 2, color);
                } else {
                    guiGraphics.fill(gx, gy, gx + 1, gy + 1, color);
                }
            }
        }
    }
    
    private boolean isNearConnectorSq(double mouseX, double mouseY, int cx, int cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double r = CONNECTOR_RADIUS + 4;
        return (dx * dx + dy * dy) <= r * r;
    }
    
    private void renderBezierLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // LOD: adaptive segment count based on connection pixel length
        int dx = x2 - x1;
        int dy = y2 - y1;
        int lengthSq = dx * dx + dy * dy;
        int segments = lengthSq < 40000 ? 6 : 12; // <200px = 6 segments, >=200px = 12
        
        int cp1x = x1 + dx / 3;
        int cp2x = x1 + 2 * dx / 3;
        
        float prevX = x1, prevY = y1;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            float invT = 1 - t;
            
            float bx = invT * invT * invT * x1 + 3 * invT * invT * t * cp1x + 3 * invT * t * t * cp2x + t * t * t * x2;
            float by = invT * invT * invT * y1 + 3 * invT * invT * t * y1 + 3 * invT * t * t * y2 + t * t * t * y2;
            
            int px1 = (int) prevX, py1 = (int) prevY;
            int px2 = (int) bx, py2 = (int) by;
            
            if (Math.abs(px2 - px1) >= Math.abs(py2 - py1)) {
                guiGraphics.fill(Math.min(px1, px2), (py1 + py2) / 2 - 1, Math.max(px1, px2) + 1, (py1 + py2) / 2 + 1, color);
            } else {
                guiGraphics.fill((px1 + px2) / 2 - 1, Math.min(py1, py2), (px1 + px2) / 2 + 1, Math.max(py1, py2) + 1, color);
            }
            
            prevX = bx;
            prevY = by;
        }
    }
    
    // --- Input Handling ---
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        
        // Must grab focus for Screen to forward mouseDragged events to us
        if (mouseX >= getX() && mouseX <= getX() + getWidth() && 
            mouseY >= getY() && mouseY <= getY() + getHeight()) {
            this.setFocused(true);
        }
        
        // Add Node Button
        int taskNodeCount = nodes.size() - 2;
        if (taskNodeCount < MAX_TASK_NODES) {
            int btnX = getX() + getWidth() / 2 - 50;
            int btnY = getY() + getHeight() - 30;
            if (mouseX >= btnX && mouseX <= btnX + 100 && mouseY >= btnY && mouseY <= btnY + 20) {
                int newX = getX() + getWidth() / 2 - NODE_WIDTH / 2 + (taskNodeCount * 25);
                int newY = getY() + getHeight() / 2 - NODE_MIN_HEIGHT / 2 + (taskNodeCount * 25);
                TaskNode tn = new TaskNode(newX, newY);
                nodes.add(nodes.size() - 1, tn); // Insert before reward node
                return true;
            }
        }
        
        // Disconnect on right-click pin
        if (button == 1 && hoveredPin != null) {
            connections.removeIf(c -> (hoveredPin.isOutput && c.from == hoveredPin.node) || (!hoveredPin.isOutput && c.to == hoveredPin.node));
            return true;
        }
        
        // Start dragging
        if (button == 0 && hoveredPin != null) {
            draggingFromNode = hoveredPin.node;
            draggingFromOutput = hoveredPin.isOutput;
            // Disconnect existing if dragging from input
            if (!draggingFromOutput) {
                connections.removeIf(c -> c.to == draggingFromNode);
            } else {
                connections.removeIf(c -> c.from == draggingFromNode);
            }
            return true;
        }
        
        // Node dragging or interaction
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node n = nodes.get(i);
            if (n.mouseClicked(mouseX, mouseY, button)) {
                if (i > 1 && i < nodes.size() - 1) { // Not root or reward
                    nodes.remove(i);
                    nodes.add(nodes.size() - 1, n);
                }
                clearOtherFocus(n);
                return true;
            }
        }
        
        if (button == 1) {
            if (this.onCameraToggle != null) {
                this.onCameraToggle.run();
                return true;
            }
        }
        
        clearOtherFocus(null);
        // Return true for any click within our bounds to prevent container
        // slot-drag interference and maintain focus for drag forwarding
        return mouseX >= getX() && mouseX <= getX() + getWidth() && 
               mouseY >= getY() && mouseY <= getY() + getHeight();
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Node n : nodes) {
            n.mouseReleased(mouseX, mouseY, button);
        }
        
        if (draggingFromNode != null) {
            if (hoveredPin != null && hoveredPin.node != draggingFromNode && hoveredPin.isOutput != draggingFromOutput) {
                Node from = draggingFromOutput ? draggingFromNode : hoveredPin.node;
                Node to = draggingFromOutput ? hoveredPin.node : draggingFromNode;
                
                // Prevent loops (DAG check)
                if (!wouldCreateLoop(from, to)) {
                    connections.removeIf(c -> c.from == from || c.to == to); // Max 1 connection per pin
                    connections.add(new Connection(from, to));
                }
            }
            draggingFromNode = null;
            return true;
        }
        return false;
    }
    
    private boolean hasConnections(Node n) {
        for (Connection c : connections) {
            if (c.from == n || c.to == n) return true;
        }
        return false;
    }
    
    private boolean wouldCreateLoop(Node from, Node to) {
        if (from == to) return true;
        Node current = to;
        int checks = 0;
        while (current != null && checks < nodes.size()) {
            Node next = getConnectedOutput(current);
            if (next == from) return true;
            current = next;
            checks++;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingFromNode != null) {
            dragMouseX = mouseX;
            dragMouseY = mouseY;
            return true;
        }
        for (Node n : nodes) {
            if (n.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Delete Node on DEL key
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            for (int i = 1; i < nodes.size() - 1; i++) { // Skip root and reward
                Node n = nodes.get(i);
                if (n.isHovered) {
                    connections.removeIf(c -> c.from == n || c.to == n);
                    nodes.remove(i);
                    return true;
                }
            }
        }
        for (Node n : nodes) {
            if (n.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Node n : nodes) {
            if (n.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Node n : nodes) {
            if (n.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }

    public boolean isAnyTextFieldFocused() {
        for (Node n : nodes) {
            if (n instanceof TaskNode) {
                if (((TaskNode) n).goalBox.isFocused() || ((TaskNode) n).descBox.isFocused()) return true;
                if (((TaskNode) n).dropdown != null && ((TaskNode) n).dropdown.isExpanded()) return true;
            } else if (n instanceof RewardNode) {
                if (((RewardNode) n).rewardBox.isFocused() || ((RewardNode) n).itemPicker.isSearchBoxFocused()) return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
    
    // --- Inner Classes ---
    
    private record Pin(Node node, boolean isOutput) {}
    
    private static class Connection {
        Node from;
        Node to;
        Connection(Node from, Node to) { this.from = from; this.to = to; }
    }
    
    private abstract class Node {
        int x, y;
        int width = NODE_WIDTH;
        int height = NODE_MIN_HEIGHT;
        boolean hasInput, hasOutput;
        String title;
        boolean isDragging = false;
        boolean isHovered = false;
        double dragOffsetX = 0, dragOffsetY = 0;
        
        Node(int x, int y, String title, boolean hasInput, boolean hasOutput) {
            this.x = x; this.y = y; this.title = title;
            this.hasInput = hasInput; this.hasOutput = hasOutput;
        }
        
        void clearFocus() {}
        
        int getInputX() { return x; }
        int getInputY() { return y + height / 2; }
        int getOutputX() { return x + width; }
        int getOutputY() { return y + height / 2; }
        
        void render(GuiGraphics g, int mx, int my, float pt) {
            isHovered = (mx >= x && mx <= x + width && my >= y && my <= y + height);
            
            g.fill(x, y, x + width, y + height, NODE_BG);
            
            // Single renderOutline call instead of 4 separate fill calls
            int borderColor = isHovered ? GOLD : NODE_BORDER;
            g.renderOutline(x, y, width, height, borderColor);
            
            g.fill(x + 1, y + 1, x + width - 1, y + NODE_HEADER_HEIGHT, NODE_HEADER_BG);
            g.drawString(font, Component.literal(title), x + 8, y + 7, GOLD, false);
            
            if (hasInput) renderPin(g, getInputX(), getInputY(), getPinColor(false));
            if (hasOutput) renderPin(g, getOutputX(), getOutputY(), getPinColor(true));
            
            renderContent(g, mx, my, pt);
        }
        
        int getPinColor(boolean isOutput) {
            if (hoveredPin != null && hoveredPin.node == this && hoveredPin.isOutput == isOutput) {
                // Use pre-computed pulse value instead of Math.sin per pin
                return ((int)(180 + 75 * precomputedPinPulse) << 24) | 0xFFD700;
            }
            // Use cached connectivity sets instead of iterating connections list
            boolean connected = isOutput ? connectedOutputNodes.contains(this) : connectedInputNodes.contains(this);
            return connected ? CONNECTOR_CONNECTED : CONNECTOR_DEFAULT;
        }
        
        void renderPin(GuiGraphics g, int cx, int cy, int color) {
            g.fill(cx - 4, cy - 2, cx + 4, cy + 2, color);
            g.fill(cx - 3, cy - 3, cx + 3, cy + 3, color);
            g.fill(cx - 2, cy - 4, cx + 2, cy + 4, color);
            g.fill(cx - 2, cy - 1, cx + 2, cy + 1, 0xFF000000);
        }
        
        abstract void renderContent(GuiGraphics g, int mx, int my, float pt);
        
        boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0 && my >= y && my <= y + height && mx >= x && mx <= x + width) {
                isDragging = true;
                dragOffsetX = mx - x;
                dragOffsetY = my - y;
                return true;
            }
            return false;
        }
        
        boolean mouseReleased(double mx, double my, int btn) {
            if (isDragging) {
                isDragging = false;
                if (previewInsertion != null) {
                    Connection c = previewInsertion;
                    connections.remove(c);
                    connections.add(new Connection(c.from, this));
                    connections.add(new Connection(this, c.to));
                    previewInsertion = null;
                }
            }
            return false;
        }
        
        boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (isDragging) {
                x = (int)(mx - dragOffsetX);
                y = (int)(my - dragOffsetY);
                
                previewInsertion = null;
                if (!hasConnections(this)) {
                    for (Connection c : connections) {
                        int x1 = c.from.getOutputX();
                        int x2 = c.to.getInputX();
                        int y1 = c.from.getOutputY();
                        int y2 = c.to.getInputY();
                        if (x2 == x1) continue;
                        
                        int nodeCenterX = x + width / 2;
                        int nodeCenterY = y + height / 2;
                        float t = (nodeCenterX - x1) / (float)(x2 - x1);
                        
                        if (t >= 0.05f && t <= 0.95f) {
                            float invT = 1 - t;
                            float expectedY = invT * invT * invT * y1 + 3 * invT * invT * t * y1 + 3 * invT * t * t * y2 + t * t * t * y2;
                            
                            if (Math.abs(nodeCenterY - expectedY) < 60) {
                                y = (int) expectedY - NODE_MIN_HEIGHT / 2;
                                previewInsertion = c;
                                break;
                            }
                        }
                    }
                }
                
                x = Math.max(NodeBasedTemplateEditorWidget.this.getX(), Math.min(x, NodeBasedTemplateEditorWidget.this.getX() + NodeBasedTemplateEditorWidget.this.getWidth() - NODE_WIDTH));
                y = Math.max(NodeBasedTemplateEditorWidget.this.getY(), Math.min(y, NodeBasedTemplateEditorWidget.this.getY() + NodeBasedTemplateEditorWidget.this.getHeight() - NODE_MIN_HEIGHT));
                updateWidgets();
                return true;
            }
            return false;
        }
        
        void updateWidgets() {}
        boolean mouseScrolled(double mx, double my, double sx, double sy) { return false; }
        boolean keyPressed(int k, int s, int m) { return false; }
        boolean charTyped(char c, int m) { return false; }
    }
    
    private class RootNode extends Node {
        RootNode(int x, int y) { super(x, y, "Start Flow", false, true); }
        @Override void renderContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, Component.literal("Trigger Event"), x + 8, y + 40, LABEL_COLOR, false);
        }
    }
    
    private class TaskNode extends Node {
        private DropdownWidget dropdown;
        private EditBox goalBox;
        private EditBox descBox;
        
        TaskNode(int x, int y) {
            super(x, y, "Task Objective", true, true);
            
            List<String> types = new ArrayList<>();
            for(TaskType t : TaskType.values()) types.add(t.getDisplayName());
            
            dropdown = new DropdownWidget(x + 8, y + 30, NODE_WIDTH - 16, 18, Component.literal("Select Type..."));
            dropdown.setOptions(types);
            
            goalBox = new EditBox(font, x + 8, y + 70, NODE_WIDTH - 16, 16, Component.literal("Goal"));
            goalBox.setFilter(s -> s.matches("\\d*"));
            goalBox.setMaxLength(10);
            
            descBox = new EditBox(font, x + 8, y + 110, NODE_WIDTH - 16, 16, Component.literal("Desc"));
            descBox.setMaxLength(100);
            
            updateWidgets();
        }
        
        TaskType getSelectedTaskType() {
            int idx = dropdown.getSelectedIndex();
            return (idx >= 0 && idx < TaskType.values().length) ? TaskType.values()[idx] : TaskType.BREAK_BLOCKS;
        }
        void setTaskType(TaskType t) { if (t != null) dropdown.setSelectedIndex(t.ordinal()); }
        
        int getGoal() { try { return Integer.parseInt(goalBox.getValue()); } catch(Exception e) { return 1; } }
        void setGoal(int g) { goalBox.setValue(String.valueOf(g)); }
        
        String getDescription() { return descBox.getValue(); }
        void setDescription(String s) { if (s != null) descBox.setValue(s); }
        
        @Override void updateWidgets() {
            dropdown.setX(x + 8); dropdown.setY(y + 30);
            goalBox.setX(x + 8); goalBox.setY(y + 70);
            descBox.setX(x + 8); descBox.setY(y + 110);
        }
        
        @Override void renderContent(GuiGraphics g, int mx, int my, float pt) {
            dropdown.render(g, mx, my, pt);
            g.drawString(font, Component.literal("Target Amount:"), x + 8, y + 58, LABEL_COLOR, false);
            goalBox.render(g, mx, my, pt);
            g.drawString(font, Component.literal("Description:"), x + 8, y + 98, LABEL_COLOR, false);
            descBox.render(g, mx, my, pt);
        }
        
        @Override void clearFocus() {
            goalBox.setFocused(false);
            descBox.setFocused(false);
            dropdown.closeDropdown();
        }
        
        @Override boolean mouseClicked(double mx, double my, int btn) {
            if (dropdown.isExpanded() && dropdown.mouseClicked(mx, my, btn)) return true;
            
            boolean clicked = false;
            if (dropdown.mouseClicked(mx, my, btn)) clicked = true;
            else if (goalBox.mouseClicked(mx, my, btn)) { goalBox.setFocused(true); descBox.setFocused(false); clicked = true; }
            else if (descBox.mouseClicked(mx, my, btn)) { descBox.setFocused(true); goalBox.setFocused(false); clicked = true; }
            
            if (clicked) return true;
            
            return super.mouseClicked(mx, my, btn);
        }
        
        @Override boolean keyPressed(int k, int s, int m) {
            if (dropdown.isExpanded() && dropdown.keyPressed(k, s, m)) return true;
            if (goalBox.isFocused() && goalBox.keyPressed(k, s, m)) return true;
            if (descBox.isFocused() && descBox.keyPressed(k, s, m)) return true;
            return false;
        }
        
        @Override boolean charTyped(char c, int m) {
            if (goalBox.isFocused() && goalBox.charTyped(c, m)) return true;
            if (descBox.isFocused() && descBox.charTyped(c, m)) return true;
            return false;
        }
    }
    
    private class RewardNode extends Node {
        private EditBox rewardBox;
        private FreeRewardEditorWidget itemPicker;
        
        RewardNode(int x, int y) {
            super(x, y, "Reward Data", true, false);
            this.height = 240; 
            this.width = 180; 
            
            rewardBox = new EditBox(font, x + 8, y + 50, this.width - 16, 16, Component.literal("Reward"));
            rewardBox.setFilter(s -> s.matches("\\d*"));
            rewardBox.setMaxLength(10);
            
            itemPicker = new FreeRewardEditorWidget(x + 8, y + 70, this.width - 16, 160, new ArrayList<>());
            
            updateWidgets();
        }
        
        void setRewardItems(List<net.minecraft.world.item.ItemStack> items) {
            itemPicker = new FreeRewardEditorWidget(x + 8, y + 70, this.width - 16, 160, items);
        }
        
        List<net.minecraft.world.item.ItemStack> getRewardItems() {
            return itemPicker.getRewardItems();
        }
        
        double getRewardAmount() { try { return Double.parseDouble(rewardBox.getValue()); } catch(Exception e) { return 0; } }
        void setRewardAmount(double d) { rewardBox.setValue(String.valueOf((int)d)); }
        
        @Override void updateWidgets() { 
            rewardBox.setX(x + 8); 
            rewardBox.setY(y + 50); 
            itemPicker.setX(x + 8);
            itemPicker.setY(y + 70);
        }
        
        @Override void renderContent(GuiGraphics g, int mx, int my, float pt) {
            g.drawString(font, Component.literal("Cash Reward ($):"), x + 8, y + 38, LABEL_COLOR, false);
            rewardBox.render(g, mx, my, pt);
            itemPicker.tick(); 
            itemPicker.render(g, mx, my, pt);
        }
        
        @Override void clearFocus() {
            rewardBox.setFocused(false);
            itemPicker.setFocused(false);
        }
        
        @Override boolean mouseClicked(double mx, double my, int btn) {
            if (itemPicker.mouseClicked(mx, my, btn)) return true;
            if (rewardBox.mouseClicked(mx, my, btn)) { rewardBox.setFocused(true); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        
        @Override boolean mouseReleased(double mx, double my, int btn) {
            itemPicker.mouseReleased(mx, my, btn);
            return super.mouseReleased(mx, my, btn);
        }
        
        @Override boolean keyPressed(int k, int s, int m) { 
            if (itemPicker.keyPressed(k, s, m)) return true;
            return rewardBox.isFocused() && rewardBox.keyPressed(k, s, m); 
        }
        @Override boolean charTyped(char c, int m) { 
            if (itemPicker.charTyped(c, m)) return true;
            return rewardBox.isFocused() && rewardBox.charTyped(c, m); 
        }
        
        @Override boolean mouseScrolled(double mx, double my, double sx, double sy) {
            return itemPicker.mouseScrolled(mx, my, sx, sy);
        }
    }
}
