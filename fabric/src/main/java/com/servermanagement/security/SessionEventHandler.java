package com.servermanagement.security;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.PacketTimestampTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/**
 * Handles player session lifecycle (login/logout) and resource cleanup
 */
public class SessionEventHandler {

    public static void onPlayerLogin(net.minecraft.server.level.ServerPlayer player) {
        // Create session for player
        SessionManager.PlayerSession session = SessionManager.getInstance().createSession(player);
        
        // Send session token to client via packet
        com.servermanagement.network.ModNetworking.sendToPlayer(
            player, 
            new com.servermanagement.network.packet.SyncSessionTokenPacket(session.getToken())
        );
    }

    public static void onPlayerLogout(net.minecraft.server.level.ServerPlayer player) {
        java.util.UUID playerUUID = player.getUUID();

        // Invalidate player session
        SessionManager.getInstance().invalidateSession(playerUUID);

        // Clear packet timestamp tracking to prevent memory leak
        PacketTimestampTracker.clearPlayer(playerUUID);

        // Clean up MineBay held items and drafts
        try {
            com.servermanagement.features.minebay.MineBayManager mineBay =
                com.servermanagement.features.minebay.MineBayManager.getInstance();
            // Return held item to player before disconnecting
            ItemStack heldItem = mineBay.releaseHeldItem(playerUUID);
            if (!heldItem.isEmpty()) {
                // Give it back or drop it at their location
                if (!player.getInventory().add(heldItem)) {
                    player.drop(heldItem, false);
                }
            }
            mineBay.clearDraft(playerUUID);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Error cleaning up MineBay data for player {}", playerUUID, e);
        }

        ServerManagementMod.LOGGER.debug("Cleaned up resources for player: {}", player.getName().getString());
    }
}
