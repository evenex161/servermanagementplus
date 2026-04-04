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
}
