package com.servermanagement.features.economy;

import net.neoforged.fml.common.EventBusSubscriber;

import com.servermanagement.ServerManagementMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player movement distance for daily tasks
 * Thread-safe with ConcurrentHashMap for concurrent access
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class PlayerMovementTracker {
    private static final Map<UUID, Vec3> lastPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> accumulatedDistance = new ConcurrentHashMap<>();
    private static int tickCounter = 0;
    
    // Check movement every 40 ticks (2 seconds) to reduce performance impact
    private static final int CHECK_INTERVAL = 40;
    // Save every 50 blocks instead of 10 to drastically reduce I/O
    private static final int SAVE_THRESHOLD = 50;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only check every CHECK_INTERVAL ticks
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        UUID playerUUID = player.getUUID();
        Vec3 currentPos = player.position();
        
        // Get last position
        Vec3 lastPos = lastPositions.get(playerUUID);
        if (lastPos != null) {
            // Calculate horizontal distance traveled (ignore vertical)
            double dx = currentPos.x - lastPos.x;
            double dz = currentPos.z - lastPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            // Only count if player actually moved (not just standing/looking around)
            if (distance > 0.1) {
                int distanceBlocks = (int) distance;
                
                // Accumulate distance
                int totalDistance = accumulatedDistance.getOrDefault(playerUUID, 0) + distanceBlocks;
                accumulatedDistance.put(playerUUID, totalDistance);
                
                // Update progress every SAVE_THRESHOLD blocks to reduce saves
                if (totalDistance >= SAVE_THRESHOLD) {
                    EconomyManager manager = EconomyManager.getInstance();
                    
                    // Check and refresh tasks if needed
                    manager.getDailyTasksManager().checkAndRefreshTasks(playerUUID);
                    
                    // Add progress
                    String completedTask = manager.getDailyTasksManager()
                        .addProgress(playerUUID, TaskType.TRAVEL_DISTANCE, totalDistance);
                    
                    // Reset accumulator
                    accumulatedDistance.put(playerUUID, 0);
                    
                    // Send notification if task completed
                    if (completedTask != null) {
                        net.minecraft.server.level.ServerPlayer serverPlayer = 
                            manager.getServer().getPlayerList().getPlayer(playerUUID);
                        if (serverPlayer != null) {
                            com.servermanagement.features.economy.notifications.NotificationManager
                                .sendTaskCompletedNotification(serverPlayer, completedTask);
                        }
                        // Only save when task completes
                        manager.save();
                    }
                    // No save if task not completed - data will be saved eventually
                }
            }
        }
        
        // Update last position
        lastPositions.put(playerUUID, currentPos);
    }

    /**
     * Clean up data when player logs out
     */
    public static void cleanup(UUID playerUUID) {
        lastPositions.remove(playerUUID);
        accumulatedDistance.remove(playerUUID);
    }
}
