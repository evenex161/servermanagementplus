package com.servermanagement.security;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.PacketTimestampTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles player session lifecycle (login/logout) and resource cleanup
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class SessionEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Create session for player
            SessionManager.PlayerSession session = SessionManager.getInstance().createSession(player);
            
            // Sync economy settings to client on login
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncEconomySettingsPacket(
                    com.servermanagement.config.ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.get(),
                    com.servermanagement.config.ModConfig.MINEBAY_ENABLED.get(),
                    com.servermanagement.config.ModConfig.MINESTACKS_ENABLED.get(),
                    com.servermanagement.config.ModConfig.TRADE_BLACKLIST.get(),
                    com.servermanagement.config.ModConfig.STARTING_BALANCE.get()
                ),
                player
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
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
}
