package com.servermanagement.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for dynamic market pricing data received from the server.
 * Used by MineBayScreen to display base prices and calculate margins.
 * Includes supply/demand data to match server-side pricing exactly.
 */
public class ClientMarketData {
    private static volatile double inflationMultiplier = 1.0;
    private static volatile double averageBalance = 0.0;
    private static volatile int totalPlayerCount = 0;
    private static volatile double starterMoney = 1000.0;
    private static final ConcurrentHashMap<String, Long> supplyData = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> recipePrices = new ConcurrentHashMap<>();
    
    private static final long SUPPLY_BASELINE = 500;
    private static final double DEFAULT_PRICE = 1.0;

    public static void update(double inflation, double avgBalance, int playerCount, double starter, Map<String, Long> supply, Map<String, Double> prices) {
        inflationMultiplier = inflation;
        averageBalance = avgBalance;
        totalPlayerCount = playerCount;
        starterMoney = starter;
        supplyData.clear();
        if (supply != null) {
            supplyData.putAll(supply);
        }
        recipePrices.clear();
        if (prices != null) {
            recipePrices.putAll(prices);
        }
    }

    public static double getInflationMultiplier() {
        return inflationMultiplier;
    }

    public static double getAverageBalance() {
        return averageBalance;
    }

    public static int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public static double getStarterMoney() {
        return starterMoney;
    }

    /**
     * Calculate the supply factor for an item, mirroring server-side logic.
     */
    private static double getSupplyFactor(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 1.0;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        long supply = supplyData.getOrDefault(itemId, 0L);
        
        if (supply <= SUPPLY_BASELINE) {
            if (supply <= 0) return 1.2;
            double scarcityRatio = (double) supply / SUPPLY_BASELINE;
            return 1.0 + (1.0 - scarcityRatio) * 0.2;
        }
        
        double supplyRatio = (double) supply / SUPPLY_BASELINE;
        return 1.0 / (1.0 + Math.log10(supplyRatio));
    }

    /**
     * Calculate the market base price for an item using recipe-based pricing,
     * cached inflation multiplier, and supply/demand data.
     * Mirrors MarketPricingEngine.getBasePrice() logic exactly.
     */
    public static double getBasePrice(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        net.minecraft.world.item.ItemStack singleItem = stack.copyWithCount(1);
        
        // Use recipe-based prices if available, otherwise fall back to ItemValuation
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(singleItem.getItem()).toString();
        double staticValue;
        if (!recipePrices.isEmpty()) {
            staticValue = recipePrices.getOrDefault(itemId, DEFAULT_PRICE);
        } else {
            staticValue = com.servermanagement.features.gambling.ItemValuation.getItemValue(singleItem);
        }
        
        // Apply enchantment bonus
        if (singleItem.isEnchanted()) {
            var enchantments = singleItem.getOrDefault(
                    net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            int enchantmentCount = enchantments.size();
            staticValue *= (1.0 + (enchantmentCount * 0.2));
        }
        
        // Apply durability penalty
        if (singleItem.isDamageableItem() && singleItem.getDamageValue() > 0) {
            double durabilityPercent = 1.0 - ((double) singleItem.getDamageValue() / singleItem.getMaxDamage());
            staticValue *= durabilityPercent;
        }
        
        double price = staticValue * inflationMultiplier;
        price *= getSupplyFactor(singleItem);
        return Math.max(0.50, price);
    }

    /**
     * Calculate price for the full stack.
     */
    public static double getStackPrice(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        return getBasePrice(stack) * stack.getCount();
    }

    /**
     * Calculate final price with margin.
     */
    public static double calculateFinalPrice(double basePrice, double marginPercent) {
        double clamped = Math.max(-50.0, Math.min(500.0, marginPercent));
        return Math.max(0.50, basePrice * (1.0 + clamped / 100.0));
    }

    public static void clear() {
        inflationMultiplier = 1.0;
        averageBalance = 0.0;
        totalPlayerCount = 0;
        starterMoney = 1000.0;
        supplyData.clear();
        recipePrices.clear();
    }
}
