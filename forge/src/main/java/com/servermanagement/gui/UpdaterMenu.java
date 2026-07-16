package com.servermanagement.gui;

import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class UpdaterMenu extends AbstractContainerMenu {
    public UpdaterMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.UPDATER_MENU.get(), containerId);
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
