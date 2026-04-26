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
        
        // Title (truncated to fit within card)
        // Use a generous safety margin (10px = 5 each side) because the panel
        // can be drawn inside a pose-scaled matrix (ScalableContainerScreen).
        // GuiGraphics.enableScissor does NOT honour the pose transform, so any
        // overflow would visually escape the card and be overpainted by the
        // next card's background. Truncating in design-space here guarantees
        // the rendered text always sits well inside the card, regardless of
        // the active GUI scale.
        var titleStr = this.getMessage().getString();
        int maxTitleW = Math.max(8, this.width - 10);
        if (font.width(titleStr) > maxTitleW) {
            String elide = "..";
            titleStr = font.plainSubstrByWidth(titleStr, maxTitleW - font.width(elide)) + elide;
        }
        guiGraphics.drawCenteredString(font, titleStr,
            this.getX() + this.width / 2,
            this.getY() + 30,
            0xFFFFFF);
        
        // Description (truncated to fit within card)
        String desc = this.description;
        int maxDescW = Math.max(8, this.width - 10);
        if (font.width(desc) > maxDescW) {
            String elide = "..";
            desc = font.plainSubstrByWidth(desc, maxDescW - font.width(elide)) + elide;
        }
        guiGraphics.drawCenteredString(font, desc,
            this.getX() + this.width / 2,
            this.getY() + 42,
            0xCCCCCC);
        
        guiGraphics.disableScissor();
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        com.servermanagement.gui.debug.DebugLogger.logWidgetClick("DashboardCard", this.getMessage().getString(), this.getX(), this.getY());
        this.onPress.onPress();
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
