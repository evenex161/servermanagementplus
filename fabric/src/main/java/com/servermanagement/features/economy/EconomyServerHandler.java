package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
/**
 * Handles server lifecycle events for economy system.
 * Ensures proper shutdown and periodic auto-save.
 */
public class EconomyServerHandler {
    private static int autoSaveTicks = 0;
    private static final int AUTOSAVE_INTERVAL = 12000; // 10 minutes (20 ticks/sec * 60 * 10)
    
    /** Shorter interval for market price recalculation and sync (2 minutes) */
    private static int marketSyncTicks = 0;
    private static final int MARKET_SYNC_INTERVAL = 2400; // 2 minutes (20 ticks/sec * 60 * 2)
    
    /**
     * Periodic auto-save to prevent data loss
     */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        // Fabric calls at END of tick
        
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }
        
        autoSaveTicks++;
        marketSyncTicks++;
        
        // Periodic market price recalculation and sync to all clients (every 2 minutes)
        if (marketSyncTicks >= MARKET_SYNC_INTERVAL) {
            marketSyncTicks = 0;
            
            EconomyManager manager = EconomyManager.getInstance();
            if (manager != null && server != null) {
                MarketPricingEngine.getInstance().recalculate(server);
                manager.syncMarketPricesToAll();
                ServerManagementMod.LOGGER.debug("Market prices recalculated and synced to all players");
            }
        }
        
        if (autoSaveTicks >= AUTOSAVE_INTERVAL) {
            autoSaveTicks = 0;
            
            EconomyManager manager = EconomyManager.getInstance();
            if (manager != null) {
                manager.save(); // This will be debounced by AsyncSaveScheduler
                ServerManagementMod.LOGGER.debug("Economy auto-save triggered");
            }
            
            // Periodic supply/demand save and decay
            ItemSupplyDemandTracker tracker = ItemSupplyDemandTracker.getInstance();
            if (server != null) {
                tracker.applyDecay();
                tracker.tickSave(server);
                
                // Periodic drop rate and margin history save
                DropRateTracker.getInstance().tickSave(server);
                MarginHistoryTracker.getInstance().save(server);
            }

            // Periodic session cleanup to prevent memory leak
            com.servermanagement.security.SessionManager.getInstance().cleanupExpiredSessions();
        }
    }
    
    /**
     * Ensure clean shutdown and data persistence
     */
    public static void onServerStopping(net.minecraft.server.MinecraftServer server) {
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
