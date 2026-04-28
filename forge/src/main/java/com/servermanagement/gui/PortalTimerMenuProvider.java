package com.servermanagement.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class PortalTimerMenuProvider implements MenuProvider {
    private final String dimensionId;

    public PortalTimerMenuProvider(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Portal Timer - " + dimensionId);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new PortalTimerMenu(windowId, playerInventory);
    }

    public String getDimensionId() {
        return dimensionId;
    }
}
