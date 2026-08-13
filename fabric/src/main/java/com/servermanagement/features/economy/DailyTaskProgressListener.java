package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
/**
 * Tracks player progress on daily tasks
 */
public class DailyTaskProgressListener {

    public static void onBlockBreak(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (player instanceof ServerPlayer sp) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(sp.getUUID());
            
            // Track block breaking
            String completedTask = manager.getDailyTasksManager()
                .addProgress(sp.getUUID(), TaskType.BREAK_BLOCKS, 1);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(sp, completedTask);
                PerformanceMetrics.getInstance().recordTaskCompletion();
                // Only save when task completes
                manager.save();
            }
            
            // Track ore mining
            Block block = state.getBlock();
            if (isOre(block)) {
                completedTask = manager.getDailyTasksManager()
                    .addProgress(sp.getUUID(), TaskType.MINE_ORES, 1);
                
                if (completedTask != null) {
                    com.servermanagement.features.economy.notifications.NotificationManager
                        .sendTaskCompletedNotification(sp, completedTask);
                    PerformanceMetrics.getInstance().recordTaskCompletion();
                    // Only save when task completes
                    manager.save();
                }
            }
            // Removed save() call - only save on completion
            // Push live sync so the open DailyTasks GUI updates without
            // having to be closed and reopened
            pushSyncDailyTasks(sp);
        }
    }

    public static void onItemCrafted(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack crafted) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (player instanceof ServerPlayer sp) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(sp.getUUID());
            
            // Track crafting
            int count = crafted.getCount();
            String completedTask = manager.getDailyTasksManager()
                .addProgress(sp.getUUID(), TaskType.CRAFT_ITEMS, count);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(sp, completedTask);
                // Only save when task completes
                manager.save();
            }
            // Removed save() call - only save on completion
            pushSyncDailyTasks(sp);
        }
    }

    public static void onEntityKilled(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            LivingEntity killed = entity;
            
            // Only count hostile/neutral mobs, not passive animals
            if (killed instanceof net.minecraft.world.entity.monster.Monster || 
                killed instanceof net.minecraft.world.entity.monster.Enemy) {
                
                EconomyManager manager = EconomyManager.getInstance();
                
                // Check and refresh tasks if needed
                manager.getDailyTasksManager().checkAndRefreshTasks(player.getUUID());
                
                // Track mob kills
                String completedTask = manager.getDailyTasksManager()
                    .addProgress(player.getUUID(), TaskType.KILL_MOBS, 1);
                
                if (completedTask != null) {
                    com.servermanagement.features.economy.notifications.NotificationManager
                        .sendTaskCompletedNotification(player, completedTask);
                    // Only save when task completes
                    manager.save();
                }
                // Removed save() call - only save on completion
                pushSyncDailyTasks(player);
            }
        }
    }

    public static void onVillagerTrade(net.minecraft.world.entity.player.Player player) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(serverPlayer.getUUID());
            
            // Track trades
            String completedTask = manager.getDailyTasksManager()
                .addProgress(serverPlayer.getUUID(), TaskType.TRADE_VILLAGERS, 1);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(serverPlayer, completedTask);
                // Only save when task completes
                manager.save();
            }
            // Removed save() call - only save on completion
            pushSyncDailyTasks(serverPlayer);
        }
    }

    /**
     * Check if a block is an ore
     */
    private static boolean isOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
               block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE ||
               block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE ||
               block == Blocks.NETHER_GOLD_ORE || block == Blocks.NETHER_QUARTZ_ORE ||
               block == Blocks.ANCIENT_DEBRIS;
    }

    /**
     * Push the player's current daily-task state to the client so an open
     * DailyTasks GUI updates live (instead of only refreshing on next reopen).
     * Mirrors the snapshot logic in {@code OpenGuiPacket.syncDailyTasks}.
     */
    public static void pushSyncDailyTasks(ServerPlayer player) {
        try {
            EconomyManager manager = EconomyManager.getInstance();
            var dailyTasksManager = manager.getDailyTasksManager();
            var playerTasks = dailyTasksManager.getOrCreatePlayerTasks(player.getUUID());
            var templateManager = manager.getTemplateManager();
            int freeRewardAmount = templateManager != null
                ? (int) templateManager.getFreeRewardAmount()
                : playerTasks.getFreeRewardAmount();
            long resetTime = System.currentTimeMillis() + playerTasks.getTimeUntilTaskRefresh();
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncDailyTasksPacket(
                    playerTasks.getTasks(),
                    resetTime,
                    playerTasks.isFreeRewardAvailable(),
                    freeRewardAmount,
                    playerTasks.getTimeUntilFreeReward(),
                    templateManager != null ? templateManager.getFreeRewardItems() : new java.util.ArrayList<>()
                ),
                player
            );
        } catch (Throwable t) {
            ServerManagementMod.LOGGER.debug("pushSyncDailyTasks failed", t);
        }
    }
}
