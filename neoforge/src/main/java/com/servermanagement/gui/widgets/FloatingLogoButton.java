package com.servermanagement.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class FloatingLogoButton extends AbstractWidget {

    private static final ResourceLocation LOGO_TEXTURE = ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "textures/gui/servermanagement_logo.png");
    private static final int LOGO_WIDTH = 48;
    private static final int LOGO_HEIGHT = 48;
    
    // Persist position across screens
    public static int savedX = -1;
    public static int savedY = -1;

    private final Runnable onPress;
    private boolean hasNotification;
    
    private boolean isDragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private boolean actuallyDragged = false;
    
    // Tick-based animation
    private int tickCount = 0;
    
    // Physics variables for edge snapping
    private static final int MARGIN = 20;
    private double currentX;
    private double currentY;
    private double targetX;
    private double targetY;
    private boolean animating = false;

    public void setNotification(boolean hasNotification) {
        this.hasNotification = hasNotification;
    }

    public FloatingLogoButton(int screenWidth, int screenHeight, boolean hasNotification, Runnable onPress) {
        super(savedX != -1 ? savedX : screenWidth - LOGO_WIDTH - MARGIN, 
              savedY != -1 ? savedY : MARGIN, 
              LOGO_WIDTH, LOGO_HEIGHT, Component.empty());
        this.onPress = onPress;
        this.hasNotification = hasNotification;
        this.currentX = this.getX();
        this.currentY = this.getY();
        this.targetX = this.currentX;
        this.targetY = this.currentY;
        
        // On construction, snap to nearest edge cleanly
        snapToNearestEdge(screenWidth, screenHeight);
        this.currentX = this.targetX;
        this.currentY = this.targetY;
        this.setX((int) this.currentX);
        this.setY((int) this.currentY);
    }

    /**
     * NOR-gate snap logic: measure distance from the button center to each of
     * the 4 screen edges, pick the SINGLE closest edge, snap to it with MARGIN
     * padding, and only clamp the perpendicular axis to stay within bounds.
     * All other edges are ignored — only the closest one dictates position.
     */
    private void snapToNearestEdge(int sw, int sh) {
        double cx = currentX + LOGO_WIDTH * 0.5;
        double cy = currentY + LOGO_HEIGHT * 0.5;
        
        // Distance from button outer edge to each screen edge
        double distLeft   = currentX;                       // distance from left side of button to left edge
        double distRight  = sw - (currentX + LOGO_WIDTH);   // distance from right side of button to right edge
        double distTop    = currentY;                       // distance from top of button to top edge
        double distBottom = sh - (currentY + LOGO_HEIGHT);  // distance from bottom of button to bottom edge
        
        // Find the minimum — the single closest edge wins (NOR principle)
        double minDist = distLeft;
        int edge = 0; // 0=left, 1=right, 2=top, 3=bottom
        
        if (distRight < minDist)  { minDist = distRight;  edge = 1; }
        if (distTop < minDist)    { minDist = distTop;    edge = 2; }
        if (distBottom < minDist) { minDist = distBottom;  edge = 3; }
        
        // Snap to the winning edge; only clamp the perpendicular axis
        switch (edge) {
            case 0: // LEFT wins — snap X to left margin, clamp Y
                targetX = MARGIN;
                targetY = clampY(currentY, sh);
                break;
            case 1: // RIGHT wins — snap X to right margin, clamp Y
                targetX = sw - LOGO_WIDTH - MARGIN;
                targetY = clampY(currentY, sh);
                break;
            case 2: // TOP wins — snap Y to top margin, clamp X
                targetY = MARGIN;
                targetX = clampX(currentX, sw);
                break;
            case 3: // BOTTOM wins — snap Y to bottom margin, clamp X
                targetY = sh - LOGO_HEIGHT - MARGIN;
                targetX = clampX(currentX, sw);
                break;
        }
        
        animating = true;
    }
    
    /** Clamp X to stay within screen bounds with MARGIN padding. */
    private double clampX(double x, int sw) {
        return Math.max(MARGIN, Math.min(x, sw - LOGO_WIDTH - MARGIN));
    }
    
    /** Clamp Y to stay within screen bounds with MARGIN padding. */
    private double clampY(double y, int sh) {
        return Math.max(MARGIN, Math.min(y, sh - LOGO_HEIGHT - MARGIN));
    }

    public void tick() {
        tickCount++;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Smooth animation towards snap target
        if (!isDragging && animating) {
            double dx = targetX - currentX;
            double dy = targetY - currentY;
            
            if (dx * dx + dy * dy < 0.5) {
                currentX = targetX;
                currentY = targetY;
                animating = false;
            } else {
                currentX += dx * 0.2;
                currentY += dy * 0.2;
            }
            
            this.setX((int) currentX);
            this.setY((int) currentY);
            savedX = this.getX();
            savedY = this.getY();
        }

        // Bouncy floating animation
        float bounceY = (float) Math.sin((tickCount + partialTick) * 0.1f) * 4.0f;
        int renderY = this.getY() + (int) bounceY;
        
        // Render logo
        guiGraphics.blit(LOGO_TEXTURE, this.getX(), renderY, 0, 0, this.width, this.height, this.width, this.height);
        
        // Notification indicator
        if (hasNotification) {
            float pulse = (float) Math.sin((tickCount + partialTick) * 0.2f) * 0.5f + 0.5f;
            int alpha = (int) (100 + 155 * pulse);
            int color = (alpha << 24) | 0xFF3333; // Glowing red dot
            guiGraphics.fill(this.getX() + this.width - 12, renderY + 4, this.getX() + this.width - 4, renderY + 12, color);
        }
        
        // Hover effect (scale or highlight)
        if (this.isHovered) {
            guiGraphics.renderOutline(this.getX() - 2, renderY - 2, this.width + 4, this.height + 4, 0x88FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isHovered) {
            if (button == 0) { // Left click
                this.isDragging = true;
                this.actuallyDragged = false;
                this.animating = false;
                this.dragStartX = mouseX;
                this.dragStartY = mouseY;
                this.dragOffsetX = mouseX - this.getX();
                this.dragOffsetY = mouseY - this.getY();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 0) {
            if (!this.actuallyDragged) {
                if (Math.abs(mouseX - this.dragStartX) > 2 || Math.abs(mouseY - this.dragStartY) > 2) {
                    this.actuallyDragged = true;
                }
            }
            if (this.actuallyDragged) {
                currentX = mouseX - dragOffsetX;
                currentY = mouseY - dragOffsetY;
                this.setX((int) currentX);
                this.setY((int) currentY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDragging && button == 0) {
            this.isDragging = false;
            if (this.actuallyDragged) {
                // Snap to the single nearest edge (NOR gate logic)
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.screen != null) {
                    snapToNearestEdge(mc.screen.width, mc.screen.height);
                }
                savedX = (int) targetX;
                savedY = (int) targetY;
            } else if (this.isHovered) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPress.run();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
