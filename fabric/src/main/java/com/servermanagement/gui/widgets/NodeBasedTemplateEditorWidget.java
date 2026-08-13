package com.servermanagement.gui.widgets;

import com.servermanagement.features.economy.TaskType;
import com.servermanagement.gui.widgets.DropdownWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Node-based visual template editor widget for the Economy Daily Task template system.
 * Inspired by TIA Portal / Unreal Blueprint node editors.
 * 
 * Layout: [Task Type Node] ---> [Task Details Node] ---> [Reward Node]
 * 
 * Each node is a dark card with gold title bar, input/output connector pins on edges,
 * and interactive content inside. Nodes are connected by rendered bezier-approximation lines.
 */
public class NodeBasedTemplateEditorWidget extends AbstractWidget {

    // Node dimensions
    private static final int NODE_WIDTH = 160;
    private static final int NODE_HEADER_HEIGHT = 22;
    private static final int NODE_MIN_HEIGHT = 120;
    private static final int CONNECTOR_RADIUS = 5;
    private static final int NODE_GAP = 50;
    
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
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int GRID_DOT = 0xFF222233;
    
    // State
    private final Font font;
    private DropdownWidget taskTypeDropdown;
    private EditBox descriptionBox;
    private EditBox goalBox;
    private EditBox rewardBox;
    
    // Connection state
    private boolean node1Connected = true; // Task Type → Details auto-connected
    private boolean node2Connected = true; // Details → Reward auto-connected
    private int draggingFromNode = -1; // -1 = none, 0/1/2 = node index
    private boolean draggingFromOutput = false;
    private double dragMouseX, dragMouseY;
    
    // Connector positions (computed during render)
    private int[] node1OutputX = new int[1], node1OutputY = new int[1];
    private int[] node2InputX = new int[1], node2InputY = new int[1];
    private int[] node2OutputX = new int[1], node2OutputY = new int[1];
    private int[] node3InputX = new int[1], node3InputY = new int[1];
    
    // Hover tracking
    private int hoveredConnector = -1; // 0=n1out, 1=n2in, 2=n2out, 3=n3in
    
    // Pulse animation
    private int tickCount = 0;
    
    // Node positions
    private int node1X, node1Y;
    private int node2X, node2Y;
    private int node3X, node3Y;

    public NodeBasedTemplateEditorWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Node Editor"));
        this.font = Minecraft.getInstance().font;
        
        calculateNodePositions();
        initializeWidgets();
    }
    
    private void calculateNodePositions() {
        int totalWidth = NODE_WIDTH * 3 + NODE_GAP * 2;
        int startX = getX() + (getWidth() - totalWidth) / 2;
        int startY = getY() + 15;
        
        node1X = startX;
        node1Y = startY;
        node2X = startX + NODE_WIDTH + NODE_GAP;
        node2Y = startY;
        node3X = startX + (NODE_WIDTH + NODE_GAP) * 2;
        node3Y = startY;
    }
    
    private void initializeWidgets() {
        // Task Type dropdown (inside node 1)
        List<String> taskTypeNames = new ArrayList<>();
        for (TaskType type : TaskType.values()) {
            taskTypeNames.add(type.getDisplayName());
        }
        taskTypeDropdown = new DropdownWidget(
            node1X + 8, node1Y + NODE_HEADER_HEIGHT + 30,
            NODE_WIDTH - 16, 18,
            Component.literal("Select Type...")
        );
        taskTypeDropdown.setOptions(taskTypeNames);
        taskTypeDropdown.setOnSelectionChanged(idx -> {
            // Auto-update the goal hint based on task type
            if (goalBox != null) {
                TaskType selectedType = TaskType.values()[idx];
                goalBox.setHint(Component.literal(selectedType.getDisplayName() + " amount..."));
            }
        });
        
        // Description box (inside node 2)
        descriptionBox = new EditBox(font,
            node2X + 8, node2Y + NODE_HEADER_HEIGHT + 30,
            NODE_WIDTH - 16, 16,
            Component.literal("Description")
        );
        descriptionBox.setMaxLength(100);
        descriptionBox.setHint(Component.literal("Custom description..."));
        descriptionBox.setBordered(true);
        
        // Goal box (inside node 2)
        goalBox = new EditBox(font,
            node2X + 8, node2Y + NODE_HEADER_HEIGHT + 68,
            NODE_WIDTH - 16, 16,
            Component.literal("Goal")
        );
        goalBox.setMaxLength(10);
        goalBox.setHint(Component.literal("Target amount..."));
        goalBox.setFilter(s -> s.matches("\\d*"));
        goalBox.setBordered(true);
        
        // Reward box (inside node 3)
        rewardBox = new EditBox(font,
            node3X + 8, node3Y + NODE_HEADER_HEIGHT + 30,
            NODE_WIDTH - 16, 16,
            Component.literal("Reward")
        );
        rewardBox.setMaxLength(10);
        rewardBox.setHint(Component.literal("Cash reward ($)..."));
        rewardBox.setFilter(s -> s.matches("\\d*"));
        rewardBox.setBordered(true);
    }
    
    public void tick() {
        tickCount++;
    }

    // --- Data Getters for save ---
    
    public TaskType getSelectedTaskType() {
        int idx = taskTypeDropdown.getSelectedIndex();
        TaskType[] values = TaskType.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : TaskType.BREAK_BLOCKS;
    }
    
    public String getDescription() {
        return descriptionBox != null ? descriptionBox.getValue() : "";
    }
    
    public int getGoal() {
        try {
            return goalBox != null ? Integer.parseInt(goalBox.getValue()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public double getRewardAmount() {
        try {
            return rewardBox != null ? Double.parseDouble(rewardBox.getValue()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // --- Data Setters for edit ---
    
    public void setTaskType(TaskType type) {
        if (type != null) {
            taskTypeDropdown.setSelectedIndex(type.ordinal());
        }
    }
    
    public void setDescription(String desc) {
        if (descriptionBox != null && desc != null) {
            descriptionBox.setValue(desc);
        }
    }
    
    public void setGoal(int goal) {
        if (goalBox != null) {
            goalBox.setValue(String.valueOf(goal));
        }
    }
    
    public void setRewardAmount(double reward) {
        if (rewardBox != null) {
            rewardBox.setValue(String.valueOf((int) reward));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Recalculate positions in case of resize
        calculateNodePositions();
        repositionWidgets();
        
        // Render grid background dots
        renderGridDots(guiGraphics);
        
        // Compute connector positions
        computeConnectorPositions();
        
        // Update hover state
        updateHoverState(mouseX, mouseY);
        
        // Render connection lines
        renderConnections(guiGraphics, mouseX, mouseY);
        
        // Render nodes
        renderNode(guiGraphics, node1X, node1Y, "Task Type", true, false, mouseX, mouseY, 0);
        renderNode(guiGraphics, node2X, node2Y, "Task Details", true, true, mouseX, mouseY, 1);
        renderNode(guiGraphics, node3X, node3Y, "Rewards", false, true, mouseX, mouseY, 2);
        
        // Render node contents
        renderNode1Content(guiGraphics, mouseX, mouseY, partialTick);
        renderNode2Content(guiGraphics, mouseX, mouseY, partialTick);
        renderNode3Content(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render drag line if dragging
        if (draggingFromNode >= 0) {
            renderDragLine(guiGraphics, mouseX, mouseY);
        }
    }
    
    private void repositionWidgets() {
        if (taskTypeDropdown != null) {
            taskTypeDropdown.setX(node1X + 8);
            taskTypeDropdown.setY(node1Y + NODE_HEADER_HEIGHT + 30);
        }
        if (descriptionBox != null) {
            descriptionBox.setX(node2X + 8);
            descriptionBox.setY(node2Y + NODE_HEADER_HEIGHT + 30);
        }
        if (goalBox != null) {
            goalBox.setX(node2X + 8);
            goalBox.setY(node2Y + NODE_HEADER_HEIGHT + 68);
        }
        if (rewardBox != null) {
            rewardBox.setX(node3X + 8);
            rewardBox.setY(node3Y + NODE_HEADER_HEIGHT + 30);
        }
    }
    
    private void renderGridDots(GuiGraphics guiGraphics) {
        int spacing = 20;
        for (int gx = getX(); gx < getX() + getWidth(); gx += spacing) {
            for (int gy = getY(); gy < getY() + getHeight(); gy += spacing) {
                guiGraphics.fill(gx, gy, gx + 1, gy + 1, GRID_DOT);
            }
        }
    }
    
    private void computeConnectorPositions() {
        // Node 1 output (right side, vertically centered)
        node1OutputX[0] = node1X + NODE_WIDTH;
        node1OutputY[0] = node1Y + NODE_MIN_HEIGHT / 2;
        
        // Node 2 input (left side) and output (right side)
        node2InputX[0] = node2X;
        node2InputY[0] = node2Y + NODE_MIN_HEIGHT / 2;
        node2OutputX[0] = node2X + NODE_WIDTH;
        node2OutputY[0] = node2Y + NODE_MIN_HEIGHT / 2;
        
        // Node 3 input (left side)
        node3InputX[0] = node3X;
        node3InputY[0] = node3Y + NODE_MIN_HEIGHT / 2;
    }
    
    private void updateHoverState(int mouseX, int mouseY) {
        hoveredConnector = -1;
        if (isNearConnector(mouseX, mouseY, node1OutputX[0], node1OutputY[0])) hoveredConnector = 0;
        else if (isNearConnector(mouseX, mouseY, node2InputX[0], node2InputY[0])) hoveredConnector = 1;
        else if (isNearConnector(mouseX, mouseY, node2OutputX[0], node2OutputY[0])) hoveredConnector = 2;
        else if (isNearConnector(mouseX, mouseY, node3InputX[0], node3InputY[0])) hoveredConnector = 3;
    }
    
    private boolean isNearConnector(double mouseX, double mouseY, int cx, int cy) {
        double dist = Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - cy, 2));
        return dist <= CONNECTOR_RADIUS + 4;
    }
    
    private void renderConnections(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (node1Connected) {
            renderBezierLine(guiGraphics, 
                node1OutputX[0], node1OutputY[0],
                node2InputX[0], node2InputY[0],
                CONNECTION_LINE);
        }
        if (node2Connected) {
            renderBezierLine(guiGraphics, 
                node2OutputX[0], node2OutputY[0],
                node3InputX[0], node3InputY[0],
                CONNECTION_LINE);
        }
    }
    
    /**
     * Render a bezier-approximation connection line using small filled segments.
     */
    private void renderBezierLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int segments = 20;
        int cp1x = x1 + (x2 - x1) / 3;
        int cp2x = x1 + 2 * (x2 - x1) / 3;
        
        float prevX = x1, prevY = y1;
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            float invT = 1 - t;
            
            // Cubic bezier with control points
            float bx = invT * invT * invT * x1 + 3 * invT * invT * t * cp1x + 3 * invT * t * t * cp2x + t * t * t * x2;
            float by = invT * invT * invT * y1 + 3 * invT * invT * t * y1 + 3 * invT * t * t * y2 + t * t * t * y2;
            
            // Draw line segment (2px thick)
            int px1 = (int) prevX, py1 = (int) prevY;
            int px2 = (int) bx, py2 = (int) by;
            
            // Horizontal-dominant segments
            if (Math.abs(px2 - px1) >= Math.abs(py2 - py1)) {
                int minX = Math.min(px1, px2);
                int maxX = Math.max(px1, px2);
                int midY = (py1 + py2) / 2;
                guiGraphics.fill(minX, midY - 1, maxX + 1, midY + 1, color);
            } else {
                int minY = Math.min(py1, py2);
                int maxY = Math.max(py1, py2);
                int midX = (px1 + px2) / 2;
                guiGraphics.fill(midX - 1, minY, midX + 1, maxY + 1, color);
            }
            
            prevX = bx;
            prevY = by;
        }
    }
    
    private void renderDragLine(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = 0, startY = 0;
        if (draggingFromNode == 0 && draggingFromOutput) {
            startX = node1OutputX[0]; startY = node1OutputY[0];
        } else if (draggingFromNode == 1 && draggingFromOutput) {
            startX = node2OutputX[0]; startY = node2OutputY[0];
        }
        renderBezierLine(guiGraphics, startX, startY, mouseX, mouseY, 0x88FFD700);
    }
    
    private void renderNode(GuiGraphics guiGraphics, int nx, int ny, String title, 
                           boolean hasOutput, boolean hasInput, int mouseX, int mouseY, int nodeIndex) {
        // Node background
        guiGraphics.fill(nx, ny, nx + NODE_WIDTH, ny + NODE_MIN_HEIGHT, NODE_BG);
        
        // Border
        guiGraphics.fill(nx, ny, nx + NODE_WIDTH, ny + 1, NODE_BORDER);
        guiGraphics.fill(nx, ny + NODE_MIN_HEIGHT - 1, nx + NODE_WIDTH, ny + NODE_MIN_HEIGHT, NODE_BORDER);
        guiGraphics.fill(nx, ny, nx + 1, ny + NODE_MIN_HEIGHT, NODE_BORDER);
        guiGraphics.fill(nx + NODE_WIDTH - 1, ny, nx + NODE_WIDTH, ny + NODE_MIN_HEIGHT, NODE_BORDER);
        
        // Header bar
        guiGraphics.fill(nx + 1, ny + 1, nx + NODE_WIDTH - 1, ny + NODE_HEADER_HEIGHT, NODE_HEADER_BG);
        
        // Title
        guiGraphics.drawString(font, Component.literal(title), 
            nx + 8, ny + 7, GOLD, false);
        
        // Connector pins
        if (hasOutput) {
            int connX = nx + NODE_WIDTH;
            int connY = ny + NODE_MIN_HEIGHT / 2;
            int connColor = getConnectorColor(nodeIndex == 0 ? 0 : 2);
            renderConnectorPin(guiGraphics, connX, connY, connColor);
        }
        if (hasInput) {
            int connX = nx;
            int connY = ny + NODE_MIN_HEIGHT / 2;
            int connColor = getConnectorColor(nodeIndex == 1 ? 1 : 3);
            renderConnectorPin(guiGraphics, connX, connY, connColor);
        }
    }
    
    private int getConnectorColor(int connectorIndex) {
        if (hoveredConnector == connectorIndex) {
            // Pulse animation on hover
            float pulse = (float) (0.5 + 0.5 * Math.sin(tickCount * 0.15));
            int alpha = (int) (180 + 75 * pulse);
            return (alpha << 24) | (0xFFD700 & 0x00FFFFFF);
        }
        // Connected state
        boolean connected = (connectorIndex <= 1 && node1Connected) || (connectorIndex >= 2 && node2Connected);
        return connected ? CONNECTOR_CONNECTED : CONNECTOR_DEFAULT;
    }
    
    private void renderConnectorPin(GuiGraphics guiGraphics, int cx, int cy, int color) {
        // Simple circle approximation using filled rectangles
        guiGraphics.fill(cx - 4, cy - 2, cx + 4, cy + 2, color);
        guiGraphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, color);
        guiGraphics.fill(cx - 2, cy - 4, cx + 2, cy + 4, color);
        // Inner dot
        guiGraphics.fill(cx - 2, cy - 1, cx + 2, cy + 1, 0xFF000000);
    }
    
    private void renderNode1Content(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Label above dropdown
        guiGraphics.drawString(font, Component.literal("Type:"),
            node1X + 8, node1Y + NODE_HEADER_HEIGHT + 18, LABEL_COLOR, false);
        
        // Render the dropdown widget
        taskTypeDropdown.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Show selected type icon below
        TaskType selected = getSelectedTaskType();
        guiGraphics.drawString(font, 
            Component.literal(selected.getIcon() + " " + selected.getDisplayName()),
            node1X + 8, node1Y + NODE_MIN_HEIGHT - 20, 0xFF88CC88, false);
    }
    
    private void renderNode2Content(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Description label
        guiGraphics.drawString(font, Component.literal("Description:"),
            node2X + 8, node2Y + NODE_HEADER_HEIGHT + 18, LABEL_COLOR, false);
        descriptionBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Goal label
        guiGraphics.drawString(font, Component.literal("Target Amount:"),
            node2X + 8, node2Y + NODE_HEADER_HEIGHT + 56, LABEL_COLOR, false);
        goalBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void renderNode3Content(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Reward label
        guiGraphics.drawString(font, Component.literal("Cash Reward ($):"),
            node3X + 8, node3Y + NODE_HEADER_HEIGHT + 18, LABEL_COLOR, false);
        rewardBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Preview the reward
        try {
            int reward = Integer.parseInt(rewardBox.getValue());
            if (reward > 0) {
                guiGraphics.drawString(font,
                    Component.literal("$" + String.format("%,d", reward)),
                    node3X + 8, node3Y + NODE_MIN_HEIGHT - 20, 0xFF88CC88, false);
            }
        } catch (NumberFormatException ignored) {}
    }
    
    // --- Input handling ---
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        
        // Check dropdown first (highest z-order)
        if (taskTypeDropdown.isExpanded()) {
            boolean result = taskTypeDropdown.mouseClicked(mouseX, mouseY, button);
            if (result) return true;
            // Close dropdown if clicked outside
            taskTypeDropdown.closeDropdown();
        }
        
        // Check connectors for drag start
        if (button == 0) {
            if (isNearConnector(mouseX, mouseY, node1OutputX[0], node1OutputY[0])) {
                draggingFromNode = 0;
                draggingFromOutput = true;
                return true;
            }
            if (isNearConnector(mouseX, mouseY, node2OutputX[0], node2OutputY[0])) {
                draggingFromNode = 1;
                draggingFromOutput = true;
                return true;
            }
        }
        
        // Check EditBoxes
        boolean anyClicked = false;
        if (descriptionBox.mouseClicked(mouseX, mouseY, button)) {
            goalBox.setFocused(false);
            rewardBox.setFocused(false);
            anyClicked = true;
        } else if (goalBox.mouseClicked(mouseX, mouseY, button)) {
            descriptionBox.setFocused(false);
            rewardBox.setFocused(false);
            anyClicked = true;
        } else if (rewardBox.mouseClicked(mouseX, mouseY, button)) {
            descriptionBox.setFocused(false);
            goalBox.setFocused(false);
            anyClicked = true;
        } else if (taskTypeDropdown.mouseClicked(mouseX, mouseY, button)) {
            descriptionBox.setFocused(false);
            goalBox.setFocused(false);
            rewardBox.setFocused(false);
            anyClicked = true;
        }
        
        if (!anyClicked) {
            // Clear focus from all
            descriptionBox.setFocused(false);
            goalBox.setFocused(false);
            rewardBox.setFocused(false);
        }
        
        return anyClicked;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingFromNode >= 0) {
            // Check if released on a valid connector
            if (draggingFromNode == 0 && draggingFromOutput) {
                if (isNearConnector(mouseX, mouseY, node2InputX[0], node2InputY[0])) {
                    node1Connected = true;
                }
            } else if (draggingFromNode == 1 && draggingFromOutput) {
                if (isNearConnector(mouseX, mouseY, node3InputX[0], node3InputY[0])) {
                    node2Connected = true;
                }
            }
            draggingFromNode = -1;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingFromNode >= 0) {
            dragMouseX = mouseX;
            dragMouseY = mouseY;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Route to dropdown first
        if (taskTypeDropdown.isExpanded()) {
            return taskTypeDropdown.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Route to focused EditBox
        if (descriptionBox.isFocused()) return descriptionBox.keyPressed(keyCode, scanCode, modifiers);
        if (goalBox.isFocused()) return goalBox.keyPressed(keyCode, scanCode, modifiers);
        if (rewardBox.isFocused()) return rewardBox.keyPressed(keyCode, scanCode, modifiers);
        
        // Tab key to cycle focus
        if (keyCode == 258) { // TAB
            if (!descriptionBox.isFocused() && !goalBox.isFocused() && !rewardBox.isFocused()) {
                descriptionBox.setFocused(true);
            } else if (descriptionBox.isFocused()) {
                descriptionBox.setFocused(false);
                goalBox.setFocused(true);
            } else if (goalBox.isFocused()) {
                goalBox.setFocused(false);
                rewardBox.setFocused(true);
            } else {
                rewardBox.setFocused(false);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (descriptionBox.isFocused()) return descriptionBox.charTyped(codePoint, modifiers);
        if (goalBox.isFocused()) return goalBox.charTyped(codePoint, modifiers);
        if (rewardBox.isFocused()) return rewardBox.charTyped(codePoint, modifiers);
        return false;
    }
    
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            descriptionBox.setFocused(false);
            goalBox.setFocused(false);
            rewardBox.setFocused(false);
            taskTypeDropdown.closeDropdown();
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }
}
