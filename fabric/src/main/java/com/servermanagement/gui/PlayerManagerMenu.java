package com.servermanagement.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PlayerManagerMenu extends AbstractContainerMenu {
    
    public PlayerManagerMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.PLAYER_MANAGER_MENU, windowId);
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
