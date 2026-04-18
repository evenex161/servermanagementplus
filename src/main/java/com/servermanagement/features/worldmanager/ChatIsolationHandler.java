package com.servermanagement.features.worldmanager;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        // Master feature toggle (Forge config)
        if (!ModConfig.WORLD_MANAGER_ENABLED.get() || !ModConfig.CHAT_ISOLATION_ENABLED.get()) {
            return;
        }
        
        // Runtime toggle (set by admin via GUI)
        WorldManagerData worldData = WorldManager.getInstance().getData();
        if (!worldData.isChatIsolationEnabled()) {
            return;
        }
        
        ServerPlayer sender = event.getPlayer();
        String senderDimension = sender.level().dimension().location().toString();
        
        // Cancel the default global broadcast
        event.setCanceled(true);
        
        // Build the chat message with dimension prefix
        MutableComponent dimPrefix = getDimensionPrefix(senderDimension);
        MutableComponent chatMessage = dimPrefix
            .append(Component.literal(" <" + sender.getName().getString() + "> ").withStyle(ChatFormatting.RESET))
            .append(Component.literal(event.getMessage().getString()).withStyle(ChatFormatting.WHITE));
        
        // Build allowed dimensions: sender's dimension + any connected dimensions
        Set<String> allowedDimensions = new HashSet<>();
        allowedDimensions.add(senderDimension);
        
        // Add connected dimensions (cross-dimension chat bridges, if configured)
        List<String> connections = worldData.getChatConnections(senderDimension);
        allowedDimensions.addAll(connections);
        
        // Send to all players in allowed dimensions
        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            String playerDimension = player.level().dimension().location().toString();
            
            if (allowedDimensions.contains(playerDimension)) {
                player.sendSystemMessage(chatMessage);
            } else {
                // Check if the player's dimension has a reverse connection to sender's dimension
                List<String> playerConnections = worldData.getChatConnections(playerDimension);
                if (playerConnections.contains(senderDimension)) {
                    player.sendSystemMessage(chatMessage);
                }
            }
        }
        
        ServerManagementMod.LOGGER.debug("Chat isolation: message from {} in {} sent to dimensions: {}", 
            sender.getName().getString(), senderDimension, allowedDimensions);
    }
    
    /**
     * Returns a colored dimension prefix like [Overworld], [Nether], [The End].
     * Colors are intuitive: green=Overworld, red=Nether, magenta=End, aqua=custom.
     */
    private static MutableComponent getDimensionPrefix(String dimensionId) {
        String name;
        ChatFormatting color;
        
        switch (dimensionId) {
            case "minecraft:overworld" -> { name = "Overworld"; color = ChatFormatting.GREEN; }
            case "minecraft:the_nether" -> { name = "Nether"; color = ChatFormatting.RED; }
            case "minecraft:the_end" -> { name = "The End"; color = ChatFormatting.LIGHT_PURPLE; }
            default -> {
                // Custom/modded dimensions — derive a readable name
                String raw = dimensionId.contains(":") ? dimensionId.substring(dimensionId.indexOf(':') + 1) : dimensionId;
                name = formatDimensionName(raw);
                color = ChatFormatting.AQUA;
            }
        }
        
        return Component.literal("[").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(name).withStyle(color))
            .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
    }
    
    /**
     * Converts "the_nether" style IDs to "The Nether" display names.
     */
    private static String formatDimensionName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
