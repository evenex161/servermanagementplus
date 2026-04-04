package com.servermanagement.features.playermanager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManagerSingleton {
    private static PlayerManagerSingleton instance;
    private MinecraftServer server;
    private final Map<UUID, SpectateData> spectating = new HashMap<>();

    private PlayerManagerSingleton() {}

    public static PlayerManagerSingleton getInstance() {
        if (instance == null) {
            instance = new PlayerManagerSingleton();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public static void spectatePlayer(ServerPlayer spectator, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            spectator.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cError: Server not initialized"));
            return;
        }
        
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            spectator.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cPlayer not found: " + targetName));
            return;
        }
        
        // Save spectator's original state
        instance.spectating.put(spectator.getUUID(), new SpectateData(
            spectator.getX(), spectator.getY(), spectator.getZ(),
            spectator.level().dimension().location().toString(),
            spectator.gameMode.getGameModeForPlayer()
        ));

        // Set to spectator mode and teleport
        spectator.setGameMode(GameType.SPECTATOR);
        spectator.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), 
            target.getYRot(), target.getXRot());
        spectator.setCamera(target);
        
        spectator.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§aNow spectating " + targetName + ". Use /stopspectate to return."));
    }

    public static void stopSpectate(ServerPlayer player) {
        SpectateData data = getInstance().spectating.remove(player.getUUID());
        if (data != null) {
            // Reset camera
            player.setCamera(player);
            
            // Restore original game mode
            player.setGameMode(data.gameMode);
            
            // Find the dimension and teleport back
            var dimensionKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new net.minecraft.resources.ResourceLocation(data.dimension)
            );
            var level = getInstance().server.getLevel(dimensionKey);
            if (level != null) {
                player.teleportTo(level, data.x, data.y, data.z, player.getYRot(), player.getXRot());
            }
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§aSpectate mode ended."));
        }
    }

    public static void viewInventory(ServerPlayer viewer, String targetName) {
        PlayerManagerSingleton instance = getInstance();
        if (instance.server == null) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cError: Server not initialized"));
            return;
        }
        
        ServerPlayer target = instance.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cPlayer not found: " + targetName));
            return;
        }
        
        // Create a menu provider for viewing the target's inventory
        viewer.openMenu(new PlayerInventoryMenuProvider(target));
    }
    
    public static boolean isSpectating(ServerPlayer player) {
        return getInstance().spectating.containsKey(player.getUUID());
    }

    private static class SpectateData {
        double x, y, z;
        String dimension;
        GameType gameMode;

        SpectateData(double x, double y, double z, String dimension, GameType gameMode) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.gameMode = gameMode;
        }
    }
}
