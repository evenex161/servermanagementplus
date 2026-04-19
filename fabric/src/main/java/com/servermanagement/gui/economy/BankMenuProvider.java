package com.servermanagement.gui.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Provider for the Bank Menu
 */
public class BankMenuProvider implements MenuProvider {
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Bank Account");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BankMenu(containerId, playerInventory);
    }
}
