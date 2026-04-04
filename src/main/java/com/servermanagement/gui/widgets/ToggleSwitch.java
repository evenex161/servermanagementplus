package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Modern toggle switch widget (like iOS toggle)
 */
public class ToggleSwitch extends AbstractWidget {
    
    private boolean toggled;
    private final OnToggle onToggle;
    
    private static final int SWITCH_WIDTH = 40;
    private static final int SWITCH_HEIGHT = 20;
    
    public interface OnToggle {
        void onToggle(boolean newState);
    }
    
    public ToggleSwitch(int x, int y, Component message, boolean initialState, OnToggle onToggle) {
        super(x, y, SWITCH_WIDTH, SWITCH_HEIGHT, message);
        this.toggled = initialState;
        this.onToggle = onToggle;
    }
    
    public boolean isToggled() {
        return toggled;
    }
    
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        boolean hovered = this.isHoveredOrFocused();
        
        // Background track
        int trackColor = toggled ? 0xFF27AE60 : 0xFF505050; // Green when on, gray when off
        if (hovered) {
            trackColor = toggled ? 0xFF1E8449 : 0xFF303030; // Darker on hover
        }
        
        // Draw track (rounded rectangle effect)
        guiGraphics.fill(this.getX() + 2, this.getY(), this.getX() + SWITCH_WIDTH - 2, this.getY() + SWITCH_HEIGHT, trackColor);
        guiGraphics.fill(this.getX(), this.getY() + 2, this.getX() + 2, this.getY() + SWITCH_HEIGHT - 2, trackColor);
        guiGraphics.fill(this.getX() + SWITCH_WIDTH - 2, this.getY() + 2, this.getX() + SWITCH_WIDTH, this.getY() + SWITCH_HEIGHT - 2, trackColor);
        
        // Draw thumb (sliding circle)
        int thumbX = toggled ? (this.getX() + SWITCH_WIDTH - 18) : (this.getX() + 2);
        int thumbColor = 0xFFFFFFFF;
        
        // Draw thumb (square with 2px padding)
        guiGraphics.fill(thumbX, this.getY() + 2, thumbX + 16, this.getY() + SWITCH_HEIGHT - 2, thumbColor);
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.toggled = !this.toggled;
        this.onToggle.onToggle(this.toggled);
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
