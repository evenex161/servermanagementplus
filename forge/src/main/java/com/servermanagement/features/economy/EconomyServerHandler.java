package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles server lifecycle events for economy system.
 * Ensures proper shutdown and periodic auto-save.
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class EconomyServerHandler {
    private static int autoSaveTicks = 0;
    private static final int AUTOSAVE_INTERVAL = 12000; // 10 minutes (20 ticks/sec * 60 * 10)
    
    /** Shorter interval for market price recalculation and sync (2 minutes) */
    private static int marketSyncTicks = 0;
    private static final int MARKET_SYNC_INTERVAL = 2400; // 2 minutes (20 ticks/sec * 60 * 2)
    
    /**
     * Periodic auto-save to prevent data loss
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }
        
        autoSaveTicks++;
        marketSyncTicks++;
        
        // Periodic market price recalculation and sync to all clients (every 2 minutes)
        if (marketSyncTicks >= MARKET_SYNC_INTERVAL) {
            marketSyncTicks = 0;
            
            EconomyManager manager = EconomyManager.getInstance();
            if (manager != null && event.getServer() != null) {
                MarketPricingEngine.getInstance().recalculate(event.getServer());
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
            
            // Periodic supply/demand save
            ItemSupplyDemandTracker tracker = ItemSupplyDemandTracker.getInstance();
            if (event.getServer() != null) {
                // Decay is handled by Global Census Engine naturally
                tracker.tickSave(event.getServer());
                DropRateTracker.getInstance().tickSave(event.getServer());
                
                // Periodic margin history save
                MarginHistoryTracker.getInstance().save(event.getServer());
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
