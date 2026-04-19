package com.servermanagement.features.serverperformance;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID)
public class RedstoneThrottleHandler {

    // Track redstone updates per world per tick
    private static final Map<String, AtomicInteger> updatesPerWorld = new ConcurrentHashMap<>();
    private static volatile long lastTickCount = -1;

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!ModConfig.SERVER_PERFORMANCE_ENABLED.get()) return;
        if (!ModConfig.REDSTONE_THROTTLE_ENABLED.get()) return;

        LevelAccessor level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Only throttle redstone-related updates
        BlockState state = event.getState();
        if (!isRedstoneRelated(state)) return;

        // Reset counters each tick
        long currentTick = serverLevel.getServer().getTickCount();
        if (currentTick != lastTickCount) {
            lastTickCount = currentTick;
            updatesPerWorld.values().forEach(counter -> counter.set(0));
        }

        // Track and limit updates per world
        String worldKey = serverLevel.dimension().location().toString();
        AtomicInteger counter = updatesPerWorld.computeIfAbsent(worldKey, k -> new AtomicInteger(0));

        int limit = ModConfig.REDSTONE_UPDATES_PER_TICK.get();
        int current = counter.incrementAndGet();

        if (current > limit) {
            event.setCanceled(true);
            ServerPerformanceManager.getInstance().addRedstoneThrottled(1);
        }
    }

    private static boolean isRedstoneRelated(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.REDSTONE_WIRE
            || block == Blocks.REPEATER
            || block == Blocks.COMPARATOR
            || block == Blocks.REDSTONE_TORCH
            || block == Blocks.REDSTONE_WALL_TORCH
            || block == Blocks.REDSTONE_BLOCK
            || block == Blocks.REDSTONE_LAMP
            || block == Blocks.PISTON
            || block == Blocks.STICKY_PISTON
            || block == Blocks.PISTON_HEAD
            || block == Blocks.OBSERVER
            || block == Blocks.DISPENSER
            || block == Blocks.DROPPER
            || block == Blocks.HOPPER
            || block == Blocks.POWERED_RAIL
            || block == Blocks.DETECTOR_RAIL
            || block == Blocks.ACTIVATOR_RAIL
            || block == Blocks.NOTE_BLOCK
            || block == Blocks.TNT;
    }

    public static void cleanup() {
        updatesPerWorld.clear();
    }
}
