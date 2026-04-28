package com.servermanagement.features.serverperformance;

import com.servermanagement.features.Feature;
import net.minecraft.server.MinecraftServer;

public class ServerPerformanceFeature implements Feature {

    @Override
    public String getId() {
        return "server_performance";
    }

    @Override
    public String getDisplayName() {
        return "Server Performance";
    }

    @Override
    public String getDescription() {
        return "Configurable server performance optimizations including entity management and TPS monitoring";
    }

    @Override
    public String getDetailedDescription() {
        return "The Server Performance system provides a suite of configurable optimizations to improve server tick rate " +
               "without interfering with other mods. Features include: Item Entity Merging (combine nearby dropped items), " +
               "Mob Spawn Rate Limiting (reduce mob spawn rates), Entity Activation Range (throttle distant entity AI), " +
               "Villager AI Throttle (reduce villager brain tick frequency), Redstone Update Throttle (prevent lag machines), " +
               "and a real-time TPS Monitor with admin warnings and optional auto-optimization. " +
               "All settings are individually toggleable via the config file. " +
               "Use /sm performance to view live TPS stats and optimization status.";
    }

    @Override
    public void initialize(MinecraftServer server) {
        ServerPerformanceManager.getInstance().initialize(server);
    }

    @Override
    public void onEnable() {
        // Performance handlers are registered via @EventBusSubscriber and check config at runtime
    }

    @Override
    public void onDisable() {
        // Handlers will early-return when disabled
    }
}
