package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DropRateTracker {
    private static DropRateTracker instance;
    
    public static class DropStats {
        public long broken = 0;
        public final ConcurrentHashMap<String, Long> drops = new ConcurrentHashMap<>();
    }
    
    private final ConcurrentHashMap<String, DropStats> dropData = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;
    private volatile long lastSave = 0;
    private static final long SAVE_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    
    private DropRateTracker() {}
    
    public static DropRateTracker getInstance() {
        if (instance == null) {
            instance = new DropRateTracker();
        }
        return instance;
    }
    
    public void recordDropSample(String blockId, List<ItemStack> simulatedDrops) {
        DropStats stats = dropData.computeIfAbsent(blockId, k -> new DropStats());
        stats.broken++;
        
        for (ItemStack drop : simulatedDrops) {
            if (drop.isEmpty()) continue;
            String dropId = BuiltInRegistries.ITEM.getKey(drop.getItem()).toString();
            if (!dropId.equals(blockId) && !dropId.equals("minecraft:air")) {
                stats.drops.merge(dropId, (long) drop.getCount(), Long::sum);
            }
        }
        
        // Decay to prevent overflow over very long periods
        if (stats.broken > 100_000) {
            stats.broken /= 2;
            for (Map.Entry<String, Long> entry : stats.drops.entrySet()) {
                stats.drops.put(entry.getKey(), entry.getValue() / 2);
            }
        }
        
        dirty = true;
    }
    
    public Map<String, Map<String, Double>> getAverages() {
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<String, DropStats> entry : dropData.entrySet()) {
            String blockId = entry.getKey();
            DropStats stats = entry.getValue();
            if (stats.broken == 0) continue;
            
            Map<String, Double> dropAverages = new HashMap<>();
            for (Map.Entry<String, Long> dropEntry : stats.drops.entrySet()) {
                double avg = (double) dropEntry.getValue() / stats.broken;
                if (avg > 0) {
                    dropAverages.put(dropEntry.getKey(), avg);
                }
            }
            if (!dropAverages.isEmpty()) {
                result.put(blockId, dropAverages);
            }
        }
        return result;
    }
    
    public static void onBlockBreak(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (player == null || player.level().isClientSide()) return;
        
        List<ItemStack> simulatedDrops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
        String blockId = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString();
        
        getInstance().recordDropSample(blockId, simulatedDrops);
    }
    
    // --- Persistence ---
    public void save(MinecraftServer server) {
        if (!dirty) return;
        try {
            Path dir = server.getServerDirectory().resolve("servermanagement");
            Files.createDirectories(dir);
            Path file = dir.resolve("drop_rates.dat");
            
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeInt(dropData.size());
                for (Map.Entry<String, DropStats> entry : dropData.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeLong(entry.getValue().broken);
                    out.writeInt(entry.getValue().drops.size());
                    for (Map.Entry<String, Long> dropEntry : entry.getValue().drops.entrySet()) {
                        out.writeUTF(dropEntry.getKey());
                        out.writeLong(dropEntry.getValue());
                    }
                }
            }
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save drop rate data", e);
        }
    }
    
    public void load(MinecraftServer server) {
        try {
            Path file = server.getServerDirectory().resolve("servermanagement").resolve("drop_rates.dat");
            if (!Files.exists(file)) return;
            
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                int blocks = in.readInt();
                dropData.clear();
                for (int i = 0; i < blocks; i++) {
                    String blockId = in.readUTF();
                    DropStats stats = new DropStats();
                    stats.broken = in.readLong();
                    int dropTypes = in.readInt();
                    for (int j = 0; j < dropTypes; j++) {
                        stats.drops.put(in.readUTF(), in.readLong());
                    }
                    dropData.put(blockId, stats);
                }
            }
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load drop rate data", e);
        }
    }
    
    public void tickSave(MinecraftServer server) {
        if (dirty && System.currentTimeMillis() - lastSave > SAVE_INTERVAL_MS) {
            save(server);
        }
    }
    
    public void shutdown(MinecraftServer server) {
        save(server);
        instance = null;
    }
}
