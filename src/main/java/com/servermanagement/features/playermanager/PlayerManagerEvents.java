package com.servermanagement.features.playermanager;

import com.servermanagement.ServerManagementMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerManagerEvents {
    private static PlayerManagerEvents instance;

    public static void register() {
        if (instance == null) {
            instance = new PlayerManagerEvents();
            MinecraftForge.EVENT_BUS.register(instance);
            ServerManagementMod.LOGGER.info("Registered PlayerManager events");
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerManagerSingleton.tickSpectators();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Send fake game mode for active spectators so joining player doesn't see
        // italic+gray spectator styling in tab list
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            // Delay by 1 tick so vanilla finishes sending initial player info first
            player.getServer().execute(() -> PlayerManagerSingleton.onPlayerJoined(player));
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // Clean up spectate data if player was spectating
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PlayerManagerSingleton.stopSpectate(player);
            
            // Clear packet timestamp tracking for this player
            com.servermanagement.network.PacketTimestampTracker.clearPlayer(player.getUUID());
        }
    }
}
