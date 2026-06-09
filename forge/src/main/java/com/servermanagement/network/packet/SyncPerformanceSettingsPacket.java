package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Server → Client: syncs all performance settings so the GUI can display current values.
 */
public record SyncPerformanceSettingsPacket(
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
        long totalEntitiesThrottled, long totalRedstoneThrottled) implements IPacket {

    public SyncPerformanceSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(),
             buf.readBoolean(), buf.readBoolean(),
             buf.readBoolean(), buf.readBoolean(),
             buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
             buf.readDouble(), buf.readInt(), buf.readInt(),
             buf.readInt(), buf.readInt(), buf.readInt(),
             buf.readInt(), buf.readInt(),
             buf.readDouble(), buf.readDouble(),
             buf.readDouble(), buf.readDouble(), buf.readBoolean(),
             buf.readLong(), buf.readLong(),
             buf.readLong(), buf.readLong());
    }

    @Override
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

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
        });
        ctx.get().setPacketHandled(true);
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
