package com.servermanagement.network;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last processed packet timestamp for each player+action combination.
 * Implements "last write wins" logic to prevent stale packets from being applied.
 */
public class PacketTimestampTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketTimestampTracker.class);
    
    // Map of PlayerUUID -> ActionKey -> LastProcessedTick
    private static final Map<UUID, Map<String, Long>> playerActionTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Checks if a packet should be processed based on its timestamp.
     * Returns true if this packet is newer than the last processed one (or is the first).
     * 
     * @param player The player sending the packet
     * @param actionKey Unique key identifying the action (e.g., "toggle_world_manager", "portal_minecraft:the_nether")
     * @param clientTick The client tick when the action occurred
     * @return true if packet should be processed, false if it's stale
     */
    public static boolean shouldProcessPacket(ServerPlayer player, String actionKey, long clientTick) {
        UUID playerUUID = player.getUUID();
        
        // Get or create the action map for this player
        Map<String, Long> actionMap = playerActionTimestamps.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        
        // Get the last processed tick for this action (default to 0 if never processed)
        long lastProcessedTick = actionMap.getOrDefault(actionKey, 0L);
        
        // Only process if this packet is newer than the last one
        if (clientTick > lastProcessedTick) {
            // Update the last processed timestamp
            actionMap.put(actionKey, clientTick);
            return true;
        } else {
            LOGGER.warn("Discarding stale packet from {} for action '{}' with tick {} (current: {})", 
                player.getName().getString(), actionKey, clientTick, lastProcessedTick);
            return false;
        }
    }
    
    /**
     * Clears all timestamp data for a player (e.g., when they disconnect).
     * 
     * @param playerUUID The UUID of the player
     */
    public static void clearPlayer(UUID playerUUID) {
        playerActionTimestamps.remove(playerUUID);
    }
    
    /**
     * Clears all timestamp data (e.g., on server restart).
     */
    public static void clearAll() {
        playerActionTimestamps.clear();
        LOGGER.debug("Cleared all packet timestamps");
    }
}
