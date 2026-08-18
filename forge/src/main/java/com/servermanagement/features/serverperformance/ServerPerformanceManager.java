package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.integration.dh.DistantHorizonsHook;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPerformanceManager {

    private static final ServerPerformanceManager INSTANCE = new ServerPerformanceManager();
    private MinecraftServer server;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // TPS tracking
    private final long[] tickTimes = new long[100];
    private int tickIndex = 0;
    private long lastTickTime = System.nanoTime();
    private double currentTps = 20.0;
    private double averageMspt = 0.0;

    // Auto-optimize state
    private boolean autoOptimizeActive = false;
    private int consecutiveLowTpsTicks = 0;
    private static final int LOW_TPS_THRESHOLD_TICKS = 100; // 5 seconds of sustained low TPS

    // Stats
    private long totalItemsMerged = 0;
    private long totalSpawnsCancelled = 0;
    private long totalEntitiesThrottled = 0;
    private long totalRedstoneThrottled = 0;
    private long totalChunksThrottled = 0;
    private int viewDistanceReductions = 0;

    // Dynamic view distance state
    private int originalViewDistance = -1;
    private int originalSimulationDistance = -1;
    private boolean viewDistanceReduced = false;

    // Movement leniency re-application counter
    private int leniencyReapplyCounter = 0;
    private static final int LENIENCY_REAPPLY_INTERVAL = 200; // every 10 seconds

    private ServerPerformanceManager() {}

    public static ServerPerformanceManager getInstance() {
        return INSTANCE;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        this.initialized.set(true);
        ServerManagementMod.LOGGER.debug("Server Performance system initialized");
    }

    public void shutdown() {
        initialized.set(false);
        server = null;
    }

    public void recordTick() {
        long now = System.nanoTime();
        long elapsed = now - lastTickTime;
        lastTickTime = now;

        tickTimes[tickIndex] = elapsed;
        tickIndex = (tickIndex + 1) % tickTimes.length;

        // Sample allocation rate every second (20 ticks)
        if (tickIndex % 20 == 0) {
            AllocationTracker.sample();
        }

        // Calculate TPS and MSPT from rolling window
        long totalNanos = 0;
        int count = 0;
        for (long t : tickTimes) {
            if (t > 0) {
                totalNanos += t;
                count++;
            }
        }

        if (count > 0) {
            double avgNanos = (double) totalNanos / count;
            averageMspt = avgNanos / 1_000_000.0;
            currentTps = Math.min(20.0, 1_000_000_000.0 / avgNanos);
        }

        // Auto-optimize logic
        if (ModConfig.TPS_AUTO_OPTIMIZE.get() && ModConfig.TPS_MONITOR_ENABLED.get()) {
            if (currentTps < ModConfig.TPS_CRITICAL_THRESHOLD.get()) {
                consecutiveLowTpsTicks++;
                if (consecutiveLowTpsTicks >= LOW_TPS_THRESHOLD_TICKS && !autoOptimizeActive) {
                    activateAutoOptimize();
                }
            } else if (currentTps >= ModConfig.TPS_WARNING_THRESHOLD.get()) {
                if (autoOptimizeActive) {
                    consecutiveLowTpsTicks = 0;
                    deactivateAutoOptimize();
                } else {
                    consecutiveLowTpsTicks = 0;
                }
            }
        }

        // Periodically re-apply movement leniency
        if (ModConfig.MOVEMENT_LENIENCY_ENABLED.get()) {
            leniencyReapplyCounter++;
            if (leniencyReapplyCounter >= LENIENCY_REAPPLY_INTERVAL) {
                leniencyReapplyCounter = 0;
                MovementLeniencyHandler.applyLeniencyToAll();
            }
        }
    }

    private void activateAutoOptimize() {
        autoOptimizeActive = true;
        ServerManagementMod.LOGGER.warn("TPS dropped below critical threshold ({}) for 5 seconds — auto-optimize activated",
                String.format("%.1f", currentTps));

        // Dynamic view distance reduction
        if (ModConfig.DYNAMIC_VIEW_DISTANCE_ENABLED.get() && server != null && !viewDistanceReduced) {
            int reduction = ModConfig.VIEW_DISTANCE_REDUCTION.get();
            originalViewDistance = server.getPlayerList().getViewDistance();
            originalSimulationDistance = server.getPlayerList().getSimulationDistance();

            int newViewDist = Math.max(2, originalViewDistance - reduction);
            int newSimDist = Math.max(2, originalSimulationDistance - reduction);

            server.getPlayerList().setViewDistance(newViewDist);
            server.getPlayerList().setSimulationDistance(newSimDist);
            viewDistanceReduced = true;
            viewDistanceReductions++;

            ServerManagementMod.LOGGER.warn("Dynamic View Distance: Reduced view {} -> {}, simulation {} -> {}",
                    originalViewDistance, newViewDist, originalSimulationDistance, newSimDist);
        }
    }

    private void deactivateAutoOptimize() {
        autoOptimizeActive = false;
        ServerManagementMod.LOGGER.info("TPS recovered above warning threshold ({}) — auto-optimize deactivated",
                String.format("%.1f", currentTps));

        // Restore original view distance
        if (viewDistanceReduced && server != null) {
            server.getPlayerList().setViewDistance(originalViewDistance);
            server.getPlayerList().setSimulationDistance(originalSimulationDistance);
            ServerManagementMod.LOGGER.info("Dynamic View Distance: Restored view {} and simulation {}",
                    originalViewDistance, originalSimulationDistance);
            viewDistanceReduced = false;
        }
    }

    // --- Getters ---

    public boolean isInitialized() {
        return initialized.get();
    }

    public MinecraftServer getServer() {
        return server;
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public double getAverageMspt() {
        return averageMspt;
    }

    public boolean isAutoOptimizeActive() {
        return autoOptimizeActive;
    }

    // Item merge radius is increased when auto-optimizing
    public double getEffectiveItemMergeRadius() {
        double base = ModConfig.ITEM_MERGE_RADIUS.get();
        return autoOptimizeActive ? base * 1.5 : base;
    }

    // Mob cap is lowered when auto-optimizing
    public int getEffectiveMobCapMultiplier() {
        int base = ModConfig.MOB_CAP_MULTIPLIER.get();
        return autoOptimizeActive ? Math.max(10, base / 2) : base;
    }

    // Villager interval is increased when auto-optimizing
    public int getEffectiveVillagerTickInterval() {
        int base = ModConfig.VILLAGER_TICK_INTERVAL.get();
        return autoOptimizeActive ? Math.min(10, base * 2) : base;
    }

    // --- Stats ---

    public void addItemsMerged(long count) {
        totalItemsMerged += count;
    }

    public void addSpawnsCancelled(long count) {
        totalSpawnsCancelled += count;
    }

    public void addEntitiesThrottled(long count) {
        totalEntitiesThrottled += count;
    }

    public void addRedstoneThrottled(long count) {
        totalRedstoneThrottled += count;
    }

    public long getTotalItemsMerged() {
        return totalItemsMerged;
    }

    public long getTotalSpawnsCancelled() {
        return totalSpawnsCancelled;
    }

    public long getTotalEntitiesThrottled() {
        return totalEntitiesThrottled;
    }

    public long getTotalRedstoneThrottled() {
        return totalRedstoneThrottled;
    }

    public void resetStats() {
        totalItemsMerged = 0;
        totalSpawnsCancelled = 0;
        totalEntitiesThrottled = 0;
        totalRedstoneThrottled = 0;
        totalChunksThrottled = 0;
        viewDistanceReductions = 0;
    }

    public TpsStatus getTpsStatus() {
        if (!ModConfig.TPS_MONITOR_ENABLED.get()) {
            return TpsStatus.HEALTHY;
        }
        if (currentTps < ModConfig.TPS_CRITICAL_THRESHOLD.get()) {
            return TpsStatus.CRITICAL;
        }
        if (currentTps < ModConfig.TPS_WARNING_THRESHOLD.get()) {
            return TpsStatus.WARNING;
        }
        return TpsStatus.HEALTHY;
    }

    // --- Chunk throttle stats ---

    public void addChunksThrottled(long count) {
        totalChunksThrottled += count;
    }

    public long getTotalChunksThrottled() {
        return totalChunksThrottled;
    }

    public int getViewDistanceReductions() {
        return viewDistanceReductions;
    }

    public boolean isViewDistanceReduced() {
        return viewDistanceReduced;
    }

    public int getEffectiveViewDistance() {
        return server != null ? server.getPlayerList().getViewDistance() : -1;
    }

    public int getEffectiveSimulationDistance() {
        return server != null ? server.getPlayerList().getSimulationDistance() : -1;
    }

    public enum TpsStatus {
        HEALTHY("§a"),
        WARNING("§e"),
        CRITICAL("§c");

        private final String colorCode;

        TpsStatus(String colorCode) {
            this.colorCode = colorCode;
        }

        public String getColorCode() {
            return colorCode;
        }
    }
}
