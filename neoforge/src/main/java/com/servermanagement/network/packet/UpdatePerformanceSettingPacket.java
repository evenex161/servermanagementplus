package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.network.PacketTimestampTracker;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: updates a single performance setting identified by key.
 * Supports both boolean toggles and numeric values (sent as String).
 */
public record UpdatePerformanceSettingPacket(String settingKey, String value, long clientTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdatePerformanceSettingPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "update_performance_setting"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, UpdatePerformanceSettingPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), UpdatePerformanceSettingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public UpdatePerformanceSettingPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(128), buf.readUtf(128), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(settingKey, 128);
        buf.writeUtf(value, 128);
        buf.writeLong(clientTick);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null || !player.hasPermissions(2)) return;

            String actionKey = "perf_" + settingKey;
            if (!PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) return;

            try {
                applySettingChange(settingKey, value);
                ServerManagementMod.LOGGER.info("Player {} changed performance setting {} to {}",
                    player.getName().getString(), settingKey, value);
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to apply performance setting {}: {}", settingKey, e.getMessage());
            }
        });
        // packet handled
    }

    private static void applySettingChange(String key, String val) {
        switch (key) {
            // Boolean toggles
            case "feature_enabled" -> ModConfig.SERVER_PERFORMANCE_ENABLED.set(Boolean.parseBoolean(val));
            case "item_merging" -> ModConfig.ITEM_MERGING_ENABLED.set(Boolean.parseBoolean(val));
            case "mob_spawn_limiter" -> ModConfig.MOB_SPAWN_LIMITER_ENABLED.set(Boolean.parseBoolean(val));
            case "entity_activation_range" -> ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.set(Boolean.parseBoolean(val));
            case "villager_throttle" -> ModConfig.VILLAGER_THROTTLE_ENABLED.set(Boolean.parseBoolean(val));
            case "redstone_throttle" -> ModConfig.REDSTONE_THROTTLE_ENABLED.set(Boolean.parseBoolean(val));
            case "tps_monitor" -> ModConfig.TPS_MONITOR_ENABLED.set(Boolean.parseBoolean(val));
            case "auto_optimize" -> ModConfig.TPS_AUTO_OPTIMIZE.set(Boolean.parseBoolean(val));

            // Numeric tunables
            case "item_merge_radius" -> ModConfig.ITEM_MERGE_RADIUS.set(clampDouble(val, 1.0, 10.0));
            case "item_merge_interval" -> ModConfig.ITEM_MERGE_INTERVAL.set(clampInt(val, 10, 200));
            case "mob_cap_multiplier" -> ModConfig.MOB_CAP_MULTIPLIER.set(clampInt(val, 10, 100));
            case "monster_activation_range" -> ModConfig.MONSTER_ACTIVATION_RANGE.set(clampInt(val, 8, 128));
            case "animal_activation_range" -> ModConfig.ANIMAL_ACTIVATION_RANGE.set(clampInt(val, 8, 128));
            case "misc_activation_range" -> ModConfig.MISC_ACTIVATION_RANGE.set(clampInt(val, 4, 64));
            case "villager_tick_interval" -> ModConfig.VILLAGER_TICK_INTERVAL.set(clampInt(val, 1, 10));
            case "redstone_updates_per_tick" -> ModConfig.REDSTONE_UPDATES_PER_TICK.set(clampInt(val, 100, 100000));
            case "tps_warning_threshold" -> ModConfig.TPS_WARNING_THRESHOLD.set(clampDouble(val, 5.0, 20.0));
            case "tps_critical_threshold" -> ModConfig.TPS_CRITICAL_THRESHOLD.set(clampDouble(val, 5.0, 20.0));

            default -> ServerManagementMod.LOGGER.warn("Unknown performance setting key: {}", key);
        }
    }

    private static int clampInt(String val, int min, int max) {
        try {
            int v = Integer.parseInt(val);
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return min;
        }
    }

    private static double clampDouble(String val, double min, double max) {
        try {
            double v = Double.parseDouble(val);
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return min;
        }
    }
}
