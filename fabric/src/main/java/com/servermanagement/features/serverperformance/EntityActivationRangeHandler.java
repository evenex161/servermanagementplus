package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
public class EntityActivationRangeHandler {

    public static void onEntityTick(net.minecraft.world.entity.LivingEntity entity) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.get()) return;
        // Only process server-side mobs
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // Don't throttle entities that are being ridden, leashed, or in combat
        if (entity.isPassenger() || entity.isVehicle()) return;
        if (entity instanceof Mob mob && mob.getTarget() != null) return;

        // Skip villagers if villager throttle is handling them separately
        if (entity instanceof Villager && ModConfig.VILLAGER_THROTTLE_ENABLED.get()) return;

        int activationRange = getActivationRange(entity);
        if (activationRange <= 0) return;

        // Check distance to nearest player
        double nearestDistSq = Double.MAX_VALUE;
        for (ServerPlayer player : serverLevel.players()) {
            double distSq = entity.distanceToSqr(player);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
            }
        }

        double rangeSq = (double) activationRange * activationRange;

        if (nearestDistSq > rangeSq) {
            // Entity is outside activation range — only tick every 4th tick
            long tickCount = entity.level().getGameTime();
            if (tickCount % 4 != 0) {
                ServerPerformanceManager.getInstance().addEntitiesThrottled(1);
                return; // Fabric: skip tick for this entity
            }
        }
    }

    private static int getActivationRange(Entity entity) {
        if (entity instanceof Monster) {
            return ModConfig.MONSTER_ACTIVATION_RANGE.get();
        }
        if (entity instanceof Animal) {
            return ModConfig.ANIMAL_ACTIVATION_RANGE.get();
        }
        if (entity instanceof Mob) {
            return ModConfig.MISC_ACTIVATION_RANGE.get();
        }
        return 0; // Non-mob entities are not throttled
    }
}
