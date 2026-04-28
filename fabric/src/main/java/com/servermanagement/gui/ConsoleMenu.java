package com.servermanagement.gui;

import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Console GUI
 */
public class ConsoleMenu extends AbstractContainerMenu {
    
    public ConsoleMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.CONSOLE_MENU, containerId);
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
