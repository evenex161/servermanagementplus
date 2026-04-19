package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class ItemMergeHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.ITEM_MERGING_ENABLED.get()) return;

        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return;

        tickCounter++;
        if (tickCounter < ModConfig.ITEM_MERGE_INTERVAL.get()) return;
        tickCounter = 0;

        try {
            for (ServerLevel level : manager.getServer().getAllLevels()) {
                mergeItemsInLevel(level, manager);
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Error during item merging: {}", e.getMessage());
        }
    }

    private static void mergeItemsInLevel(ServerLevel level, ServerPerformanceManager manager) {
        double radius = manager.getEffectiveItemMergeRadius();
        long merged = 0;

        // Get world border bounds as AABB
        net.minecraft.world.level.border.WorldBorder wb = level.getWorldBorder();
        AABB worldBounds = new AABB(
            wb.getMinX(), level.getMinBuildHeight(), wb.getMinZ(),
            wb.getMaxX(), level.getMaxBuildHeight(), wb.getMaxZ()
        );
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, worldBounds);

        // Cap the list to avoid O(n²) blowup with massive item accumulations
        int maxItems = Math.min(items.size(), 500);

        for (int i = 0; i < maxItems; i++) {
            ItemEntity primary = items.get(i);
            if (!primary.isAlive()) continue;

            ItemStack primaryStack = primary.getItem();
            if (primaryStack.isEmpty()) continue;

            int maxStackSize = primaryStack.getMaxStackSize();
            if (primaryStack.getCount() >= maxStackSize) continue;

            AABB searchBox = primary.getBoundingBox().inflate(radius);

            for (int j = i + 1; j < maxItems; j++) {
                ItemEntity secondary = items.get(j);
                if (!secondary.isAlive()) continue;
                if (!searchBox.contains(secondary.position())) continue;

                ItemStack secondaryStack = secondary.getItem();
                if (secondaryStack.isEmpty()) continue;

                if (!canMergeStacks(primaryStack, secondaryStack)) continue;

                int space = maxStackSize - primaryStack.getCount();
                if (space <= 0) break;

                int toTransfer = Math.min(space, secondaryStack.getCount());
                primaryStack.grow(toTransfer);
                secondaryStack.shrink(toTransfer);

                if (secondaryStack.isEmpty()) {
                    secondary.discard();
                }

                merged++;

                if (primaryStack.getCount() >= maxStackSize) break;
            }
        }

        if (merged > 0) {
            manager.addItemsMerged(merged);
        }
    }

    private static boolean canMergeStacks(ItemStack a, ItemStack b) {
        // Use ItemStack.isSameItemSameComponents which handles DataComponents in 1.21.1
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
