package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Tracks player progress on daily tasks
 */
@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class DailyTaskProgressListener {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (event.getPlayer() instanceof ServerPlayer player) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(player.getUUID());
            
            // Track block breaking
            String completedTask = manager.getDailyTasksManager()
                .addProgress(player.getUUID(), TaskType.BREAK_BLOCKS, 1);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(player, completedTask);
                PerformanceMetrics.getInstance().recordTaskCompletion();
                // Only save when task completes
                manager.save();
            }
            
            // Track ore mining
            Block block = event.getState().getBlock();
            if (isOre(block)) {
                completedTask = manager.getDailyTasksManager()
                    .addProgress(player.getUUID(), TaskType.MINE_ORES, 1);
                
                if (completedTask != null) {
                    com.servermanagement.features.economy.notifications.NotificationManager
                        .sendTaskCompletedNotification(player, completedTask);
                    PerformanceMetrics.getInstance().recordTaskCompletion();
                    // Only save when task completes
                    manager.save();
                }
            }
            // Removed save() call - only save on completion
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(player.getUUID());
            
            // Track crafting
            int count = event.getCrafting().getCount();
            String completedTask = manager.getDailyTasksManager()
                .addProgress(player.getUUID(), TaskType.CRAFT_ITEMS, count);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(player, completedTask);
                // Only save when task completes
                manager.save();
            }
            // Removed save() call - only save on completion
        }
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity killed = event.getEntity();
            
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
            }
        }
    }

    @SubscribeEvent
    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            EconomyManager manager = EconomyManager.getInstance();
            
            // Check and refresh tasks if needed
            manager.getDailyTasksManager().checkAndRefreshTasks(player.getUUID());
            
            // Track trades
            String completedTask = manager.getDailyTasksManager()
                .addProgress(player.getUUID(), TaskType.TRADE_VILLAGERS, 1);
            
            if (completedTask != null) {
                com.servermanagement.features.economy.notifications.NotificationManager
                    .sendTaskCompletedNotification(player, completedTask);
                // Only save when task completes
                manager.save();
            }
            // Removed save() call - only save on completion
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
}
