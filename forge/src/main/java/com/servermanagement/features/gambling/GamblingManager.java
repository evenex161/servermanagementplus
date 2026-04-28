package com.servermanagement.features.gambling;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.util.AsyncSaveScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the MineStacks gambling system
 */
public class GamblingManager {
    private static GamblingManager instance;
    private MinecraftServer server;
    private Map<UUID, GamblingStats> playerStats = new ConcurrentHashMap<>();
    private File statsFile;
    private AsyncSaveScheduler saveScheduler;
    
    // House edge percentages (1.0 = 100%)
    public static final double SLOT_MACHINE_HOUSE_EDGE = 0.05; // 5% house edge
    public static final double COIN_FLIP_HOUSE_EDGE = 0.02; // 2% house edge
    public static final double DICE_ROLL_HOUSE_EDGE = 0.03; // 3% house edge
    public static final double ROULETTE_HOUSE_EDGE = 0.027; // 2.7% house edge
    
    // Minimum and maximum bets
    public static final double MIN_BET = 10.0;
    public static final double MAX_BET = 10000.0;
    
    private GamblingManager() {}
    
    public static GamblingManager getInstance() {
        if (instance == null) {
            instance = new GamblingManager();
        }
        return instance;
    }
    
    public void initialize(MinecraftServer server) {
        this.server = server;
        File serverDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File dataDir = new File(serverDir, "data/servermanagement");
        dataDir.mkdirs();
        
        this.statsFile = new File(dataDir, "gambling_stats.dat");
        this.saveScheduler = new AsyncSaveScheduler(1, 5000); // 5 second debounce
        loadStats();
    }
    
    /**
     * Get or create gambling stats for a player
     */
    public GamblingStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, id -> new GamblingStats(id));
    }
    
    /**
     * Place a bet with money
     */
    public GamblingResult placeBet(ServerPlayer player, double amount, GamblingGame game) {
        if (amount < MIN_BET || amount > MAX_BET) {
            return new GamblingResult(false, 0.0, "Bet must be between $" + (int)MIN_BET + " and $" + (int)MAX_BET);
        }
        
        // Check if player has enough money
        BankAccount account = EconomyManager.getInstance().getOrCreateAccount(player.getUUID());
        
        // Synchronize on the account to ensure atomic withdraw+deposit
        synchronized (account) {
            if (account.getBalance() < amount) {
                return new GamblingResult(false, 0.0, "Insufficient funds");
            }
            
            // Deduct bet amount
            account.withdraw(amount);
            
            // Play the game
            double payout = game.play(amount);
            
            // Validate payout to prevent exploits
            if (Double.isNaN(payout) || Double.isInfinite(payout) || payout < 0) {
                account.deposit(amount); // Refund bet
                return new GamblingResult(false, 0.0, "Game error - bet refunded");
            }
            
            boolean won = payout > amount;
            double profit = payout - amount;
            
            // Add payout to account
            if (payout > 0) {
                account.deposit(payout);
            }
            
            // Record transactions
            account.addTransaction(new Transaction(
                TransactionType.GAMBLING_BET, amount,
                game.getName() + " bet"));
            if (won) {
                account.addTransaction(new Transaction(
                    TransactionType.GAMBLING_WIN, profit,
                    game.getName() + " win (+$" + String.format("%.2f", profit) + ")"));
            }
            
            // Update stats
            GamblingStats stats = getStats(player.getUUID());
            stats.recordBet(amount, profit, won);
            saveStats();
            
            String message = won ? 
                "You won $" + String.format("%.2f", profit) + "!" :
                "You lost $" + String.format("%.2f", Math.abs(profit));
            
            return new GamblingResult(won, payout, message);
        }
    }
    
    /**
     * Place a bet with an item (item is valued using market pricing engine)
     */
    public GamblingResult placeBetWithItem(ServerPlayer player, ItemStack item, GamblingGame game) {
        // Use MarketPricingEngine for item value (consistent with GUI display)
        double itemValue = com.servermanagement.features.economy.MarketPricingEngine.getInstance()
            .getBasePrice(item) * item.getCount();
        
        if (itemValue < MIN_BET) {
            return new GamblingResult(false, 0.0, "Item value too low (min $" + (int)MIN_BET + ")");
        }
        
        if (itemValue > MAX_BET) {
            return new GamblingResult(false, 0.0, "Item value too high (max $" + (int)MAX_BET + ")");
        }
        
        // Remove item from player
        item.shrink(item.getCount());
        
        // Play the game
        double payout = game.play(itemValue);
        
        // Validate payout to prevent exploits
        if (Double.isNaN(payout) || Double.isInfinite(payout) || payout < 0) {
            // Refund item value as money since item is already consumed
            BankAccount refundAccount = EconomyManager.getInstance().getOrCreateAccount(player.getUUID());
            refundAccount.deposit(itemValue);
            refundAccount.addTransaction(new Transaction(
                TransactionType.GAMBLING_BET, itemValue,
                game.getName() + " error - refunded as money"));
            return new GamblingResult(false, 0.0, "Game error - bet refunded as money ($" + String.format("%.2f", itemValue) + ")");
        }
        
        boolean won = payout > itemValue;
        double profit = payout - itemValue;
        
        // Add payout to bank account
        if (payout > 0) {
            BankAccount account = EconomyManager.getInstance().getOrCreateAccount(player.getUUID());
            account.deposit(payout);
            
            // Record transactions
            account.addTransaction(new Transaction(
                TransactionType.GAMBLING_BET, itemValue,
                game.getName() + " bet (item)"));
            if (won) {
                account.addTransaction(new Transaction(
                    TransactionType.GAMBLING_WIN, profit,
                    game.getName() + " win (+$" + String.format("%.2f", profit) + ")"));
            }
        } else {
            // Lost everything - still record the bet
            BankAccount account = EconomyManager.getInstance().getOrCreateAccount(player.getUUID());
            account.addTransaction(new Transaction(
                TransactionType.GAMBLING_BET, itemValue,
                game.getName() + " bet (item) - lost"));
        }
        
        // Update stats
        GamblingStats stats = getStats(player.getUUID());
        stats.recordBet(itemValue, profit, won);
        saveStats();
        
        // Note: Transaction logging removed - gambling is separate from economy transactions
        
        String message = won ? 
            "You won $" + String.format("%.2f", profit) + "!" :
            "You lost $" + String.format("%.2f", Math.abs(profit));
        
        return new GamblingResult(won, payout, message);
    }
    
    /**
     * Load gambling stats from file
     */
    private void loadStats() {
        if (!statsFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(statsFile))) {
            @SuppressWarnings("unchecked")
            Map<UUID, GamblingStats> loaded = (Map<UUID, GamblingStats>) ois.readObject();
            playerStats = new ConcurrentHashMap<>(loaded);
            ServerManagementMod.LOGGER.info("Loaded gambling stats for {} players", playerStats.size());
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to load gambling stats", e);
        }
    }
    
    /**
     * Save gambling stats to file (debounced to avoid excessive disk I/O)
     */
    private void saveStats() {
        if (saveScheduler != null) {
            saveScheduler.scheduleSave("gambling_stats", this::doSaveStats);
        } else {
            doSaveStats();
        }
    }

    private void doSaveStats() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(statsFile))) {
            oos.writeObject(new HashMap<>(playerStats));
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save gambling stats", e);
        }
    }

    /**
     * Force save immediately (used during shutdown)
     */
    public void forceSave() {
        doSaveStats();
    }
    
    /**
     * Get the server instance
     */
    public MinecraftServer getServer() {
        return server;
    }
}
