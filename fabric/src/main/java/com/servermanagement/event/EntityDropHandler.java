package com.servermanagement.event;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.slimehead.SlimeHeadManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

public class EntityDropHandler {
    
    private static final Random RANDOM = new Random();
    private static final double DROP_CHANCE = 0.05; // 5% chance
    
    public static void onEntityDrop(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, java.util.Collection<net.minecraft.world.entity.item.ItemEntity> drops, boolean recentlyHit) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        // Check if the entity is a slime
        if (entity.getType() == EntityType.SLIME) {
            // 5% chance to drop slime head
            if (RANDOM.nextDouble() < DROP_CHANCE) {
                ItemStack slimeHead = SlimeHeadManager.createSlimeHead();
                ItemEntity itemEntity = new ItemEntity(
                    entity.level(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    slimeHead
                );
                drops.add(itemEntity);
            }
        }
    }
}
