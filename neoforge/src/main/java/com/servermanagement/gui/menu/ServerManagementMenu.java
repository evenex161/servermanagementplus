package com.servermanagement.gui.menu;

import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ServerManagementMenu extends AbstractContainerMenu {
    
    private boolean worldManagerEnabled;
    private boolean playerManagerEnabled;
    private boolean slimeHeadEnabled;
    
    public ServerManagementMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SERVER_MANAGEMENT_MENU.get(), containerId);
        
        // Load current states from FeatureManager (which is synced to client)
        refreshStates();
    }
    
    /**
     * Refresh states from FeatureManager. Call this to update the menu with latest synced states.
     */
    public void refreshStates() {
        this.worldManagerEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("world_manager");
        this.playerManagerEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("player_manager");
        this.slimeHeadEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("slimehead");
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    public boolean isWorldManagerEnabled() {
        return this.worldManagerEnabled;
    }
    
    public void setWorldManagerEnabled(boolean enabled) {
        this.worldManagerEnabled = enabled;
    }
    
    public boolean isPlayerManagerEnabled() {
        return this.playerManagerEnabled;
    }
    
    public void setPlayerManagerEnabled(boolean enabled) {
        this.playerManagerEnabled = enabled;
    }
    
    public boolean isSlimeHeadEnabled() {
        return this.slimeHeadEnabled;
    }
    
    public void setSlimeHeadEnabled(boolean enabled) {
        this.slimeHeadEnabled = enabled;
    }
}
