package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.gambling.ItemValuation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic market pricing engine that calculates item prices based on the server economy.
 * Prices are influenced by: starter money, total player count, and average bank balance.
 *
 * Formula:
 *   inflationMultiplier = (avgBalance / starterMoney) * (1 + log10(max(playerCount, 1)))
 *   marketPrice = staticBaseValue * inflationMultiplier
 *
 * The engine recalculates periodically and provides base prices that MineBay uses
 * as the market floor. Sellers then add their own margin percentage on top.
 */
public class MarketPricingEngine {
    private static MarketPricingEngine instance;

    private volatile double inflationMultiplier = 1.0;
    private volatile double averageBalance = 0.0;
    private volatile int totalPlayerCount = 0;
    private volatile double starterMoney = 1000.0;
    private volatile long lastRecalculation = 0;

    private static final long RECALCULATE_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final double MIN_INFLATION = 0.1;
    private static final double MAX_INFLATION = 50.0;
    private static final double FLOOR_PRICE = 0.50; // Minimum price for any item

    private MarketPricingEngine() {
    }

    public static MarketPricingEngine getInstance() {
        if (instance == null) {
            instance = new MarketPricingEngine();
        }
        return instance;
    }

    /**
     * Recalculate the inflation multiplier based on current economy state.
     * Called periodically or when significant economy events occur.
     */
    public void recalculate(MinecraftServer server) {
        try {
            EconomyManager economy = EconomyManager.getInstance();
            if (economy.getData() == null) return;

            Map<UUID, BankAccount> allAccounts = economy.getData().getAllAccounts();
            this.totalPlayerCount = allAccounts.size();
            this.starterMoney = ModConfig.STARTING_BALANCE.get();

            if (totalPlayerCount == 0 || starterMoney <= 0) {
                this.inflationMultiplier = 1.0;
                this.averageBalance = starterMoney;
                this.lastRecalculation = System.currentTimeMillis();
                return;
            }

            // Calculate average balance across all players
            double totalBalance = 0.0;
            for (BankAccount account : allAccounts.values()) {
                totalBalance += account.getBalance();
            }
            this.averageBalance = totalBalance / totalPlayerCount;

            // inflation = (avgBalance / starterMoney) * (1 + log10(playerCount))
            double balanceRatio = averageBalance / starterMoney;
            double playerFactor = 1.0 + Math.log10(Math.max(totalPlayerCount, 1));
            this.inflationMultiplier = Math.max(MIN_INFLATION,
                    Math.min(MAX_INFLATION, balanceRatio * playerFactor));

            this.lastRecalculation = System.currentTimeMillis();

            ServerManagementMod.LOGGER.debug(
                    "Market recalculated: inflation={}, avgBalance={}, players={}, starterMoney={}",
                    String.format("%.3f", inflationMultiplier),
                    String.format("%.2f", averageBalance),
                    totalPlayerCount,
                    String.format("%.2f", starterMoney));
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to recalculate market prices", e);
        }
    }

    /**
     * Ensure prices are up-to-date, recalculating if stale.
     */
    public void ensureFresh(MinecraftServer server) {
        if (System.currentTimeMillis() - lastRecalculation > RECALCULATE_INTERVAL_MS) {
            recalculate(server);
        }
    }

    /**
     * Get the dynamic market base price for an item stack (single item, count=1).
     * Uses ItemValuation's static values scaled by inflation and supply/demand factors.
     * 
     * Formula: marketPrice = staticValue * inflationMultiplier * supplyFactor
     */
    public double getBasePrice(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        // Get static base value from ItemValuation (for a single item, ignoring stack count)
        ItemStack singleItem = stack.copyWithCount(1);
        double staticValue = ItemValuation.getItemValue(singleItem);

        // Apply inflation multiplier
        double marketPrice = staticValue * inflationMultiplier;
        
        // Apply supply/demand factor
        double supplyFactor = ItemSupplyDemandTracker.getInstance().getSupplyFactor(singleItem);
        marketPrice *= supplyFactor;

        return Math.max(FLOOR_PRICE, marketPrice);
    }

    /**
     * Get the market price for the full stack (price per item * count).
     */
    public double getStackPrice(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        return getBasePrice(stack) * stack.getCount();
    }

    /**
     * Calculate the final listing price with a seller margin.
     * @param basePrice the market base price
     * @param marginPercent the seller's markup percentage (0-500%)
     * @return the final price buyers must pay
     */
    public static double calculateFinalPrice(double basePrice, double marginPercent) {
        double clamped = Math.max(-50.0, Math.min(200.0, marginPercent));
        return Math.max(FLOOR_PRICE, basePrice * (1.0 + clamped / 100.0));
    }

    // --- Getters for market data (used by sync packet) ---

    public double getInflationMultiplier() {
        return inflationMultiplier;
    }

    public double getAverageBalance() {
        return averageBalance;
    }

    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public double getStarterMoney() {
        return starterMoney;
    }

    public long getLastRecalculation() {
        return lastRecalculation;
    }

    /**
     * Shutdown and clear instance
     */
    public void shutdown() {
        instance = null;
    }
}
