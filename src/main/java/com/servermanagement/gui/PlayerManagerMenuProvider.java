package com.servermanagement.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class PlayerManagerMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.literal("Player Manager");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new PlayerManagerMenu(windowId, playerInventory);
    }
}
