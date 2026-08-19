package com.servermanagement.gui.menu;


import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PerformanceSettingsMenu extends AbstractContainerMenu {

    // Subsystem toggles
    private boolean featureEnabled;
    private boolean itemMergingEnabled;
    private boolean mobSpawnLimiterEnabled;
    private boolean entityActivationRangeEnabled;
    private boolean villagerThrottleEnabled;
    private boolean redstoneThrottleEnabled;
    private boolean tpsMonitorEnabled;
    private boolean tpsAutoOptimize;

    // Tunables
    private double itemMergeRadius;
    private int itemMergeInterval;
    private int mobCapMultiplier;
    private int monsterActivationRange;
    private int animalActivationRange;
    private int miscActivationRange;
    private int villagerTickInterval;
    private int redstoneUpdatesPerTick;
    private double tpsWarningThreshold;
    private double tpsCriticalThreshold;

    // Live stats (read-only display)
    private double currentTps;
    private double averageMspt;
    private boolean autoOptimizeActive;
    private long totalItemsMerged;
    private long totalSpawnsCancelled;
    private long totalEntitiesThrottled;
    private long totalRedstoneThrottled;

    public PerformanceSettingsMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.PERFORMANCE_SETTINGS_MENU.get(), windowId);
    }

    // --- Getters & Setters ---

    public boolean isFeatureEnabled() { return featureEnabled; }
    public void setFeatureEnabled(boolean v) { this.featureEnabled = v; }

    public boolean isItemMergingEnabled() { return itemMergingEnabled; }
    public void setItemMergingEnabled(boolean v) { this.itemMergingEnabled = v; }

    public boolean isMobSpawnLimiterEnabled() { return mobSpawnLimiterEnabled; }
    public void setMobSpawnLimiterEnabled(boolean v) { this.mobSpawnLimiterEnabled = v; }

    public boolean isEntityActivationRangeEnabled() { return entityActivationRangeEnabled; }
    public void setEntityActivationRangeEnabled(boolean v) { this.entityActivationRangeEnabled = v; }

    public boolean isVillagerThrottleEnabled() { return villagerThrottleEnabled; }
    public void setVillagerThrottleEnabled(boolean v) { this.villagerThrottleEnabled = v; }

    public boolean isRedstoneThrottleEnabled() { return redstoneThrottleEnabled; }
    public void setRedstoneThrottleEnabled(boolean v) { this.redstoneThrottleEnabled = v; }

    public boolean isTpsMonitorEnabled() { return tpsMonitorEnabled; }
    public void setTpsMonitorEnabled(boolean v) { this.tpsMonitorEnabled = v; }

    public boolean isTpsAutoOptimize() { return tpsAutoOptimize; }
    public void setTpsAutoOptimize(boolean v) { this.tpsAutoOptimize = v; }

    public double getItemMergeRadius() { return itemMergeRadius; }
    public void setItemMergeRadius(double v) { this.itemMergeRadius = v; }

    public int getItemMergeInterval() { return itemMergeInterval; }
    public void setItemMergeInterval(int v) { this.itemMergeInterval = v; }

    public int getMobCapMultiplier() { return mobCapMultiplier; }
    public void setMobCapMultiplier(int v) { this.mobCapMultiplier = v; }

    public int getMonsterActivationRange() { return monsterActivationRange; }
    public void setMonsterActivationRange(int v) { this.monsterActivationRange = v; }

    public int getAnimalActivationRange() { return animalActivationRange; }
    public void setAnimalActivationRange(int v) { this.animalActivationRange = v; }

    public int getMiscActivationRange() { return miscActivationRange; }
    public void setMiscActivationRange(int v) { this.miscActivationRange = v; }

    public int getVillagerTickInterval() { return villagerTickInterval; }
    public void setVillagerTickInterval(int v) { this.villagerTickInterval = v; }

    public int getRedstoneUpdatesPerTick() { return redstoneUpdatesPerTick; }
    public void setRedstoneUpdatesPerTick(int v) { this.redstoneUpdatesPerTick = v; }

    public double getTpsWarningThreshold() { return tpsWarningThreshold; }
    public void setTpsWarningThreshold(double v) { this.tpsWarningThreshold = v; }

    public double getTpsCriticalThreshold() { return tpsCriticalThreshold; }
    public void setTpsCriticalThreshold(double v) { this.tpsCriticalThreshold = v; }

    public double getCurrentTps() { return currentTps; }
    public void setCurrentTps(double currentTps) { this.currentTps = currentTps; }
    public double getAverageMspt() { return averageMspt; }
    public void setAverageMspt(double averageMspt) { this.averageMspt = averageMspt; }
    public boolean isAutoOptimizeActive() { return autoOptimizeActive; }
    public void setAutoOptimizeActive(boolean autoOptimizeActive) { this.autoOptimizeActive = autoOptimizeActive; }
    public long getTotalItemsMerged() { return totalItemsMerged; }
    public void setTotalItemsMerged(long totalItemsMerged) { this.totalItemsMerged = totalItemsMerged; }
    public long getTotalSpawnsCancelled() { return totalSpawnsCancelled; }
    public void setTotalSpawnsCancelled(long totalSpawnsCancelled) { this.totalSpawnsCancelled = totalSpawnsCancelled; }
    public long getTotalEntitiesThrottled() { return totalEntitiesThrottled; }
    public void setTotalEntitiesThrottled(long totalEntitiesThrottled) { this.totalEntitiesThrottled = totalEntitiesThrottled; }
    public long getTotalRedstoneThrottled() { return totalRedstoneThrottled; }
    public void setTotalRedstoneThrottled(long totalRedstoneThrottled) { this.totalRedstoneThrottled = totalRedstoneThrottled; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
