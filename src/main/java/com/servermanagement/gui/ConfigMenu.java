package com.servermanagement.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ConfigMenu extends AbstractContainerMenu {
    
    private boolean worldManagerEnabled;
    private boolean playerManagerEnabled;
    private boolean economyEnabled;
    private boolean slimeHeadEnabled;
    
    public ConfigMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.CONFIG_MENU.get(), windowId);
        
        // Load current states from FeatureManager (synced to client)
        refreshStates();
    }
    
    public void refreshStates() {
        this.worldManagerEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("world_manager");
        this.playerManagerEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("player_manager");
        this.economyEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("economy");
        this.slimeHeadEnabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("slimehead");
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
    
    public boolean isEconomyEnabled() {
        return this.economyEnabled;
    }
    
    public void setEconomyEnabled(boolean enabled) {
        this.economyEnabled = enabled;
    }
    
    public boolean isSlimeHeadEnabled() {
        return this.slimeHeadEnabled;
    }
    
    public void setSlimeHeadEnabled(boolean enabled) {
        this.slimeHeadEnabled = enabled;
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
