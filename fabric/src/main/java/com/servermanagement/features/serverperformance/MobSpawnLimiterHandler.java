package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
// Fabric: return false to cancel spawn
public class MobSpawnLimiterHandler {

    private static volatile int spawnedThisTick = 0;
    private static volatile long lastTickTime = 0;

    public static boolean onMobSpawnCheck(net.minecraft.world.entity.Mob mob) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return true;
        if (!ModConfig.MOB_SPAWN_LIMITER_ENABLED.get()) return true;
        MobSpawnType spawnType = null;
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) return true;
        ServerPerformanceManager manager = ServerPerformanceManager.getInstance();
        if (!manager.isInitialized()) return true;
        // Reset counter each tick
        long currentTick = manager.getServer().getTickCount();
        if (currentTick != lastTickTime) {
            lastTickTime = currentTick;
            spawnedThisTick = 0;
        }

        // Calculate effective spawn cap for this tick
        int multiplier = manager.getEffectiveMobCapMultiplier();
        MobCategory category = mob.getType().getCategory();
        int vanillaCap = category.getMaxInstancesPerChunk();
        int effectiveCap = Math.max(1, (vanillaCap * multiplier) / 100);

        // Limit spawns per tick based on multiplier
        int maxPerTick = Math.max(1, effectiveCap);
        if (spawnedThisTick >= maxPerTick) {
            manager.addSpawnsCancelled(1);
            return false; // Cancel spawn
        }

        spawnedThisTick++;
        return true; // Allow spawn
    }
}
