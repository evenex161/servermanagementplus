package com.servermanagement.client;

/**
 * Client-side cache for dynamic market pricing data received from the server.
 * Used by MineBayScreen to display base prices and calculate margins.
 */
public class ClientMarketData {
    private static volatile double inflationMultiplier = 1.0;
    private static volatile double averageBalance = 0.0;
    private static volatile int totalPlayerCount = 0;
    private static volatile double starterMoney = 1000.0;

    public static void update(double inflation, double avgBalance, int playerCount, double starter) {
        inflationMultiplier = inflation;
        averageBalance = avgBalance;
        totalPlayerCount = playerCount;
        starterMoney = starter;
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
     * Calculate the market base price for an item using the cached inflation multiplier.
     * Mirrors MarketPricingEngine.getBasePrice() logic on the client side.
     */
    public static double getBasePrice(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        net.minecraft.world.item.ItemStack singleItem = stack.copyWithCount(1);
        double staticValue = com.servermanagement.features.gambling.ItemValuation.getItemValue(singleItem);
        double price = staticValue * inflationMultiplier;
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
    }
}
