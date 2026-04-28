package com.servermanagement.gui.provider;

import com.servermanagement.gui.DashboardMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class DashboardMenuProvider implements MenuProvider {
    
    @Override
    public Component getDisplayName() {
        return Component.literal("ServerManagement Dashboard");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DashboardMenu(containerId, playerInventory);
    }
}
