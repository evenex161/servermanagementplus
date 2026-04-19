package com.servermanagement.event;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.slimehead.SlimeHeadManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Random;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class EntityDropHandler {
    
    private static final Random RANDOM = new Random();
    private static final double DROP_CHANCE = 0.05; // 5% chance
    
    @SubscribeEvent
    public static void onEntityDrop(LivingDropsEvent event) {
        if (!ModConfig.SLIME_HEADS_ENABLED.get()) {
            return;
        }
        
        // Check if the entity is a slime
        if (event.getEntity().getType() == EntityType.SLIME) {
            // 5% chance to drop slime head
            if (RANDOM.nextDouble() < DROP_CHANCE) {
                ItemStack slimeHead = SlimeHeadManager.createSlimeHead();
                ItemEntity itemEntity = new ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    slimeHead
                );
                event.getDrops().add(itemEntity);
            }
        }
    }
}
