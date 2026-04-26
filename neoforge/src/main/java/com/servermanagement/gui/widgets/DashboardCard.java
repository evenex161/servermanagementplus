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
        // Use a *very* generous safety margin (16px each side, 32 total)
        // because the panel can be drawn inside a pose-scaled matrix
        // (ScalableContainerScreen). GuiGraphics.enableScissor does NOT honour
        // the pose transform, so the scissor rectangle effectively moves
        // around at non-1:1 GUI scales (especially scale 4/5/Auto on small
        // windows) and cannot be relied upon to clip overflowing text.
        // Truncating aggressively in design-space here guarantees the
        // rendered text always sits well inside the visible card border.
        var titleStr = this.getMessage().getString();
        int textPadding = 16; // px each side
        int maxTitleW = Math.max(8, this.width - textPadding * 2);
        String elide = "\u2026"; // single-char ellipsis to save horizontal space
        int elideW = font.width(elide);
        if (font.width(titleStr) > maxTitleW) {
            titleStr = font.plainSubstrByWidth(titleStr, Math.max(0, maxTitleW - elideW)) + elide;
            // Defensive second pass: plainSubstrByWidth + elide can still
            // round just over budget for some glyph combinations \u2014 trim
            // one char at a time until it strictly fits.
            while (titleStr.length() > 1 && font.width(titleStr) > maxTitleW) {
                titleStr = titleStr.substring(0, titleStr.length() - 2) + elide;
            }
        }
        guiGraphics.drawCenteredString(font, titleStr,
            this.getX() + this.width / 2,
            this.getY() + 30,
            0xFFFFFF);
        
        // Description (truncated to fit within card)
        String desc = this.description;
        int maxDescW = Math.max(8, this.width - textPadding * 2);
        if (font.width(desc) > maxDescW) {
            desc = font.plainSubstrByWidth(desc, Math.max(0, maxDescW - elideW)) + elide;
            while (desc.length() > 1 && font.width(desc) > maxDescW) {
                desc = desc.substring(0, desc.length() - 2) + elide;
            }
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
