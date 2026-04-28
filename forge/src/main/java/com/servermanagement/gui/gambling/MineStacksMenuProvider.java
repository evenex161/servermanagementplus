package com.servermanagement.gui.gambling;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineStacksMenuProvider implements MenuProvider {
    
    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("MineStacks Casino");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MineStacksMenu(containerId, playerInventory, player);
    }
    
    public static void open(ServerPlayer player) {
        player.openMenu(new MineStacksMenuProvider());
    }
}
