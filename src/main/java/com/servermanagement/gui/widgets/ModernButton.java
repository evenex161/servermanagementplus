package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Modern, minimal button widget with clean styling
 */
public class ModernButton extends Button {
    
    private final ButtonStyle style;
    private boolean isToggled = false;
    
    public enum ButtonStyle {
        PRIMARY(0x4A90E2, 0x5DA8F2, 0xFFFFFF, 0xFF6AB8FF),      // Blue
        SUCCESS(0x27AE60, 0x33CC73, 0xFFFFFF, 0xFF50E88A),      // Green
        DANGER(0xE74C3C, 0xF06050, 0xFFFFFF, 0xFFFF7B6E),       // Red
        SECONDARY(0x505050, 0x6A6A6A, 0xE0E0E0, 0xFFA0A0A0),    // Gray
        DARK(0x2C2C2C, 0x404040, 0xE0E0E0, 0xFF808080);         // Dark Gray
        
        final int baseColor;
        final int hoverColor;
        final int textColor;
        final int hoverBorderColor;
        
        ButtonStyle(int baseColor, int hoverColor, int textColor, int hoverBorderColor) {
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            this.textColor = textColor;
            this.hoverBorderColor = hoverBorderColor;
        }
    }
    
    public ModernButton(int x, int y, int width, int height, Component message, OnPress onPress, ButtonStyle style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }
    
    public void setToggled(boolean toggled) {
        this.isToggled = toggled;
    }
    
    public boolean isToggled() {
        return this.isToggled;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        boolean hovered = this.isHoveredOrFocused();
        int color = hovered ? style.hoverColor : style.baseColor;
        
        // Disabled state
        if (!this.active) {
            color = 0x404040;
        } else if (this.isToggled) {
            // Toggled/active state: use a highlighted color
            color = 0x4A90E2;
        }
        
        // Render background with slight transparency
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 
                        0xFF000000 | color);
        
        // Render highlight overlay on hover for extra visibility
        if (hovered && this.active && !this.isToggled) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 
                            0x30FFFFFF);
        }
        
        // Render subtle border
        int borderColor = hovered ? style.hoverBorderColor : 0x80FFFFFF;
        if (!this.active) {
            borderColor = 0x40FFFFFF;
        } else if (this.isToggled) {
            borderColor = 0xFF6AB0FF;
        }
        
        // Top border
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        // Bottom border
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
        // Left border
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        // Right border
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);
        
        // Render text centered
        int textColor = this.active ? style.textColor : 0x808080;
        guiGraphics.drawCenteredString(
            net.minecraft.client.Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            textColor
        );
    }
    
    /**
     * Builder for easy button creation
     */
    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private ButtonStyle style = ButtonStyle.PRIMARY;
        private int x, y, width = 200, height = 20;
        
        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }
        
        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder style(ButtonStyle style) {
            this.style = style;
            return this;
        }
        
        public ModernButton build() {
            return new ModernButton(x, y, width, height, message, onPress, style);
        }
    }
}
