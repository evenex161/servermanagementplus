package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
/**
 * Server → Client: syncs all performance settings so the GUI can display current values.
 */
public class SyncPerformanceSettingsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncPerformanceSettingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_performance_settings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncPerformanceSettingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncPerformanceSettingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }


    // Subsystem toggles
    private final boolean featureEnabled;
    private final boolean itemMergingEnabled;
    private final boolean mobSpawnLimiterEnabled;
    private final boolean entityActivationRangeEnabled;
    private final boolean villagerThrottleEnabled;
    private final boolean redstoneThrottleEnabled;
    private final boolean tpsMonitorEnabled;
    private final boolean tpsAutoOptimize;

    // Tunables
    private final double itemMergeRadius;
    private final int itemMergeInterval;
    private final int mobCapMultiplier;
    private final int monsterActivationRange;
    private final int animalActivationRange;
    private final int miscActivationRange;
    private final int villagerTickInterval;
    private final int redstoneUpdatesPerTick;
    private final double tpsWarningThreshold;
    private final double tpsCriticalThreshold;

    // Live stats
    private final double currentTps;
    private final double averageMspt;
    private final boolean autoOptimizeActive;
    private final long totalItemsMerged;
    private final long totalSpawnsCancelled;
    private final long totalEntitiesThrottled;
    private final long totalRedstoneThrottled;

    public SyncPerformanceSettingsPacket(
            boolean featureEnabled,
            boolean itemMergingEnabled, boolean mobSpawnLimiterEnabled,
            boolean entityActivationRangeEnabled, boolean villagerThrottleEnabled,
            boolean redstoneThrottleEnabled, boolean tpsMonitorEnabled, boolean tpsAutoOptimize,
            double itemMergeRadius, int itemMergeInterval, int mobCapMultiplier,
            int monsterActivationRange, int animalActivationRange, int miscActivationRange,
            int villagerTickInterval, int redstoneUpdatesPerTick,
            double tpsWarningThreshold, double tpsCriticalThreshold,
            double currentTps, double averageMspt, boolean autoOptimizeActive,
            long totalItemsMerged, long totalSpawnsCancelled,
            long totalEntitiesThrottled, long totalRedstoneThrottled) {
        this.featureEnabled = featureEnabled;
        this.itemMergingEnabled = itemMergingEnabled;
        this.mobSpawnLimiterEnabled = mobSpawnLimiterEnabled;
        this.entityActivationRangeEnabled = entityActivationRangeEnabled;
        this.villagerThrottleEnabled = villagerThrottleEnabled;
        this.redstoneThrottleEnabled = redstoneThrottleEnabled;
        this.tpsMonitorEnabled = tpsMonitorEnabled;
        this.tpsAutoOptimize = tpsAutoOptimize;
        this.itemMergeRadius = itemMergeRadius;
        this.itemMergeInterval = itemMergeInterval;
        this.mobCapMultiplier = mobCapMultiplier;
        this.monsterActivationRange = monsterActivationRange;
        this.animalActivationRange = animalActivationRange;
        this.miscActivationRange = miscActivationRange;
        this.villagerTickInterval = villagerTickInterval;
        this.redstoneUpdatesPerTick = redstoneUpdatesPerTick;
        this.tpsWarningThreshold = tpsWarningThreshold;
        this.tpsCriticalThreshold = tpsCriticalThreshold;
        this.currentTps = currentTps;
        this.averageMspt = averageMspt;
        this.autoOptimizeActive = autoOptimizeActive;
        this.totalItemsMerged = totalItemsMerged;
        this.totalSpawnsCancelled = totalSpawnsCancelled;
        this.totalEntitiesThrottled = totalEntitiesThrottled;
        this.totalRedstoneThrottled = totalRedstoneThrottled;
    }

    public SyncPerformanceSettingsPacket(FriendlyByteBuf buf) {
        this.featureEnabled = buf.readBoolean();
        this.itemMergingEnabled = buf.readBoolean();
        this.mobSpawnLimiterEnabled = buf.readBoolean();
        this.entityActivationRangeEnabled = buf.readBoolean();
        this.villagerThrottleEnabled = buf.readBoolean();
        this.redstoneThrottleEnabled = buf.readBoolean();
        this.tpsMonitorEnabled = buf.readBoolean();
        this.tpsAutoOptimize = buf.readBoolean();
        this.itemMergeRadius = buf.readDouble();
        this.itemMergeInterval = buf.readInt();
        this.mobCapMultiplier = buf.readInt();
        this.monsterActivationRange = buf.readInt();
        this.animalActivationRange = buf.readInt();
        this.miscActivationRange = buf.readInt();
        this.villagerTickInterval = buf.readInt();
        this.redstoneUpdatesPerTick = buf.readInt();
        this.tpsWarningThreshold = buf.readDouble();
        this.tpsCriticalThreshold = buf.readDouble();
        this.currentTps = buf.readDouble();
        this.averageMspt = buf.readDouble();
        this.autoOptimizeActive = buf.readBoolean();
        this.totalItemsMerged = buf.readLong();
        this.totalSpawnsCancelled = buf.readLong();
        this.totalEntitiesThrottled = buf.readLong();
        this.totalRedstoneThrottled = buf.readLong();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(featureEnabled);
        buf.writeBoolean(itemMergingEnabled);
        buf.writeBoolean(mobSpawnLimiterEnabled);
        buf.writeBoolean(entityActivationRangeEnabled);
        buf.writeBoolean(villagerThrottleEnabled);
        buf.writeBoolean(redstoneThrottleEnabled);
        buf.writeBoolean(tpsMonitorEnabled);
        buf.writeBoolean(tpsAutoOptimize);
        buf.writeDouble(itemMergeRadius);
        buf.writeInt(itemMergeInterval);
        buf.writeInt(mobCapMultiplier);
        buf.writeInt(monsterActivationRange);
        buf.writeInt(animalActivationRange);
        buf.writeInt(miscActivationRange);
        buf.writeInt(villagerTickInterval);
        buf.writeInt(redstoneUpdatesPerTick);
        buf.writeDouble(tpsWarningThreshold);
        buf.writeDouble(tpsCriticalThreshold);
        buf.writeDouble(currentTps);
        buf.writeDouble(averageMspt);
        buf.writeBoolean(autoOptimizeActive);
        buf.writeLong(totalItemsMerged);
        buf.writeLong(totalSpawnsCancelled);
        buf.writeLong(totalEntitiesThrottled);
        buf.writeLong(totalRedstoneThrottled);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientPacketHandler.handlePerformanceSettings(
                featureEnabled,
                itemMergingEnabled, mobSpawnLimiterEnabled,
                entityActivationRangeEnabled, villagerThrottleEnabled,
                redstoneThrottleEnabled, tpsMonitorEnabled, tpsAutoOptimize,
                itemMergeRadius, itemMergeInterval, mobCapMultiplier,
                monsterActivationRange, animalActivationRange, miscActivationRange,
                villagerTickInterval, redstoneUpdatesPerTick,
                tpsWarningThreshold, tpsCriticalThreshold,
                currentTps, averageMspt, autoOptimizeActive,
                totalItemsMerged, totalSpawnsCancelled,
                totalEntitiesThrottled, totalRedstoneThrottled
            );

}

    /**
     * Helper to build and send this packet from server config + manager state.
     */
    public static void syncToPlayer(net.minecraft.server.level.ServerPlayer player) {
        var config = com.servermanagement.config.ModConfig.class;
        var manager = com.servermanagement.features.serverperformance.ServerPerformanceManager.getInstance();

        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncPerformanceSettingsPacket(
                com.servermanagement.config.ModConfig.SERVER_PERFORMANCE_ENABLED.get(),
                com.servermanagement.config.ModConfig.ITEM_MERGING_ENABLED.get(),
                com.servermanagement.config.ModConfig.MOB_SPAWN_LIMITER_ENABLED.get(),
                com.servermanagement.config.ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.get(),
                com.servermanagement.config.ModConfig.VILLAGER_THROTTLE_ENABLED.get(),
                com.servermanagement.config.ModConfig.REDSTONE_THROTTLE_ENABLED.get(),
                com.servermanagement.config.ModConfig.TPS_MONITOR_ENABLED.get(),
                com.servermanagement.config.ModConfig.TPS_AUTO_OPTIMIZE.get(),
                com.servermanagement.config.ModConfig.ITEM_MERGE_RADIUS.get(),
                com.servermanagement.config.ModConfig.ITEM_MERGE_INTERVAL.get(),
                com.servermanagement.config.ModConfig.MOB_CAP_MULTIPLIER.get(),
                com.servermanagement.config.ModConfig.MONSTER_ACTIVATION_RANGE.get(),
                com.servermanagement.config.ModConfig.ANIMAL_ACTIVATION_RANGE.get(),
                com.servermanagement.config.ModConfig.MISC_ACTIVATION_RANGE.get(),
                com.servermanagement.config.ModConfig.VILLAGER_TICK_INTERVAL.get(),
                com.servermanagement.config.ModConfig.REDSTONE_UPDATES_PER_TICK.get(),
                com.servermanagement.config.ModConfig.TPS_WARNING_THRESHOLD.get(),
                com.servermanagement.config.ModConfig.TPS_CRITICAL_THRESHOLD.get(),
                manager.getCurrentTps(),
                manager.getAverageMspt(),
                manager.isAutoOptimizeActive(),
                manager.getTotalItemsMerged(),
                manager.getTotalSpawnsCancelled(),
                manager.getTotalEntitiesThrottled(),
                manager.getTotalRedstoneThrottled()
            ),
            player
        );
    }
}
