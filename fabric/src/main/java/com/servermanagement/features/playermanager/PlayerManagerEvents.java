package com.servermanagement.features.playermanager;

import com.servermanagement.ServerManagementMod;
public class PlayerManagerEvents {
    private static PlayerManagerEvents instance;

    public static void register() {
        if (instance == null) {
            instance = new PlayerManagerEvents();
            ServerManagementMod.LOGGER.debug("Registered PlayerManager events");
        }
    }

    public void onServerTick(net.minecraft.server.MinecraftServer server) {
        PlayerManagerSingleton.tickSpectators();
    }

    public void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        // Send fake game mode for active spectators so joining player doesn't see
        // italic+gray spectator styling in tab list
        // Delay by 1 tick so vanilla finishes sending initial player info first
        com.servermanagement.ServerManagementMod.getServer().execute(() -> PlayerManagerSingleton.onPlayerJoined(player));
    }

    public void onPlayerLeave(net.minecraft.server.level.ServerPlayer player) {
        // Clean up spectate data if player was spectating
        PlayerManagerSingleton.stopSpectate(player);
        
        // Clear packet timestamp tracking for this player
        com.servermanagement.network.PacketTimestampTracker.clearPlayer(player.getUUID());
    }
}
