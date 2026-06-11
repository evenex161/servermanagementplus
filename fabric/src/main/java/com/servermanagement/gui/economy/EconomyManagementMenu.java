package com.servermanagement.gui.economy;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for Economy Management GUI (Admin only)
 */
public class EconomyManagementMenu extends AbstractContainerMenu {

    public EconomyManagementMenu(int containerId, Inventory playerInventory) {
        super(com.servermanagement.gui.ModMenuTypes.ECONOMY_MANAGEMENT_MENU, containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return serverPlayer.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        }
        return player.canUseGameMasterBlocks();
    }
}
