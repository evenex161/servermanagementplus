package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.integration.dh.DistantHorizonsHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.lang.reflect.Field;

/**
 * Increases the server's movement validation leniency to suppress
 * "moved too quickly" log spam during TPS drops or when Distant Horizons
 * causes large position updates.
 *
 * Uses reflection to modify ServerGamePacketListenerImpl.allowedPlayerTicks,
 * which controls how many ticks of movement are accepted in a single packet.
 * Vanilla default is 20; we increase it proportionally when TPS is low.
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class MovementLeniencyHandler {

    private static Field allowedPlayerTicksField = null;
    private static boolean reflectionFailed = false;
    private static final int VANILLA_DEFAULT = 20;
    private static int appliedLeniencyTicks = 0;

    static {
        try {
            allowedPlayerTicksField = ServerGamePacketListenerImpl.class.getDeclaredField("allowedPlayerTicks");
            allowedPlayerTicksField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                for (Field f : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                    if (f.getType() == int.class && f.getName().contains("allowed")) {
                        f.setAccessible(true);
                        allowedPlayerTicksField = f;
                        break;
                    }
                }
            } catch (Exception ex) {
                // Silent
            }
            if (allowedPlayerTicksField == null) {
                reflectionFailed = true;
                ServerManagementMod.LOGGER.debug("MovementLeniencyHandler: Could not find allowedPlayerTicks field. " +
                    "Movement leniency will not be applied. This is harmless.");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (reflectionFailed) return;
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.MOVEMENT_LENIENCY_ENABLED.get()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            applyLeniency(player);
        }
    }

    /**
     * Called periodically from ServerPerformanceManager to re-apply leniency.
     */
    public static void applyLeniencyToAll() {
        if (reflectionFailed) return;
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.MOVEMENT_LENIENCY_ENABLED.get()) return;

        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return;

        for (ServerPlayer player : manager.getServer().getPlayerList().getPlayers()) {
            applyLeniency(player);
        }
    }

    private static void applyLeniency(ServerPlayer player) {
        if (allowedPlayerTicksField == null) return;

        try {
            ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
            boolean dhActive = DistantHorizonsHook.isAvailable();
            boolean tpsLow = manager.isInitialized() &&
                manager.getCurrentTps() < ModConfig.TPS_WARNING_THRESHOLD.get();

            int multiplier;
            if (dhActive || tpsLow || manager.isAutoOptimizeActive()) {
                multiplier = ModConfig.MOVEMENT_LENIENCY_MULTIPLIER.get();
                if (manager.isInitialized() &&
                    manager.getCurrentTps() < ModConfig.TPS_CRITICAL_THRESHOLD.get()) {
                    multiplier = Math.min(10, multiplier * 2);
                }
            } else {
                multiplier = 1;
            }

            int newValue = VANILLA_DEFAULT * multiplier;
            allowedPlayerTicksField.setInt(player.connection, newValue);
            appliedLeniencyTicks = newValue;
        } catch (Exception e) {
            if (!reflectionFailed) {
                reflectionFailed = true;
                ServerManagementMod.LOGGER.debug("MovementLeniencyHandler: Failed to set allowedPlayerTicks: {}", e.getMessage());
            }
        }
    }

    public static boolean isReflectionAvailable() {
        return !reflectionFailed;
    }

    public static int getAppliedLeniencyTicks() {
        return appliedLeniencyTicks;
    }
}
