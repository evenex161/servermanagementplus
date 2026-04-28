package com.servermanagement.gui.provider;

import com.servermanagement.gui.menu.PerformanceSettingsMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class PerformanceSettingsMenuProvider implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.literal("Performance Settings");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PerformanceSettingsMenu(containerId, playerInventory);
    }
}
