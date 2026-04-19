package com.servermanagement.features;

import com.servermanagement.features.worldmanager.WorldManager;
import com.servermanagement.features.worldmanager.WorldManagerEvents;
import net.minecraft.server.MinecraftServer;

public class WorldManagerFeature implements Feature {

    @Override
    public String getId() {
        return "world_manager";
    }

    @Override
    public String getDisplayName() {
        return "World Manager";
    }

    @Override
    public String getDescription() {
        return "Control world access, portals, spawn points, and dimension-specific features";
    }

    @Override
    public String getDetailedDescription() {
        return "The World Manager provides comprehensive control over server dimensions and world access. " +
               "Operators can manually activate or deactivate portals to specific dimensions like the Nether or End, " +
               "or set timers to automatically enable/disable portal access at specific times. " +
               "The World Manager GUI displays a list of all available dimensions on the server and provides quick teleportation commands. " +
               "Set a server lobby spawn point where players spawn on join or after death (when no bed respawn is set). " +
               "Configure dimension-specific chat isolation so players in different worlds cannot see each other's messages. " +
               "Control Tab list visibility to show only players in the same dimension when chat is decoupled. " +
               "Access via /worldmanager command.";
    }

    @Override
    public void initialize(MinecraftServer server) {
        WorldManager.getInstance().initialize(server);
        WorldManagerEvents.register();
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
