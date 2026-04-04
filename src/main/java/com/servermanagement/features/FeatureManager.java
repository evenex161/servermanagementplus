package com.servermanagement.features;

import com.servermanagement.ServerManagementMod;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

public class FeatureManager {
    private static final Map<String, Feature> features = new HashMap<>();
    private static final Map<String, Boolean> featureStates = new HashMap<>();
    private static MinecraftServer server;

    public static void registerFeature(Feature feature) {
        features.put(feature.getId(), feature);
        featureStates.put(feature.getId(), true); // Default enabled
        ServerManagementMod.LOGGER.info("Registered feature: {}", feature.getId());
    }

    public static void initializeFeatures(MinecraftServer srv) {
        server = srv;
        
        // Load feature states from config
        loadFeatureStatesFromConfig();
        
        features.values().forEach(feature -> {
            try {
                feature.initialize(server);
                
                // Apply initial state from config
                boolean enabled = featureStates.getOrDefault(feature.getId(), true);
                if (enabled) {
                    feature.onEnable();
                } else {
                    feature.onDisable();
                }
                
                ServerManagementMod.LOGGER.info("Initialized feature: {} ({})", feature.getId(), enabled ? "enabled" : "disabled");
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to initialize feature: {}", feature.getId(), e);
            }
        });
    }
    
    private static void loadFeatureStatesFromConfig() {
        // Load feature states from config file
        featureStates.put("world_manager", com.servermanagement.config.ModConfig.WORLD_MANAGER_ENABLED.get());
        featureStates.put("player_manager", com.servermanagement.config.ModConfig.PLAYER_MANAGER_ENABLED.get());
        featureStates.put("portals", com.servermanagement.config.ModConfig.NETHER_PORTALS_ENABLED.get());
        featureStates.put("timer", com.servermanagement.config.ModConfig.WORLD_TIMERS_ENABLED.get());
        featureStates.put("slimehead", com.servermanagement.config.ModConfig.SLIME_HEADS_ENABLED.get());
        featureStates.put("chatisolation", com.servermanagement.config.ModConfig.CHAT_ISOLATION_ENABLED.get());
        featureStates.put("tablist", com.servermanagement.config.ModConfig.TAB_ISOLATION_ENABLED.get());
    }

    public static void toggleFeature(String featureId, boolean enabled) {
        if (features.containsKey(featureId)) {
            featureStates.put(featureId, enabled);
            Feature feature = features.get(featureId);
            if (enabled) {
                feature.onEnable();
            } else {
                feature.onDisable();
            }
            
            // Also update the config file
            updateConfig(featureId, enabled);
            
            ServerManagementMod.LOGGER.info("Feature {} set to: {}", featureId, enabled);
        }
    }
    
    private static void updateConfig(String featureId, boolean enabled) {
        // Update config values based on feature ID
        switch (featureId) {
            case "world_manager":
                com.servermanagement.config.ModConfig.WORLD_MANAGER_ENABLED.set(enabled);
                break;
            case "player_manager":
                com.servermanagement.config.ModConfig.PLAYER_MANAGER_ENABLED.set(enabled);
                break;
            case "portals":
                com.servermanagement.config.ModConfig.NETHER_PORTALS_ENABLED.set(enabled);
                com.servermanagement.config.ModConfig.END_PORTALS_ENABLED.set(enabled);
                break;
            case "timer":
                com.servermanagement.config.ModConfig.WORLD_TIMERS_ENABLED.set(enabled);
                break;
            case "slimehead":
                com.servermanagement.config.ModConfig.SLIME_HEADS_ENABLED.set(enabled);
                break;
            case "chatisolation":
                com.servermanagement.config.ModConfig.CHAT_ISOLATION_ENABLED.set(enabled);
                break;
            case "tablist":
                com.servermanagement.config.ModConfig.TAB_ISOLATION_ENABLED.set(enabled);
                break;
        }
        // Save config to disk
        com.servermanagement.config.ModConfig.SPEC.save();
    }

    public static boolean isFeatureEnabled(String featureId) {
        return featureStates.getOrDefault(featureId, false);
    }

    public static Map<String, Boolean> getFeatureStates() {
        return new HashMap<>(featureStates);
    }

    public static void syncFeatureStates(Map<String, Boolean> states) {
        featureStates.clear();
        featureStates.putAll(states);
        ServerManagementMod.LOGGER.debug("Synced feature states to client: {}", states);
    }

    public static Feature getFeature(String featureId) {
        return features.get(featureId);
    }

    public static Map<String, Feature> getAllFeatures() {
        return new HashMap<>(features);
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
