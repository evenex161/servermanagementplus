package com.servermanagement.features;

import com.servermanagement.features.playermanager.PlayerManagerSingleton;
import com.servermanagement.features.playermanager.PlayerManagerEvents;
import net.minecraft.server.MinecraftServer;

public class PlayerManagerFeature implements Feature {

    @Override
    public String getId() {
        return "player_manager";
    }

    @Override
    public String getDisplayName() {
        return "Player Manager";
    }

    @Override
    public String getDescription() {
        return "Manage players, spectate, view inventories, and moderate the server";
    }

    @Override
    public String getDetailedDescription() {
        return "The Player Manager provides server operators with comprehensive player management tools. " +
               "Spectate any player in real-time to observe their gameplay without interference. " +
               "View player inventories to check items and assist with troubleshooting. " +
               "Kick or ban players directly from the GUI. " +
               "Access player information including location, dimension, and game mode. " +
               "Access via /playermanager command.";
    }

    @Override
    public void initialize(MinecraftServer server) {
        PlayerManagerSingleton.getInstance().initialize(server);
        PlayerManagerEvents.register();
    }

    @Override
    public void onEnable() {
        // Feature enabled
    }

    @Override
    public void onDisable() {
        // Feature disabled
    }
}
