package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Dashboard card widget for feature panels
 */
public class DashboardCard extends AbstractWidget {
    
    private final String iconText;
    private final String description;
    private final CardStyle style;
    private final OnPress onPress;
    
    public interface OnPress {
        void onPress();
    }
    
    public enum CardStyle {
        BLUE(0x4A90E2, 0x357ABD),
        GREEN(0x27AE60, 0x1E8449),
        RED(0xE74C3C, 0xC0392B),
        PURPLE(0x9B59B6, 0x7D3C98),
        ORANGE(0xE67E22, 0xCA6F1E),
        GRAY(0x505050, 0x303030);
        
        final int color;
        final int hoverColor;
        
        CardStyle(int color, int hoverColor) {
            this.color = color;
            this.hoverColor = hoverColor;
        }
    }
    
    public DashboardCard(int x, int y, int width, int height, Component message, 
                        String iconText, String description, CardStyle style, OnPress onPress) {
        super(x, y, width, height, message);
        this.iconText = iconText;
        this.description = description;
        this.style = style;
        this.onPress = onPress;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        boolean hovered = this.isHoveredOrFocused();
        int color = hovered ? style.hoverColor : style.color;
        
        // Background
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 
                        0xFF000000 | color);
        
        // Border
        int borderColor = hovered ? 0xFFFFFFFF : 0x80FFFFFF;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);
        
        // Clip text to card bounds
        guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1,
            this.getX() + this.width - 1, this.getY() + this.height - 1);
        
        // Icon (large text)
        var font = net.minecraft.client.Minecraft.getInstance().font;
        guiGraphics.drawString(font, this.iconText,
            this.getX() + (this.width - font.width(this.iconText)) / 2,
            this.getY() + 10,
            0xFFFFFF, false);
        
        // Title and description — render via the helper below so any text
        // wider than the card budget is *visually shrunk* (pose scale) rather
        // than truncated. This is bullet-proof against pose-scale interactions
        // with enableScissor (which still doesn't reliably clip when the
        // parent ScalableContainerScreen has applied its own pose scale at
        // small GUI workspaces / Auto / Scale 4 / Scale 5).
        int textPadding = 4; // tiny padding because we shrink-to-fit instead of truncate
        int maxLineW = Math.max(8, this.width - textPadding * 2);

        drawScaledCenteredString(guiGraphics, font, this.getMessage().getString(),
            this.getX() + this.width / 2, this.getY() + 30, maxLineW, 0xFFFFFF);

        drawScaledCenteredString(guiGraphics, font, this.description,
            this.getX() + this.width / 2, this.getY() + 42, maxLineW, 0xCCCCCC);

        guiGraphics.disableScissor();
    }

    /**
     * Draw {@code text} centered horizontally on {@code centerX}, baseline
     * at {@code y}. If the text is wider than {@code maxWidth}, the pose is
     * pushed and a uniform horizontal-only scale is applied so the rendered
     * glyphs fit exactly within {@code maxWidth}. Vertical glyph height is
     * preserved by counter-scaling Y so the line stays visually centered on
     * the same baseline. This guarantees the text never paints outside the
     * card border at any GUI scale (Phase 2.5 truncation was insufficient
     * because {@code GuiGraphics.enableScissor} does not honour pose scaling
     * applied by {@link com.servermanagement.gui.ScalableContainerScreen}).
     */
    private static void drawScaledCenteredString(GuiGraphics g, net.minecraft.client.gui.Font font,
                                                 String text, int centerX, int y,
                                                 int maxWidth, int color) {
        if (text == null || text.isEmpty()) return;
        int w = font.width(text);
        if (w <= maxWidth) {
            g.drawCenteredString(font, text, centerX, y, color);
            return;
        }
        float scale = (float) maxWidth / (float) w;
        g.pose().pushMatrix();
        // Translate to centerX/y, scale X only, translate back.
        g.pose().translate(centerX, y);
        g.pose().scale(scale, 1f);
        g.pose().translate(-centerX, -y);
        g.drawCenteredString(font, text, centerX, y, color);
        g.pose().popMatrix();
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                com.servermanagement.gui.debug.DebugLogger.logWidgetClick("DashboardCard", this.getMessage().getString(), this.getX(), this.getY());
                this.onPress.onPress();
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
