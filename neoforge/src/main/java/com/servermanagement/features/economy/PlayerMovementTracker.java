package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

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
    private static final Map<UUID, Double> accumulatedDistance = new ConcurrentHashMap<>();
    private static final double SAVE_THRESHOLD = 15.0;

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.tickCount % 20 != 0) return;

        UUID playerUUID = player.getUUID();
        Vec3 currentPos = player.position();
        
        Vec3 lastPos = lastPositions.get(playerUUID);
        if (lastPos != null) {
            double dx = currentPos.x - lastPos.x;
            double dz = currentPos.z - lastPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0.1) {
                double totalDistance = accumulatedDistance.getOrDefault(playerUUID, 0.0) + distance;
                accumulatedDistance.put(playerUUID, totalDistance);
                
                if (totalDistance >= SAVE_THRESHOLD) {
                    EconomyManager manager = EconomyManager.getInstance();
                    manager.getDailyTasksManager().checkAndRefreshTasks(playerUUID);
                    
                    int distanceBlocks = (int) totalDistance;
                    String completedTask = manager.getDailyTasksManager().addProgress(playerUUID, TaskType.TRAVEL_DISTANCE, distanceBlocks);
                    accumulatedDistance.put(playerUUID, totalDistance - distanceBlocks);
                    
                    if (completedTask != null) {
                        net.minecraft.server.level.ServerPlayer serverPlayer = manager.getServer().getPlayerList().getPlayer(playerUUID);
                        if (serverPlayer != null) com.servermanagement.features.economy.notifications.NotificationManager.sendTaskCompletedNotification(serverPlayer, completedTask);
                        manager.save();
                    }
                    
                    net.minecraft.server.level.ServerPlayer sp = manager.getServer().getPlayerList().getPlayer(playerUUID);
                    if (sp != null) DailyTaskProgressListener.pushSyncDailyTasks(sp);
                }
            }
        }
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
