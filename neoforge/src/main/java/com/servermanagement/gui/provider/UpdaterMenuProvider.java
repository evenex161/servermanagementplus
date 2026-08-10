package com.servermanagement.gui.provider;

import com.servermanagement.gui.UpdaterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class UpdaterMenuProvider implements MenuProvider {
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Server Updater");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new UpdaterMenu(containerId, playerInventory);
    }
}
