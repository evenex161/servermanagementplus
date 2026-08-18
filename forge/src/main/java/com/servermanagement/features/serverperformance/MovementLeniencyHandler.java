package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.integration.dh.DistantHorizonsHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
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
            // Try obfuscated/intermediary names
            try {
                for (Field f : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        // Heuristic: allowedPlayerTicks is typically the first int field after connection fields
                        // We'll try setting it and verify it doesn't break
                        f.setAccessible(true);
                        // Store candidate — we'll validate on first player join
                        if (allowedPlayerTicksField == null && f.getName().contains("allowed")) {
                            allowedPlayerTicksField = f;
                        }
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
     * Called periodically from TpsMonitorHandler to re-apply leniency
     * when TPS is low, since the field may be reset by vanilla code.
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
                // During critical TPS, double the multiplier for extra tolerance
                if (manager.isInitialized() &&
                    manager.getCurrentTps() < ModConfig.TPS_CRITICAL_THRESHOLD.get()) {
                    multiplier = Math.min(10, multiplier * 2);
                }
            } else {
                multiplier = 1; // Vanilla
            }

            int newValue = VANILLA_DEFAULT * multiplier;
            allowedPlayerTicksField.setInt(player.connection, newValue);
            appliedLeniencyTicks = newValue;
        } catch (Exception e) {
            // If reflection fails at runtime, disable silently
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
