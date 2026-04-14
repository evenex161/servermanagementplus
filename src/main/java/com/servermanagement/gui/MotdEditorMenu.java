package com.servermanagement.gui;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MotdEditorMenu extends AbstractContainerMenu {

    private String motdText;

    public MotdEditorMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.MOTD_EDITOR_MENU.get(), windowId);
        this.motdText = ClientPacketHandler.getCachedMotdText();
    }

    public String getMotdText() {
        return this.motdText;
    }

    public void setMotdText(String text) {
        this.motdText = text;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}