package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.integration.dh.DistantHorizonsHook;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Dynamically throttles chunk pressure when TPS drops, especially with Distant Horizons.
 *
 * When DH is active and TPS falls below the warning threshold, this handler progressively
 * reduces the server's view-distance to limit the chunk generation/sending workload.
 * Unlike the auto-optimize dynamic view distance (which is a binary on/off emergency),
 * this handler applies smooth, graduated reductions based on current TPS level.
 *
 * The reduction is reversed as TPS recovers. This operates independently of the
 * auto-optimize system and activates at the WARNING threshold rather than CRITICAL.
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class ChunkGenThrottleHandler {

    private static volatile boolean throttleActive = false;
    private static int originalViewDistance = -1;
    private static int currentReduction = 0;
    private static int stabilityCounter = 0;
    private static final int STABILITY_TICKS = 100; // 5 seconds of stability before acting

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.CHUNK_GEN_THROTTLE_ENABLED.get()) return;

        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return;

        MinecraftServer server = manager.getServer();
        if (server == null) return;

        double tps = manager.getCurrentTps();
        boolean dhActive = DistantHorizonsHook.isAvailable();
        double warningThreshold = ModConfig.TPS_WARNING_THRESHOLD.get();
        int maxReduction = ModConfig.MAX_CHUNKS_PER_TICK.get();

        if (!dhActive && !ModConfig.TPS_AUTO_OPTIMIZE.get()) return;

        if (tps < warningThreshold) {
            if (!throttleActive) {
                stabilityCounter++;
                if (stabilityCounter >= STABILITY_TICKS) {
                    // First activation — capture original view distance
                    originalViewDistance = server.getPlayerList().getViewDistance();
                    throttleActive = true;
                    stabilityCounter = 0;
                    ServerManagementMod.LOGGER.info("ChunkGenThrottle: Activating (TPS: {}, DH: {})",
                            String.format("%.1f", tps), dhActive);
                }
            }
            
            if (throttleActive) {
                // Reset deactivation counter because we are below threshold
                stabilityCounter = 0;

                // Calculate graduated reduction: worse TPS = more reduction
                // At TPS 18 (warning), reduce by 1. At TPS 10, reduce by max.
                double severity = Math.max(0, 1.0 - (tps / warningThreshold));
                int targetReduction = Math.max(1, (int) Math.ceil(severity * maxReduction));
                targetReduction = Math.min(targetReduction, maxReduction);

                // Only increase reduction to prevent view distance oscillation
                if (targetReduction > currentReduction) {
                    currentReduction = targetReduction;
                    int newViewDist = Math.max(2, originalViewDistance - currentReduction);
                    server.getPlayerList().setViewDistance(newViewDist);
                    manager.addChunksThrottled(1);

                    ServerManagementMod.LOGGER.debug("ChunkGenThrottle: View distance {} -> {} (TPS: {})",
                            originalViewDistance, newViewDist, String.format("%.1f", tps));
                }
            }
        } else if (tps >= warningThreshold + 1.0) {
            if (throttleActive) {
                stabilityCounter++;
                // Hysteresis: wait 3x stability ticks before deactivating to prevent lag spike loops
                if (stabilityCounter >= STABILITY_TICKS * 3) {
                    stabilityCounter = 0;
                    currentReduction = 0;
                    throttleActive = false;

                    if (originalViewDistance > 0) {
                        // Only restore if auto-optimize hasn't taken over
                        if (!manager.isViewDistanceReduced()) {
                            server.getPlayerList().setViewDistance(originalViewDistance);
                        }
                        ServerManagementMod.LOGGER.info("ChunkGenThrottle: Deactivated, restored view distance to {}",
                                originalViewDistance);
                    }
                    originalViewDistance = -1;
                }
            } else {
                stabilityCounter = 0;
            }
        } else {
            // Deadband: reset counters if TPS fluctuates near the threshold
            stabilityCounter = 0;
        }
    }

    public static boolean isThrottleActive() {
        return throttleActive;
    }

    public static int getCurrentReduction() {
        return currentReduction;
    }
}
