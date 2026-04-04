package com.servermanagement.gui.minebay;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * A slot that can have its position changed dynamically
 */
public class ToggleableSlot extends Slot {
    
    private int baseX;
    private int baseY;
    private boolean visible = true;
    
    public ToggleableSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.baseX = x;
        this.baseY = y;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isActive() {
        return visible;
    }
    
    public void setBasePosition(int x, int y) {
        this.baseX = x;
        this.baseY = y;
    }
    
    public int getBaseX() {
        return baseX;
    }
    
    public int getBaseY() {
        return baseY;
    }
}
