package com.servermanagement.features.economy;

import net.neoforged.fml.common.EventBusSubscriber;

import com.servermanagement.ServerManagementMod;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Handles server lifecycle events for economy system.
 * Ensures proper shutdown and periodic auto-save.
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class EconomyServerHandler {
    private static int autoSaveTicks = 0;
    private static final int AUTOSAVE_INTERVAL = 12000; // 10 minutes (20 ticks/sec * 60 * 10)
    
    /**
     * Periodic auto-save to prevent data loss
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }
        
        autoSaveTicks++;
        if (autoSaveTicks >= AUTOSAVE_INTERVAL) {
            autoSaveTicks = 0;
            
            EconomyManager manager = EconomyManager.getInstance();
            if (manager != null) {
                manager.save(); // This will be debounced by AsyncSaveScheduler
                ServerManagementMod.LOGGER.debug("Economy auto-save triggered");
            }

            // Periodic session cleanup to prevent memory leak
            com.servermanagement.security.SessionManager.getInstance().cleanupExpiredSessions();
        }
    }
    
    /**
     * Ensure clean shutdown and data persistence
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager != null) {
            manager.shutdown(); // Force save and cleanup
        }

        // Clear packet timestamp tracking
        com.servermanagement.network.PacketTimestampTracker.clearAll();

        // Clean up sessions
        com.servermanagement.security.SessionManager.getInstance().cleanupExpiredSessions();
    }
}
