package com.servermanagement.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class MotdEditorMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.literal("MOTD Editor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new MotdEditorMenu(windowId, playerInventory);
    }
}