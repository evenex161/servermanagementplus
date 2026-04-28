package com.servermanagement.gui.minebay;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Menu provider for MineBay marketplace
 */
public class MineBayMenuProvider implements MenuProvider {
    
    @Override
    public Component getDisplayName() {
        return Component.literal("MineBay - Player Trading");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MineBayMenu(containerId, playerInventory);
    }
}
