package com.servermanagement.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class GlobalSettingsMenu extends AbstractContainerMenu {
    
    private boolean chatIsolationEnabled;
    private boolean tabIsolationEnabled;
    
    public GlobalSettingsMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.GLOBAL_SETTINGS_MENU, windowId);
    }


    public boolean isChatIsolationEnabled() {
        return this.chatIsolationEnabled;
    }
    
    public void setChatIsolationEnabled(boolean enabled) {
        this.chatIsolationEnabled = enabled;
    }
    
    public boolean isTabIsolationEnabled() {
        return this.tabIsolationEnabled;
    }
    
    public void setTabIsolationEnabled(boolean enabled) {
        this.tabIsolationEnabled = enabled;
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
