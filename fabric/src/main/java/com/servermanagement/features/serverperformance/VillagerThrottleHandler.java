package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
public class VillagerThrottleHandler {

    public static void onVillagerTick(net.minecraft.world.entity.LivingEntity entity) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.VILLAGER_THROTTLE_ENABLED.get()) return;
        if (!(entity instanceof Villager)) return;
        if (entity.level().isClientSide()) return;

        // Don't throttle villagers that are trading with a player
        Villager villager = (Villager) entity;
        Player tradingPlayer = villager.getTradingPlayer();
        if (tradingPlayer != null) return;

        // Throttle: only fully tick every N ticks
        int interval = ServerPerformanceManager.getInstance().getEffectiveVillagerTickInterval();
        long gameTime = entity.level().getGameTime();

        // Stagger different villagers using their entity ID to avoid all ticking on the same frame
        int offset = entity.getId() % interval;
        if ((gameTime + offset) % interval != 0) {
            ServerPerformanceManager.getInstance().addEntitiesThrottled(1);
            return; // Fabric: skip tick for this entity
        }
    }
}
