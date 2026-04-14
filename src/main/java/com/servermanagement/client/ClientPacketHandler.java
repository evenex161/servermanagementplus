package com.servermanagement.client;

import com.servermanagement.network.packet.SyncEconomyTemplatesPacket;
import com.servermanagement.network.packet.SyncWorldListPacket;

import java.util.ArrayList;
import java.util.List;

public class ClientPacketHandler {
    private static List<SyncWorldListPacket.WorldInfo> cachedWorldList = new ArrayList<>();
    private static String cachedDimensionId = "";
    private static boolean cachedNetherPortalsEnabled = false;
    private static boolean cachedEndPortalsEnabled = false;
    private static boolean cachedHasTimer = false;
    private static int cachedTimerSeconds = 0;
    private static boolean cachedChatConnected = false;
    private static String cachedTimerPortalType = "both";
    
    // MOTD cache
    private static String cachedMotdText = "";

    public static void handleMotdSync(String motdText) {
        cachedMotdText = motdText;
    }

    public static String getCachedMotdText() {
        return cachedMotdText;
    }

    // Global settings cache
    private static boolean cachedChatIsolationEnabled = false;
    private static boolean cachedTabIsolationEnabled = false;

    public static void handleWorldList(List<SyncWorldListPacket.WorldInfo> worlds) {
        cachedWorldList = new ArrayList<>(worlds);
    }

    public static List<SyncWorldListPacket.WorldInfo> getCachedWorldList() {
        return cachedWorldList;
    }

    public static void handleWorldDetail(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled,
                                          boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) {
        cachedDimensionId = dimensionId;
        cachedNetherPortalsEnabled = netherPortalsEnabled;
        cachedEndPortalsEnabled = endPortalsEnabled;
        cachedHasTimer = hasTimer;
        cachedTimerSeconds = timerSeconds;
        cachedChatConnected = chatConnected;
        cachedTimerPortalType = timerPortalType;
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
        cachedChatIsolationEnabled = chatIsolationEnabled;
        cachedTabIsolationEnabled = tabIsolationEnabled;
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
        cachedTemplates = new ArrayList<>(templates);
        cachedFreeRewardAmount = freeRewardAmount;
        cachedFreeRewardCooldownHours = freeRewardCooldownHours;
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
