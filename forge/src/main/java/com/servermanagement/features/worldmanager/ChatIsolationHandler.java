package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class ChatIsolationHandler {
    
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get() || !ModConfig.CHAT_ISOLATION_ENABLED.get()) {
            return;
        }
        
        ServerPlayer sender = event.getPlayer();
        String senderDimension = sender.level().dimension().location().toString();
        WorldManagerData WorldManagerData = WorldManager.getInstance().getData();
        
        // Get connected dimensions for the sender's dimension
        List<String> connectedDimensions = WorldManagerData.getChatConnections(senderDimension);
        
        // If no isolation (no specific connections), allow global chat
        if (connectedDimensions.isEmpty()) {
            return;
        }
        
        // Cancel the default broadcast
        event.setCanceled(true);
        
        // Build the chat message
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append('<').append(sender.getName().getString()).append("> ").append(event.getMessage().getString());
        Component chatMessage = Component.literal(msgBuilder.toString());
        
        // Send to sender
        sender.sendSystemMessage(chatMessage);
        
        // Send to all players in connected dimensions (use HashSet for O(1) lookups)
        Set<String> allowedDimensions = new HashSet<>(connectedDimensions);
        allowedDimensions.add(senderDimension); // Include sender's dimension
        
        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            // Skip the sender (already sent)
            if (player.equals(sender)) {
                continue;
            }
            
            String playerDimension = player.level().dimension().location().toString();
            
            // Check if player's dimension is in the allowed set
            if (allowedDimensions.contains(playerDimension)) {
                player.sendSystemMessage(chatMessage);
            } else {
                // Check if the player's dimension has a connection back to sender's dimension
                List<String> playerConnections = WorldManagerData.getChatConnections(playerDimension);
                if (playerConnections.contains(senderDimension)) {
                    player.sendSystemMessage(chatMessage);
                }
            }
        }
        
        ServerManagementMod.LOGGER.debug("Chat message from {} in {} sent to connected dimensions: {}", 
            sender.getName().getString(), senderDimension, allowedDimensions);
    }
}
