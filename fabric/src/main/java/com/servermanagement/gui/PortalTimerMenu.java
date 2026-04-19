package com.servermanagement.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PortalTimerMenu extends AbstractContainerMenu {
    
    public PortalTimerMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.PORTAL_TIMER_MENU, windowId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
