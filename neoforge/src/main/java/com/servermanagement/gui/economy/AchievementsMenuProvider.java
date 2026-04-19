package com.servermanagement.gui.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Provider for the Achievements Menu
 */
public class AchievementsMenuProvider implements MenuProvider {
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Achievements");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AchievementsMenu(containerId, playerInventory);
    }
}
