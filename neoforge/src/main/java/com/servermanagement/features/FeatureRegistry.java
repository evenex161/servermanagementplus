package com.servermanagement.features;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.PlayerPreferences;
import net.minecraft.server.MinecraftServer;

public class FeatureRegistry {
    
    public static void registerFeatures() {
        // Register WorldManager feature
        FeatureManager.registerFeature(new com.servermanagement.features.WorldManagerFeature());
        
        // Register PlayerManager feature
        FeatureManager.registerFeature(new com.servermanagement.features.PlayerManagerFeature());
        
        // Register SlimeHead feature
        FeatureManager.registerFeature(new com.servermanagement.features.slimehead.SlimeHeadManager());
        
        // Register Economy feature
        FeatureManager.registerFeature(new com.servermanagement.features.economy.EconomyFeature());
        
        // Register Server Performance feature
        FeatureManager.registerFeature(new com.servermanagement.features.serverperformance.ServerPerformanceFeature());
    }
    
    public static void initializeFeatures(MinecraftServer server) {
        // Initialize player preferences
        PlayerPreferences.initialize(server);
        
        // Initialize all registered features
        FeatureManager.initializeFeatures(server);
    }
}
