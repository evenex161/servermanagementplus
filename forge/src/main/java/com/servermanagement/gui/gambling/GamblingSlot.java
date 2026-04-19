package com.servermanagement.gui.gambling;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot for gambling GUIs that can be dynamically enabled/disabled
 */
public class GamblingSlot extends Slot {
    
    private boolean enabled = true;
    
    public GamblingSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isActive() {
        return enabled;
    }
}
