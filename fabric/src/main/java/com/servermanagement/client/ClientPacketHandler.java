package com.servermanagement.client;

import com.servermanagement.network.packet.SyncEconomyTemplatesPacket;
import com.servermanagement.network.packet.SyncWorldListPacket;

import java.util.ArrayList;
import java.util.List;

public class ClientPacketHandler {    
    // Economy Settings state
    private static boolean showMarketValueTooltips = true;
    private static String tradeBlacklist = "";
    private static boolean minebayEnabled = true;
    private static boolean minestacksEnabled = true;
    
    public static boolean minebayEnabled() {
        return minebayEnabled;
    }
    
    public static void setMinebayEnabled(boolean enabled) {
        minebayEnabled = enabled;
    }
    
    public static boolean minestacksEnabled() {
        return minestacksEnabled;
    }
    
    public static void setMinestacksEnabled(boolean enabled) {
        minestacksEnabled = enabled;
    }
    
    public static boolean showMarketValueTooltips() {
        return showMarketValueTooltips;
    }
    
    public static void setShowMarketValueTooltips(boolean show) {
        showMarketValueTooltips = show;
    }
    
    public static String getTradeBlacklist() {
        return tradeBlacklist;
    }
    
    public static void setTradeBlacklist(String blacklist) {
        tradeBlacklist = blacklist;
    }
    private static List<SyncWorldListPacket.WorldInfo> cachedWorldList = new ArrayList<>();
    private static String cachedDimensionId = "";
    private static boolean cachedNetherPortalsEnabled = false;
    private static boolean cachedEndPortalsEnabled = false;
    private static boolean cachedHasTimer = false;
    private static int cachedTimerSeconds = 0;
    private static boolean cachedChatConnected = false;
    private static String cachedTimerPortalType = "both";

    /**
     * Bug 5: On Fabric, custom S2C sync packet receivers are wrapped in
     * {@code context.client().execute(...)} so they may run AFTER the vanilla
     * {@code ClientboundOpenScreen} handler that opens the menu. Screens that
     * read the cache only in their constructor or {@code init()} therefore
     * display stale defaults. After every cache update, re-run {@code init()}
     * on our own currently-open screen so widgets pick up the fresh values.
     */
    public static void refreshOpenScreen() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        net.minecraft.client.gui.screens.Screen screen = mc.screen;
        if (screen == null) {
            return;
        }
        if (!screen.getClass().getName().startsWith("com.servermanagement")) {
            return;
        }
        try {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        } catch (Throwable t) {
            // Don't take the client down if a screen rebuild misbehaves.
            com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("ScreenRefresh",
                    "failed for " + screen.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
    
    // MOTD cache
    private static String cachedMotdText = "";

    public static void handleMotdSync(String motdText) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("MOTD", "text=\"" + (motdText.length() > 60 ? motdText.substring(0, 57) + "..." : motdText) + "\"");
        cachedMotdText = motdText;
        refreshOpenScreen();
    }

    public static String getCachedMotdText() {
        return cachedMotdText;
    }

    // Global settings cache
    private static boolean cachedChatIsolationEnabled = false;
    private static boolean cachedTabIsolationEnabled = false;

    public static void handleWorldList(List<SyncWorldListPacket.WorldInfo> worlds) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("WorldList", worlds.size() + " worlds");
        cachedWorldList = new ArrayList<>(worlds);
        refreshOpenScreen();
    }

    public static List<SyncWorldListPacket.WorldInfo> getCachedWorldList() {
        return cachedWorldList;
    }

    public static void handleWorldDetail(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled,
                                          boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("WorldDetail",
                String.format("dim=%s nether=%s end=%s timer=%s(%ds) chat=%s portalType=%s",
                        dimensionId, netherPortalsEnabled, endPortalsEnabled, hasTimer, timerSeconds, chatConnected, timerPortalType));
        cachedDimensionId = dimensionId;
        cachedNetherPortalsEnabled = netherPortalsEnabled;
        cachedEndPortalsEnabled = endPortalsEnabled;
        cachedHasTimer = hasTimer;
        cachedTimerSeconds = timerSeconds;
        cachedChatConnected = chatConnected;
        cachedTimerPortalType = timerPortalType;
        refreshOpenScreen();
    }

    public static String getCachedDimensionId() {
        return cachedDimensionId;
    }

    public static boolean isNetherPortalsEnabled() {
        return cachedNetherPortalsEnabled;
    }

    public static boolean isEndPortalsEnabled() {
        return cachedEndPortalsEnabled;
    }

    public static boolean hasTimer() {
        return cachedHasTimer;
    }

    public static int getTimerSeconds() {
        return cachedTimerSeconds;
    }

    public static boolean isChatConnected() {
        return cachedChatConnected;
    }

    public static String getTimerPortalType() {
        return cachedTimerPortalType;
    }
    
    public static void handleGlobalSettings(boolean chatIsolationEnabled, boolean tabIsolationEnabled) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("GlobalSettings",
                String.format("chatIsolation=%s tabIsolation=%s", chatIsolationEnabled, tabIsolationEnabled));
        cachedChatIsolationEnabled = chatIsolationEnabled;
        cachedTabIsolationEnabled = tabIsolationEnabled;
        refreshOpenScreen();
    }
    
    public static boolean isChatIsolationEnabled() {
        return cachedChatIsolationEnabled;
    }
    
    public static boolean isTabIsolationEnabled() {
        return cachedTabIsolationEnabled;
    }
    
    // Economy template cache
    private static List<SyncEconomyTemplatesPacket.TemplateData> cachedTemplates = new ArrayList<>();
    private static int cachedFreeRewardAmount = 100;
    private static int cachedFreeRewardCooldownHours = 24;
    
    public static void handleEconomyTemplates(List<SyncEconomyTemplatesPacket.TemplateData> templates,
            int freeRewardAmount, int freeRewardCooldownHours) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("EconomyTemplates",
                String.format("%d templates, freeReward=%d, cooldown=%dh", templates.size(), freeRewardAmount, freeRewardCooldownHours));
        cachedTemplates = new ArrayList<>(templates);
        cachedFreeRewardAmount = freeRewardAmount;
        cachedFreeRewardCooldownHours = freeRewardCooldownHours;
        refreshOpenScreen();
    }
    
    public static List<SyncEconomyTemplatesPacket.TemplateData> getCachedTemplates() {
        return cachedTemplates;
    }
    
    public static int getCachedFreeRewardAmount() {
        return cachedFreeRewardAmount;
    }
    
    public static int getCachedFreeRewardCooldownHours() {
        return cachedFreeRewardCooldownHours;
    }

    // --- Economy Statistics cache ---
    private static int statTotalAccounts = 0;
    private static double statTotalMoney = 0;
    private static double statAverageBalance = 0;
    private static double statRichestBalance = 0;
    private static String statRichestPlayerName = "N/A";
    private static double statInflation = 1.0;
    private static int statActiveListings = 0;
    private static int statTotalTemplates = 0;
    private static int statEnabledTemplates = 0;
    private static int statTotalTransactions = 0;
    private static int statPurchaseCount = 0;
    private static int statSaleCount = 0;
    private static int statGamblingBetCount = 0;
    private static int statGamblingWinCount = 0;
    private static int statFreeRewardCount = 0;
    private static int statTransferCount = 0;
    private static double statTotalPurchaseVolume = 0;
    private static double statTotalSaleVolume = 0;
    private static double statTotalGamblingWagered = 0;
    private static double statTotalGamblingWon = 0;

    public static void handleEconomyStats(
            int totalAccounts, double totalMoney, double averageBalance,
            double richestBalance, String richestPlayerName, double inflation,
            int activeListings, int totalTemplates, int enabledTemplates,
            int totalTransactions, int purchaseCount, int saleCount,
            int gamblingBetCount, int gamblingWinCount, int freeRewardCount,
            int transferCount, double totalPurchaseVolume, double totalSaleVolume,
            double totalGamblingWagered, double totalGamblingWon) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("EconomyStats",
                String.format("accounts=%d totalMoney=%.0f avgBal=%.0f richest=%s(%.0f) listings=%d txns=%d",
                        totalAccounts, totalMoney, averageBalance, richestPlayerName, richestBalance, activeListings, totalTransactions));
        statTotalAccounts = totalAccounts;
        statTotalMoney = totalMoney;
        statAverageBalance = averageBalance;
        statRichestBalance = richestBalance;
        statRichestPlayerName = richestPlayerName;
        statInflation = inflation;
        statActiveListings = activeListings;
        statTotalTemplates = totalTemplates;
        statEnabledTemplates = enabledTemplates;
        statTotalTransactions = totalTransactions;
        statPurchaseCount = purchaseCount;
        statSaleCount = saleCount;
        statGamblingBetCount = gamblingBetCount;
        statGamblingWinCount = gamblingWinCount;
        statFreeRewardCount = freeRewardCount;
        statTransferCount = transferCount;
        statTotalPurchaseVolume = totalPurchaseVolume;
        statTotalSaleVolume = totalSaleVolume;
        statTotalGamblingWagered = totalGamblingWagered;
        statTotalGamblingWon = totalGamblingWon;
        refreshOpenScreen();
    }

    public static int getStatTotalAccounts() { return statTotalAccounts; }
    public static double getStatTotalMoney() { return statTotalMoney; }
    public static double getStatAverageBalance() { return statAverageBalance; }
    public static double getStatRichestBalance() { return statRichestBalance; }
    public static String getStatRichestPlayerName() { return statRichestPlayerName; }
    public static double getStatInflation() { return statInflation; }
    public static int getStatActiveListings() { return statActiveListings; }
    public static int getStatTotalTemplates() { return statTotalTemplates; }
    public static int getStatEnabledTemplates() { return statEnabledTemplates; }
    public static int getStatTotalTransactions() { return statTotalTransactions; }
    public static int getStatPurchaseCount() { return statPurchaseCount; }
    public static int getStatSaleCount() { return statSaleCount; }
    public static int getStatGamblingBetCount() { return statGamblingBetCount; }
    public static int getStatGamblingWinCount() { return statGamblingWinCount; }
    public static int getStatFreeRewardCount() { return statFreeRewardCount; }
    public static int getStatTransferCount() { return statTransferCount; }
    public static double getStatTotalPurchaseVolume() { return statTotalPurchaseVolume; }
    public static double getStatTotalSaleVolume() { return statTotalSaleVolume; }
    public static double getStatTotalGamblingWagered() { return statTotalGamblingWagered; }
    public static double getStatTotalGamblingWon() { return statTotalGamblingWon; }

    // --- Performance settings cache ---
    private static boolean perfFeatureEnabled = true;
    private static boolean perfItemMergingEnabled = true;
    private static boolean perfMobSpawnLimiterEnabled = false;
    private static boolean perfEntityActivationRangeEnabled = false;
    private static boolean perfVillagerThrottleEnabled = false;
    private static boolean perfRedstoneThrottleEnabled = false;
    private static boolean perfTpsMonitorEnabled = true;
    private static boolean perfTpsAutoOptimize = false;
    private static double perfItemMergeRadius = 3.0;
    private static int perfItemMergeInterval = 40;
    private static int perfMobCapMultiplier = 75;
    private static int perfMonsterActivationRange = 32;
    private static int perfAnimalActivationRange = 16;
    private static int perfMiscActivationRange = 8;
    private static int perfVillagerTickInterval = 3;
    private static int perfRedstoneUpdatesPerTick = 1000;
    private static double perfTpsWarningThreshold = 18.0;
    private static double perfTpsCriticalThreshold = 15.0;
    private static double perfCurrentTps = 20.0;
    private static double perfAverageMspt = 0.0;
    private static boolean perfAutoOptimizeActive = false;
    private static long perfTotalItemsMerged = 0;
    private static long perfTotalSpawnsCancelled = 0;
    private static long perfTotalEntitiesThrottled = 0;
    private static long perfTotalRedstoneThrottled = 0;

    public static void handlePerformanceSettings(
            boolean featureEnabled,
            boolean itemMergingEnabled, boolean mobSpawnLimiterEnabled,
            boolean entityActivationRangeEnabled, boolean villagerThrottleEnabled,
            boolean redstoneThrottleEnabled, boolean tpsMonitorEnabled, boolean tpsAutoOptimize,
            double itemMergeRadius, int itemMergeInterval, int mobCapMultiplier,
            int monsterActivationRange, int animalActivationRange, int miscActivationRange,
            int villagerTickInterval, int redstoneUpdatesPerTick,
            double tpsWarningThreshold, double tpsCriticalThreshold,
            double currentTps, double averageMspt, boolean autoOptimizeActive,
            long totalItemsMerged, long totalSpawnsCancelled,
            long totalEntitiesThrottled, long totalRedstoneThrottled) {
        com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("PerformanceSettings",
                String.format("enabled=%s tps=%.1f mspt=%.1f autoOpt=%s merged=%d spawns=%d throttled=%d",
                        featureEnabled, currentTps, averageMspt, autoOptimizeActive, totalItemsMerged, totalSpawnsCancelled, totalEntitiesThrottled));
        perfFeatureEnabled = featureEnabled;
        perfItemMergingEnabled = itemMergingEnabled;
        perfMobSpawnLimiterEnabled = mobSpawnLimiterEnabled;
        perfEntityActivationRangeEnabled = entityActivationRangeEnabled;
        perfVillagerThrottleEnabled = villagerThrottleEnabled;
        perfRedstoneThrottleEnabled = redstoneThrottleEnabled;
        perfTpsMonitorEnabled = tpsMonitorEnabled;
        perfTpsAutoOptimize = tpsAutoOptimize;
        perfItemMergeRadius = itemMergeRadius;
        perfItemMergeInterval = itemMergeInterval;
        perfMobCapMultiplier = mobCapMultiplier;
        perfMonsterActivationRange = monsterActivationRange;
        perfAnimalActivationRange = animalActivationRange;
        perfMiscActivationRange = miscActivationRange;
        perfVillagerTickInterval = villagerTickInterval;
        perfRedstoneUpdatesPerTick = redstoneUpdatesPerTick;
        perfTpsWarningThreshold = tpsWarningThreshold;
        perfTpsCriticalThreshold = tpsCriticalThreshold;
        perfCurrentTps = currentTps;
        perfAverageMspt = averageMspt;
        perfAutoOptimizeActive = autoOptimizeActive;
        perfTotalItemsMerged = totalItemsMerged;
        perfTotalSpawnsCancelled = totalSpawnsCancelled;
        perfTotalEntitiesThrottled = totalEntitiesThrottled;
        perfTotalRedstoneThrottled = totalRedstoneThrottled;
        refreshOpenScreen();
    }

    public static boolean getPerfFeatureEnabled() { return perfFeatureEnabled; }
    public static boolean getPerfItemMergingEnabled() { return perfItemMergingEnabled; }
    public static boolean getPerfMobSpawnLimiterEnabled() { return perfMobSpawnLimiterEnabled; }
    public static boolean getPerfEntityActivationRangeEnabled() { return perfEntityActivationRangeEnabled; }
    public static boolean getPerfVillagerThrottleEnabled() { return perfVillagerThrottleEnabled; }
    public static boolean getPerfRedstoneThrottleEnabled() { return perfRedstoneThrottleEnabled; }
    public static boolean getPerfTpsMonitorEnabled() { return perfTpsMonitorEnabled; }
    public static boolean getPerfTpsAutoOptimize() { return perfTpsAutoOptimize; }
    public static double getPerfItemMergeRadius() { return perfItemMergeRadius; }
    public static int getPerfItemMergeInterval() { return perfItemMergeInterval; }
    public static int getPerfMobCapMultiplier() { return perfMobCapMultiplier; }
    public static int getPerfMonsterActivationRange() { return perfMonsterActivationRange; }
    public static int getPerfAnimalActivationRange() { return perfAnimalActivationRange; }
    public static int getPerfMiscActivationRange() { return perfMiscActivationRange; }
    public static int getPerfVillagerTickInterval() { return perfVillagerTickInterval; }
    public static int getPerfRedstoneUpdatesPerTick() { return perfRedstoneUpdatesPerTick; }
    public static double getPerfTpsWarningThreshold() { return perfTpsWarningThreshold; }
    public static double getPerfTpsCriticalThreshold() { return perfTpsCriticalThreshold; }
    public static double getPerfCurrentTps() { return perfCurrentTps; }
    public static double getPerfAverageMspt() { return perfAverageMspt; }
    public static boolean getPerfAutoOptimizeActive() { return perfAutoOptimizeActive; }
    public static long getPerfTotalItemsMerged() { return perfTotalItemsMerged; }
    public static long getPerfTotalSpawnsCancelled() { return perfTotalSpawnsCancelled; }
    public static long getPerfTotalEntitiesThrottled() { return perfTotalEntitiesThrottled; }
    public static long getPerfTotalRedstoneThrottled() { return perfTotalRedstoneThrottled; }
}


