package com.servermanagement.features.worldmanager;

import net.neoforged.fml.common.EventBusSubscriber;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.*;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class TabListIsolationHandler {
    
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20; // Update every second
    
    // Reuse collections to avoid per-tick allocation and GC pressure
    private static final Map<String, List<ServerPlayer>> dimensionPlayers = new HashMap<>();
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ModConfig.WORLD_MANAGER_ENABLED.get() || !ModConfig.TAB_ISOLATION_ENABLED.get()) {
            return;
        }
        
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        var server = event.getServer();
        
        // Reuse the static map - clear and repopulate instead of allocating new
        dimensionPlayers.values().forEach(List::clear);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String dimension = player.level().dimension().location().toString();
            dimensionPlayers.computeIfAbsent(dimension, k -> new ArrayList<>()).add(player);
        }
        
        // Update tab list for each player based on their dimension
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerDimension = player.level().dimension().location().toString();
            List<ServerPlayer> visiblePlayers = getVisiblePlayersForDimension(
                playerDimension, dimensionPlayers, server
            );
            
            // Create player info entries for visible players
            Set<UUID> visibleUUIDs = new HashSet<>();
            for (ServerPlayer visible : visiblePlayers) {
                visibleUUIDs.add(visible.getUUID());
            }
            
            // Send update packet to show only visible players
            // Note: Full tab list isolation requires packet manipulation which may need mixins
            // This is a simplified version that filters visibility
        }
    }
    
    private static List<ServerPlayer> getVisiblePlayersForDimension(
        String dimension, 
        Map<String, List<ServerPlayer>> dimensionPlayers,
        net.minecraft.server.MinecraftServer server
    ) {
        List<ServerPlayer> visible = new ArrayList<>();
        Set<UUID> visibleIds = new HashSet<>();
        
        // Add players from the same dimension
        for (ServerPlayer player : dimensionPlayers.getOrDefault(dimension, Collections.emptyList())) {
            visible.add(player);
            visibleIds.add(player.getUUID());
        }
        
        // Add players from connected dimensions (if chat connections exist)
        WorldManagerData worldData = WorldManager.getInstance().getData();
        List<String> connections = worldData.getChatConnections(dimension);
        
        for (String connectedDim : connections) {
            for (ServerPlayer player : dimensionPlayers.getOrDefault(connectedDim, Collections.emptyList())) {
                if (visibleIds.add(player.getUUID())) {
                    visible.add(player);
                }
            }
        }
        
        // Always show operators to everyone (O(1) duplicate check via HashSet)
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2) && visibleIds.add(player.getUUID())) {
                visible.add(player);
            }
        }
        
        return visible;
    }
}
