package com.servermanagement.features.economy.notifications;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.PlayerDailyTasks;
import com.servermanagement.security.SessionManager;
import net.minecraft.server.level.ServerPlayer;
/**
 * Handles player login notifications for economy features
 */
public class LoginNotificationHandler {

    public static void onPlayerLogin(net.minecraft.server.level.ServerPlayer player) {
        // Delay notifications slightly to ensure player is fully loaded
        var server = player.level().getServer();
        if (server == null) return;
        server.execute(() -> {
            try {
                sendLoginNotifications(player);
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Error sending login notifications", e);
            }
        });
    }

    private static void sendLoginNotifications(ServerPlayer player) {
        // Sync market prices to the joining player
        EconomyManager.getInstance(player.level().getServer()).syncMarketPrices(player);
        
        // Deliver overflow items
        com.servermanagement.features.economy.OverflowInventoryManager overflow = 
            com.servermanagement.features.economy.OverflowInventoryManager.getInstance();
        if (overflow.hasItems(player.getUUID())) {
            int remaining = overflow.deliverItems(player);
            if (remaining > 0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§e[MineBay] You have " + remaining + " overflow item(s) that couldn't fit in your inventory. Use /overflow to claim them."));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a[MineBay] Overflow items have been delivered to your inventory!"));
            }
        }
        
        // Check if player is admin
        boolean isAdmin = SessionManager.getInstance().hasAdminPermission(player);
 
        // Send admin dashboard notification first if admin
        if (isAdmin) {
            NotificationManager.sendAdminDashboardNotification(player);
        }
 
        // Get economy manager
        EconomyManager economyManager = EconomyManager.getInstance(player.level().getServer());
        if (economyManager == null) {
            return;
        }

        // Check daily tasks status
        PlayerDailyTasks dailyTasks = economyManager.getDailyTasksManager()
                .getOrCreatePlayerTasks(player.getUUID());

        // Check if free reward is available
        boolean hasFreeReward = dailyTasks.isFreeRewardAvailable();

        // Count unfinished and unclaimed tasks
        int unfinishedCount = 0;
        int unclaimedCount = 0;

        for (int i = 0; i < 3; i++) {
            var task = dailyTasks.getTask(i);
            if (task != null) {
                if (!task.isClaimed()) {
                    if (task.isCompleted()) {
                        unclaimedCount++;
                    } else {
                        unfinishedCount++;
                    }
                }
            }
        }

        // Send unified daily tasks notification if there's anything to show
        if (hasFreeReward || unfinishedCount > 0 || unclaimedCount > 0) {
            NotificationManager.sendUnifiedDailyTasksNotification(player, hasFreeReward, unfinishedCount, unclaimedCount);
        }

        // Note: Bank balance notifications will be sent on actual balance changes
        // not on login, to avoid spam
    }
}
