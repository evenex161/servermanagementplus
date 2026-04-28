package com.servermanagement.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class WorldDetailMenuProvider implements MenuProvider {
    private final String dimensionId;

    public WorldDetailMenuProvider(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("World Detail - " + dimensionId);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new WorldDetailMenu(windowId, playerInventory);
    }

    public String getDimensionId() {
        return dimensionId;
    }
}
