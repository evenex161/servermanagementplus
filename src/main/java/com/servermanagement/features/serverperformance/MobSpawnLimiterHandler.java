package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class MobSpawnLimiterHandler {

    private static int spawnedThisTick = 0;
    private static long lastTickTime = 0;

    @SubscribeEvent
    public static void onMobSpawnCheck(FinalizeSpawnEvent event) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.MOB_SPAWN_LIMITER_ENABLED.get()) return;

        // Only limit natural spawns, not spawners/commands/etc.
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) return;

        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return;

        // Reset counter each tick
        long currentTick = manager.getServer().getTickCount();
        if (currentTick != lastTickTime) {
            lastTickTime = currentTick;
            spawnedThisTick = 0;
        }

        // Calculate effective spawn cap for this tick
        int multiplier = manager.getEffectiveMobCapMultiplier();
        MobCategory category = event.getEntity().getType().getCategory();
        int vanillaCap = category.getMaxInstancesPerChunk();
        int effectiveCap = Math.max(1, (vanillaCap * multiplier) / 100);

        // Limit spawns per tick based on multiplier
        int maxPerTick = Math.max(1, effectiveCap);
        if (spawnedThisTick >= maxPerTick) {
            event.setSpawnCancelled(true);
            manager.addSpawnsCancelled(1);
            return;
        }

        spawnedThisTick++;
    }
}
