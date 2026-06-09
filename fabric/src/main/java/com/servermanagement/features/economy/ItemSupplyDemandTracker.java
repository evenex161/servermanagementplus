package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks item supply and demand across the server to influence dynamic market pricing.
 * 
 * Supply increases when items are:
 *   - Mined (block drops)
 *   - Picked up by players
 *   - Crafted (adds to output item supply)
 *   - Smelted (adds to output item supply)
 * 
 * Supply decreases when items are consumed as crafting/smelting inputs.
 * 
 * The supply factor influences MarketPricingEngine:
 *   supplyFactor = 1.0 / (1.0 + log10(max(supplyCount / BASELINE, 1)))
 *   Higher supply → lower prices, lower supply → higher prices.
 */
public class ItemSupplyDemandTracker {
    private static ItemSupplyDemandTracker instance;
    
    // Item ID → cumulative supply count
    private final ConcurrentHashMap<String, Long> supplyMap = new ConcurrentHashMap<>();
    
    // Baseline supply count — items below this have no price reduction
    private static final long SUPPLY_BASELINE = 500;
    
    // Decay factor: supply counts decay over time to prevent runaway deflation
    private static final double DECAY_RATE = 0.995; // 0.5% decay per save cycle
    
    // Save/load
    private volatile boolean dirty = false;
    private volatile long lastSave = 0;
    private static final long SAVE_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    
    private ItemSupplyDemandTracker() {
    }
    
    public static ItemSupplyDemandTracker getInstance() {
        if (instance == null) {
            instance = new ItemSupplyDemandTracker();
        }
        return instance;
    }
    
    /**
     * Record supply increase for an item (mined, picked up, crafted output, smelted output).
     */
    public void recordSupply(ItemStack stack, int count) {
        if (stack.isEmpty()) return;
        String itemId = getItemId(stack);
        supplyMap.merge(itemId, (long) count, Long::sum);
        dirty = true;
    }
    
    /**
     * Record supply decrease for an item (consumed as crafting/smelting input).
     */
    public void recordConsumption(ItemStack stack, int count) {
        if (stack.isEmpty()) return;
        String itemId = getItemId(stack);
        supplyMap.merge(itemId, (long) -count, Long::sum);
        // Don't go below 0
        supplyMap.computeIfPresent(itemId, (k, v) -> Math.max(0, v));
        dirty = true;
    }
    
    /**
     * Get the supply factor for an item. Values &lt;lt; 1.0 reduce price (high supply),
     * values close to 1.0 mean normal supply.
     * 
     * Formula: 1.0 / (1.0 + log10(max(supplyCount / BASELINE, 1)))
     */
    public double getSupplyFactor(ItemStack stack) {
        if (stack.isEmpty()) return 1.0;
        String itemId = getItemId(stack);
        long supply = supplyMap.getOrDefault(itemId, 0L);
        
        if (supply <= SUPPLY_BASELINE) {
            // Below baseline — scarcity bonus (slight price increase)
            if (supply <= 0) return 1.2; // Very scarce
            double scarcityRatio = (double) supply / SUPPLY_BASELINE;
            return 1.0 + (1.0 - scarcityRatio) * 0.2; // Up to 20% bonus
        }
        
        // Above baseline — supply pressure reduces price
        double supplyRatio = (double) supply / SUPPLY_BASELINE;
        return 1.0 / (1.0 + Math.log10(supplyRatio));
    }
    
    /**
     * Get the raw supply count for an item.
     */
    public long getSupplyCount(String itemId) {
        return supplyMap.getOrDefault(itemId, 0L);
    }
    
    /**
     * Get a read-only view of all supply data.
     */
    public Map<String, Long> getAllSupplyData() {
        return java.util.Collections.unmodifiableMap(supplyMap);
    }
    
    /**
     * Apply time-based decay to all supply counts to prevent runaway values.
     */
    public void applyDecay() {
        supplyMap.replaceAll((itemId, count) -> (long)(count * DECAY_RATE));
        // Remove zero entries to keep map clean
        supplyMap.entrySet().removeIf(e -> e.getValue() <= 0);
        dirty = true;
    }
    
    // --- Event Handlers ---
    
    public static void onBlockBreak(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (player == null || player.level().isClientSide()) return;
        
        // Block drops are tracked when picked up (onItemPickup), 
        // but we record the block break to increase supply of the block's item form
        ItemStack blockItem = new ItemStack(state.getBlock().asItem());
        if (!blockItem.isEmpty()) {
            getInstance().recordSupply(blockItem, 1);
        }
    }
    
    public static void onItemPickup(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (player.level().isClientSide()) return;
        
        ItemStack picked = stack;
        if (!picked.isEmpty()) {
            getInstance().recordSupply(picked, picked.getCount());
        }
    }
    
    public static void onItemCrafted(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack crafted) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (player.level().isClientSide()) return;
        
        // Record supply of crafted output
        if (!crafted.isEmpty()) {
            getInstance().recordSupply(crafted, crafted.getCount());
        }
        
        // Record consumption of crafting inputs
        net.minecraft.world.Container craftMatrix = null;
        if (craftMatrix != null) {
            for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                ItemStack input = craftMatrix.getItem(i);
                if (!input.isEmpty()) {
                    getInstance().recordConsumption(input, 1);
                }
            }
        }
    }
    
    public static void onItemSmelted(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack smelted) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        if (player.level().isClientSide()) return;
        
        // Record supply of smelted output
        if (!smelted.isEmpty()) {
            getInstance().recordSupply(smelted, smelted.getCount());
        }
    }
    
    // --- Persistence ---
    
    public void save(MinecraftServer server) {
        if (!dirty) return;
        
        try {
            Path dir = server.getServerDirectory().toPath().resolve("servermanagement");
            Files.createDirectories(dir);
            Path file = dir.resolve("supply_demand.dat");
            
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeInt(supplyMap.size());
                for (Map.Entry<String, Long> entry : supplyMap.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeLong(entry.getValue());
                }
            }
            
            dirty = false;
            lastSave = System.currentTimeMillis();
            ServerManagementMod.LOGGER.debug("Saved supply/demand data: {} items tracked", supplyMap.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save supply/demand data", e);
        }
    }
    
    public void load(MinecraftServer server) {
        try {
            Path file = server.getServerDirectory().toPath()
                .resolve("servermanagement").resolve("supply_demand.dat");
            
            if (!Files.exists(file)) {
                ServerManagementMod.LOGGER.debug("No supply/demand data found, starting fresh");
                return;
            }
            
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(file)))) {
                int count = in.readInt();
                supplyMap.clear();
                for (int i = 0; i < count; i++) {
                    String itemId = in.readUTF();
                    long supply = in.readLong();
                    if (supply > 0) {
                        supplyMap.put(itemId, supply);
                    }
                }
            }
            
            ServerManagementMod.LOGGER.debug("Loaded supply/demand data: {} items tracked", supplyMap.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load supply/demand data", e);
        }
    }
    
    /**
     * Periodic save check — called from server tick handler.
     */
    public void tickSave(MinecraftServer server) {
        if (dirty && System.currentTimeMillis() - lastSave > SAVE_INTERVAL_MS) {
            save(server);
        }
    }
    
    /**
     * Shutdown and save
     */
    public void shutdown(MinecraftServer server) {
        save(server);
        instance = null;
    }
    
    private static String getItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
