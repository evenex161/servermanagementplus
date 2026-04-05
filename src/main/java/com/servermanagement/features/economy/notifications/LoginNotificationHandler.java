package com.servermanagement.features.economy.notifications;

import net.neoforged.fml.common.EventBusSubscriber;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.PlayerDailyTasks;
import com.servermanagement.security.SessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Handles player login notifications for economy features
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class LoginNotificationHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Delay notifications slightly to ensure player is fully loaded
            var server = player.getServer();
            if (server == null) return;
            server.execute(() -> {
                try {
                    sendLoginNotifications(player);
                } catch (Exception e) {
                    ServerManagementMod.LOGGER.error("Error sending login notifications", e);
                }
            });
        }
    }

    private static void sendLoginNotifications(ServerPlayer player) {
        // Check if player is admin
        boolean isAdmin = SessionManager.getInstance().hasAdminPermission(player);

        // Send admin dashboard notification first if admin
        if (isAdmin) {
            NotificationManager.sendAdminDashboardNotification(player);
        }

        // Get economy manager
        EconomyManager economyManager = EconomyManager.getInstance(player.getServer());
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
