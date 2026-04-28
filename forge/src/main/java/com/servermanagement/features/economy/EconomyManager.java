package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.AsyncSaveScheduler;
import com.servermanagement.util.ExpiringCache;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Central manager for the Economy feature - handles bank accounts, transactions, and balance operations.
 * Optimized with batched saves and caching for better performance.
 */
public class EconomyManager {
    private static EconomyManager instance;
    private EconomyData data;
    private AchievementRewardTracker achievementTracker;
    private DailyTasksManager dailyTasksManager;
    private DailyTaskTemplateManager templateManager;
    private MoneyRequestManager requestManager;
    private MinecraftServer server;
    
    // Bank inventories for all players
    private final java.util.Map<UUID, BankInventory> bankInventories = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Performance optimizations
    private AsyncSaveScheduler saveScheduler;
    private ExpiringCache<UUID, Double> balanceCache;
    private volatile boolean dirty = false;
    private static final long SAVE_DEBOUNCE_MS = 5000; // 5 seconds
    private static final long CACHE_TTL_MS = 30000; // 30 seconds

    private EconomyManager() {
    }

    public static synchronized EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }

    /**
     * Get instance by server (ensures initialization)
     */
    public static synchronized EconomyManager getInstance(MinecraftServer server) {
        EconomyManager manager = getInstance();
        if (manager.server == null && server != null) {
            manager.initialize(server);
        }
        return manager;
    }

    /**
     * Initialize the economy system
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        
        // Initialize save scheduler with 2 threads and 5 second debounce
        this.saveScheduler = new AsyncSaveScheduler(2, SAVE_DEBOUNCE_MS);
        
        // Initialize balance cache (1000 entries, 30 second TTL)
        this.balanceCache = new ExpiringCache<>(1000, CACHE_TTL_MS);
        
        // Load data
        this.data = EconomyData.load(server);
        this.achievementTracker = AchievementRewardTracker.load(server);
        this.templateManager = DailyTaskTemplateManager.load(server);
        this.dailyTasksManager = DailyTasksManager.load(server);
        this.requestManager = MoneyRequestManager.load(server);
        
        // Link template manager to daily tasks manager
        this.dailyTasksManager.setTemplateManager(templateManager);
        
        // Load bank inventories
        loadBankInventories();
        
        // Initialize recipe-based pricing (needs RecipeManager, available after datapack load)
        RecipeBasedPricing.getInstance().initialize(server);
        
        // Initialize market pricing engine
        MarketPricingEngine.getInstance().recalculate(server);
        
        // Load supply/demand tracking data
        ItemSupplyDemandTracker.getInstance().load(server);
        
        // Load margin history data
        MarginHistoryTracker.getInstance().load(server);
        
        ServerManagementMod.LOGGER.info("Economy system initialized with performance optimizations");
    }

    /**
     * Sync market prices to a specific player (call on player join or when MineBay opens)
     */
    public void syncMarketPrices(ServerPlayer player) {
        MarketPricingEngine engine = MarketPricingEngine.getInstance();
        engine.ensureFresh(server);
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncMarketPricesPacket(
                engine.getInflationMultiplier(),
                engine.getAverageBalance(),
                engine.getTotalPlayerCount(),
                engine.getStarterMoney(),
                ItemSupplyDemandTracker.getInstance().getAllSupplyData(),
                RecipeBasedPricing.getInstance().getAllPrices()
            ),
            player
        );
    }

    /**
     * Sync market prices to all online players
     */
    public void syncMarketPricesToAll() {
        MarketPricingEngine engine = MarketPricingEngine.getInstance();
        engine.ensureFresh(server);
        com.servermanagement.network.ModNetworking.sendToAllPlayers(
            new com.servermanagement.network.packet.SyncMarketPricesPacket(
                engine.getInflationMultiplier(),
                engine.getAverageBalance(),
                engine.getTotalPlayerCount(),
                engine.getStarterMoney(),
                ItemSupplyDemandTracker.getInstance().getAllSupplyData(),
                RecipeBasedPricing.getInstance().getAllPrices()
            )
        );
    }

    /**
     * Save economy data asynchronously with debouncing
     */
    public void save() {
        if (!dirty) return; // Skip if no changes
        
        dirty = false;
        
        if (saveScheduler != null) {
            // Schedule batched saves
            if (data != null && server != null) {
                saveScheduler.scheduleSave("economy_data", () -> data.save(server));
            }
            if (achievementTracker != null && server != null) {
                saveScheduler.scheduleSave("achievement_tracker", () -> achievementTracker.save(server));
            }
            if (templateManager != null && server != null) {
                saveScheduler.scheduleSave("template_manager", () -> templateManager.save(server));
            }
            if (dailyTasksManager != null && server != null) {
                saveScheduler.scheduleSave("daily_tasks", () -> dailyTasksManager.save(server));
            }
            if (requestManager != null && server != null) {
                saveScheduler.scheduleSave("money_requests", () -> requestManager.save(server));
            }
            // Schedule bank inventories save
            if (!bankInventories.isEmpty() && server != null) {
                saveScheduler.scheduleSave("bank_inventories", this::saveBankInventories);
            }
        } else {
            // Fallback to synchronous save if scheduler not initialized
            saveSync();
        }
    }

    /**
     * Force synchronous save (used during shutdown)
     */
    public void saveSync() {
        if (data != null && server != null) {
            data.save(server);
        }
        if (achievementTracker != null && server != null) {
            achievementTracker.save(server);
        }
        if (templateManager != null && server != null) {
            templateManager.save(server);
        }
        if (dailyTasksManager != null && server != null) {
            dailyTasksManager.save(server);
        }
        if (requestManager != null && server != null) {
            requestManager.save(server);
        }
        // Save bank inventories
        saveBankInventories();
        
        dirty = false;
    }

    /**
     * Shutdown economy system gracefully
     */
    public void shutdown() {
        ServerManagementMod.LOGGER.info("Shutting down economy system");
        
        if (saveScheduler != null) {
            try {
                saveScheduler.flushAll(); // Force save all pending
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to flush pending saves during shutdown", e);
            }
            saveScheduler.shutdown();
        }
        
        // Final synchronous save to ensure data persistence
        try {
            saveSync();
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save economy data during shutdown — data may be lost", e);
        }
        
        // Clear caches
        if (balanceCache != null) {
            balanceCache.clear();
        }
        
        // Save and shutdown supply/demand tracker
        if (server != null) {
            ItemSupplyDemandTracker.getInstance().shutdown(server);
            MarginHistoryTracker.getInstance().shutdown(server);
        }
    }

    /**
     * Mark data as dirty (needs save)
     */
    private void markDirty() {
        this.dirty = true;
    }

    /**
     * Get or create a bank account for a player
     */
    public BankAccount getOrCreateAccount(UUID playerUUID) {
        return data.getOrCreateAccount(playerUUID);
    }

    /**
     * Get a player's balance (with caching)
     */
    public double getBalance(UUID playerUUID) {
        // Check cache first
        if (balanceCache != null) {
            Double cachedBalance = balanceCache.get(playerUUID);
            if (cachedBalance != null) {
                return cachedBalance;
            }
        }
        
        // Cache miss - fetch from data
        BankAccount account = data.getAccount(playerUUID);
        double balance = account != null ? account.getBalance() : 0.0;
        
        // Update cache
        if (balanceCache != null) {
            balanceCache.put(playerUUID, balance);
        }
        
        return balance;
    }

    /**
     * Set a player's balance (admin operation)
     */
    public void setBalance(UUID playerUUID, double amount) {
        BankAccount account = getOrCreateAccount(playerUUID);
        account.setBalance(amount);
        
        // Invalidate cache
        if (balanceCache != null) {
            balanceCache.invalidate(playerUUID);
        }
        
        markDirty();
        save();
    }

    /**
     * Add money to a player's account
     * @return true if successful
     */
    public boolean deposit(UUID playerUUID, double amount, TransactionType type, String description) {
        if (amount <= 0) return false;
        
        BankAccount account = getOrCreateAccount(playerUUID);
        if (account.deposit(amount)) {
            account.addTransaction(new Transaction(type, amount, description));
            
            // Invalidate cache
            if (balanceCache != null) {
                balanceCache.invalidate(playerUUID);
            }
            
            markDirty();
            save();
            return true;
        }
        return false;
    }

    /**
     * Remove money from a player's account
     * @return true if successful, false if insufficient funds
     */
    public boolean withdraw(UUID playerUUID, double amount, TransactionType type, String description) {
        if (amount <= 0) return false;
        
        BankAccount account = data.getAccount(playerUUID);
        if (account != null && account.withdraw(amount)) {
            account.addTransaction(new Transaction(type, amount, description));
            
            // Invalidate cache
            if (balanceCache != null) {
                balanceCache.invalidate(playerUUID);
            }
            
            markDirty();
            save();
            return true;
        }
        return false;
    }

    /**
     * Transfer money between players
     * @return true if successful
     */
    public boolean transfer(UUID fromUUID, UUID toUUID, double amount) {
        if (amount <= 0) return false;
        if (fromUUID.equals(toUUID)) return false;
        
        BankAccount fromAccount = data.getAccount(fromUUID);
        if (fromAccount == null || fromAccount.getBalance() < amount) {
            return false; // Insufficient funds
        }
        
        BankAccount toAccount = getOrCreateAccount(toUUID);
        
        // Perform transfer
        if (fromAccount.withdraw(amount)) {
            toAccount.deposit(amount);
            
            // Record transactions
            String toName = getPlayerName(toUUID);
            String fromName = getPlayerName(fromUUID);
            
            fromAccount.addTransaction(new Transaction(
                TransactionType.PLAYER_TRANSFER_SENT,
                amount,
                "Sent to " + toName,
                toUUID
            ));
            
            toAccount.addTransaction(new Transaction(
                TransactionType.PLAYER_TRANSFER_RECEIVED,
                amount,
                "Received from " + fromName,
                fromUUID
            ));
            
            // Invalidate both caches
            if (balanceCache != null) {
                balanceCache.invalidate(fromUUID);
                balanceCache.invalidate(toUUID);
            }
            
            markDirty();
            save();
            return true;
        }
        
        return false;
    }

    /**
     * Get player name for transactions (falls back to UUID if offline)
     */
    private String getPlayerName(UUID playerUUID) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return playerUUID.toString().substring(0, 8);
    }

    /**
     * Get the data instance (for advanced operations)
     */
    public EconomyData getData() {
        return data;
    }

    /**
     * Get the server instance
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Get the achievement tracker
     */
    public AchievementRewardTracker getAchievementTracker() {
        return achievementTracker;
    }

    /**
     * Get the daily tasks manager
     */
    public DailyTasksManager getDailyTasksManager() {
        return dailyTasksManager;
    }

    /**
     * Get the daily task template manager
     */
    public DailyTaskTemplateManager getTemplateManager() {
        return templateManager;
    }

    /**
     * Get the money request manager
     */
    public MoneyRequestManager getRequestManager() {
        return requestManager;
    }
    
    /**
     * Get or create a bank inventory for a player
     */
    public BankInventory getBankInventory(UUID playerId) {
        return bankInventories.computeIfAbsent(playerId, BankInventory::new);
    }
    
    /**
     * Load bank inventories from disk
     */
    private void loadBankInventories() {
        try {
            java.io.File bankInvDir = new java.io.File(server.getServerDirectory(), "servermanagement/bankinventories");
            if (!bankInvDir.exists()) {
                return;
            }
            
            java.io.File[] files = bankInvDir.listFiles((dir, name) -> name.endsWith(".dat"));
            if (files != null) {
                for (java.io.File file : files) {
                    try {
                        net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(file);
                        BankInventory inventory = BankInventory.fromNBT(tag);
                        bankInventories.put(inventory.playerId, inventory);
                    } catch (Exception e) {
                        ServerManagementMod.LOGGER.error("Failed to load bank inventory: {}", file.getName(), e);
                    }
                }
            }
            ServerManagementMod.LOGGER.info("Loaded {} bank inventories", bankInventories.size());
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to load bank inventories", e);
        }
    }
    
    /**
     * Save all bank inventories to disk
     */
    public void saveBankInventories() {
        try {
            java.io.File bankInvDir = new java.io.File(server.getServerDirectory(), "servermanagement/bankinventories");
            if (!bankInvDir.exists()) {
                bankInvDir.mkdirs();
            }
            
            for (BankInventory inventory : bankInventories.values()) {
                if (!inventory.isEmpty()) {
                    java.io.File file = new java.io.File(bankInvDir, inventory.playerId.toString() + ".dat");
                    net.minecraft.nbt.NbtIo.writeCompressed(inventory.toNBT(), file);
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save bank inventories", e);
        }
    }
}
