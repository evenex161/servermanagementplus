package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks item supply and demand across the server to influence dynamic market pricing.
 * Uses a Global Census Engine to continuously scan player inventories and container blocks.
 */
public class ItemSupplyDemandTracker {
    private static ItemSupplyDemandTracker instance;
    
    // Global real-time supply counts for fast lookups
    private final ConcurrentHashMap<String, Long> supplyMap = new ConcurrentHashMap<>();
    
    // Caches to track last known states for delta calculation
    private final Map<String, Map<String, Long>> playerCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> chunkCache = new ConcurrentHashMap<>();
    
    // Loaded chunks tracking for the chunk scanner
    private final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, java.util.Set<net.minecraft.world.level.ChunkPos>> loadedChunks = new ConcurrentHashMap<>();
    
    // Scanner iterators
    private java.util.Iterator<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> levelIterator = null;
    private java.util.Iterator<net.minecraft.world.level.ChunkPos> chunkIterator = null;
    private net.minecraft.server.level.ServerLevel currentScanLevel = null;

    // Baseline supply count — items below this have no price reduction
    private static final long SUPPLY_BASELINE = 500;
    
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
     * Get the supply factor for an item. Values < 1.0 reduce price (high supply),
     * values close to 1.0 mean normal supply.
     * 
     * Curve is intentionally gentle: even at 50,000 supply, items only lose ~30% value.
     * This prevents common building materials from collapsing to the floor price.
     */
    public double getSupplyFactor(ItemStack stack) {
        if (stack.isEmpty()) return 1.0;
        String itemId = getItemId(stack);
        long supply = supplyMap.getOrDefault(itemId, 0L);
        
        if (supply <= SUPPLY_BASELINE) {
            // Below baseline — slight scarcity bonus (up to 10%)
            if (supply <= 0) return 1.1; // Very scarce
            double scarcityRatio = (double) supply / SUPPLY_BASELINE;
            return 1.0 + (1.0 - scarcityRatio) * 0.1; // Up to 10% bonus
        }
        
        // Above baseline — gentle supply pressure using natural log with small coefficient.
        // At 5000 supply (10x baseline): 1/(1+0.15*ln(10)) = 1/1.345 = 0.74 (26% discount)
        // At 50000 supply (100x baseline): 1/(1+0.15*ln(100)) = 1/1.69 = 0.59 (41% discount max)
        double supplyRatio = (double) supply / SUPPLY_BASELINE;
        return 1.0 / (1.0 + 0.15 * Math.log(supplyRatio));
    }
    
    public long getSupplyCount(String itemId) {
        return supplyMap.getOrDefault(itemId, 0L);
    }
    
    public Map<String, Long> getAllSupplyData() {
        return java.util.Collections.unmodifiableMap(supplyMap);
    }

    // --- Core Census Logic ---

    private void updateSupplyFromDelta(String cacheKey, Map<String, Map<String, Long>> cache, Map<String, Long> currentItems) {
        Map<String, Long> oldItems = cache.getOrDefault(cacheKey, Collections.emptyMap());
        
        // Positive deltas (new items)
        for (Map.Entry<String, Long> entry : currentItems.entrySet()) {
            String id = entry.getKey();
            long currentCount = entry.getValue();
            long oldCount = oldItems.getOrDefault(id, 0L);
            long delta = currentCount - oldCount;
            if (delta != 0) {
                supplyMap.merge(id, delta, Long::sum);
                dirty = true;
            }
        }
        
        // Negative deltas (removed items)
        for (Map.Entry<String, Long> entry : oldItems.entrySet()) {
            String id = entry.getKey();
            if (!currentItems.containsKey(id)) {
                long delta = -entry.getValue();
                supplyMap.merge(id, delta, Long::sum);
                dirty = true;
            }
        }
        
        // Cleanup empty caches to save memory
        if (currentItems.isEmpty()) {
            cache.remove(cacheKey);
        } else {
            cache.put(cacheKey, currentItems);
        }
    }

    // --- Fabric Explicit Dispatches ---

    public void onChunkLoad(net.minecraft.server.level.ServerLevel serverLevel, net.minecraft.world.level.chunk.LevelChunk chunk) {
        loadedChunks.computeIfAbsent(serverLevel.dimension(), k -> ConcurrentHashMap.newKeySet())
            .add(chunk.getPos());
    }

    public void onChunkUnload(net.minecraft.server.level.ServerLevel serverLevel, net.minecraft.world.level.chunk.LevelChunk chunk) {
        java.util.Set<net.minecraft.world.level.ChunkPos> set = loadedChunks.get(serverLevel.dimension());
        if (set != null) {
            set.remove(chunk.getPos());
        }
    }

    public void onPlayerTick(MinecraftServer server) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.tickCount % 20 == 0) {
                scanPlayer(player);
            }
        }
    }

    public void onServerTick(MinecraftServer server) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;
        tickSave(server);
        performChunkCensusTick(server);
    }

    private void scanPlayer(net.minecraft.server.level.ServerPlayer player) {
        Map<String, Long> currentItems = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).contains("servermanagement_creative")) continue;
                String id = getItemId(stack);
                currentItems.put(id, currentItems.getOrDefault(id, 0L) + stack.getCount());
            }
        }
        updateSupplyFromDelta(player.getUUID().toString(), playerCache, currentItems);
    }

    private void performChunkCensusTick(MinecraftServer server) {
        if (levelIterator == null || !levelIterator.hasNext()) {
            levelIterator = loadedChunks.keySet().iterator();
            chunkIterator = null;
            currentScanLevel = null;
        }
        
        if (levelIterator != null && levelIterator.hasNext()) {
            if (chunkIterator == null || !chunkIterator.hasNext()) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = levelIterator.next();
                currentScanLevel = server.getLevel(dim);
                java.util.Set<net.minecraft.world.level.ChunkPos> chunks = loadedChunks.get(dim);
                if (chunks != null) {
                    chunkIterator = chunks.iterator();
                } else {
                    chunkIterator = null;
                }
            }
            
            if (chunkIterator != null && chunkIterator.hasNext() && currentScanLevel != null) {
                net.minecraft.world.level.ChunkPos pos = chunkIterator.next();
                scanChunk(currentScanLevel, pos);
            }
        }
    }

    private void scanChunk(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.ChunkPos pos) {
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) return;

        Map<String, Long> currentChunkItems = new HashMap<>();

        for (java.util.Map.Entry<net.minecraft.core.BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            net.minecraft.world.level.block.entity.BlockEntity be = entry.getValue();
            
            if (be instanceof net.minecraft.world.Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).contains("servermanagement_creative")) continue;
                        String id = getItemId(stack);
                        currentChunkItems.put(id, currentChunkItems.getOrDefault(id, 0L) + stack.getCount());
                    }
                }
            }
        }

        String chunkKey = level.dimension().location() + ":" + pos.toLong();
        updateSupplyFromDelta(chunkKey, chunkCache, currentChunkItems);
    }
    
    // --- Persistence ---
    
    public void save(MinecraftServer server) {
        if (!dirty) return;
        
        try {
            Path dir = server.getServerDirectory().resolve("servermanagement");
            Files.createDirectories(dir);
            Path file = dir.resolve("supply_demand_census.dat");
            
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeInt(1); // version
                
                out.writeInt(playerCache.size());
                for (Map.Entry<String, Map<String, Long>> pEntry : playerCache.entrySet()) {
                    out.writeUTF(pEntry.getKey());
                    out.writeInt(pEntry.getValue().size());
                    for (Map.Entry<String, Long> iEntry : pEntry.getValue().entrySet()) {
                        out.writeUTF(iEntry.getKey());
                        out.writeLong(iEntry.getValue());
                    }
                }
                
                out.writeInt(chunkCache.size());
                for (Map.Entry<String, Map<String, Long>> cEntry : chunkCache.entrySet()) {
                    out.writeUTF(cEntry.getKey());
                    out.writeInt(cEntry.getValue().size());
                    for (Map.Entry<String, Long> iEntry : cEntry.getValue().entrySet()) {
                        out.writeUTF(iEntry.getKey());
                        out.writeLong(iEntry.getValue());
                    }
                }
            }
            
            dirty = false;
            lastSave = System.currentTimeMillis();
            ServerManagementMod.LOGGER.debug("Saved Global Census data: {} items tracked globally", supplyMap.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save Global Census data", e);
        }
    }
    
    public void load(MinecraftServer server) {
        try {
            Path legacyFile = server.getServerDirectory().resolve("servermanagement").resolve("supply_demand.dat");
            if (Files.exists(legacyFile)) {
                Files.deleteIfExists(legacyFile); // Delete old event-based tracker file
            }
            
            Path file = server.getServerDirectory().resolve("servermanagement").resolve("supply_demand_census.dat");
            if (!Files.exists(file)) {
                ServerManagementMod.LOGGER.debug("No Global Census data found, starting fresh");
                return;
            }
            
            playerCache.clear();
            chunkCache.clear();
            supplyMap.clear();
            
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                int version = in.readInt(); // currently unused, reserved for future
                
                int pSize = in.readInt();
                for (int i = 0; i < pSize; i++) {
                    String playerKey = in.readUTF();
                    int itemsSize = in.readInt();
                    Map<String, Long> items = new HashMap<>();
                    for (int j = 0; j < itemsSize; j++) {
                        items.put(in.readUTF(), in.readLong());
                    }
                    playerCache.put(playerKey, items);
                }
                
                int cSize = in.readInt();
                for (int i = 0; i < cSize; i++) {
                    String chunkKey = in.readUTF();
                    int itemsSize = in.readInt();
                    Map<String, Long> items = new HashMap<>();
                    for (int j = 0; j < itemsSize; j++) {
                        items.put(in.readUTF(), in.readLong());
                    }
                    chunkCache.put(chunkKey, items);
                }
            }
            
            // Rebuild real-time supply map from caches
            for (Map<String, Long> items : playerCache.values()) {
                items.forEach((id, count) -> supplyMap.merge(id, count, Long::sum));
            }
            for (Map<String, Long> items : chunkCache.values()) {
                items.forEach((id, count) -> supplyMap.merge(id, count, Long::sum));
            }
            
            ServerManagementMod.LOGGER.debug("Loaded Global Census data: {} items tracked globally", supplyMap.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load Global Census data", e);
        }
        scanOfflinePlayers(server);
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
    
    private static String getItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private void scanOfflinePlayers(MinecraftServer server) {
        try {
            java.io.File playerDataDir = new java.io.File(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile(), "");
            if (playerDataDir.exists() && playerDataDir.isDirectory()) {
                java.io.File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".dat"));
                if (files != null) {
                    for (java.io.File file : files) {
                        String uuidStr = file.getName().replace(".dat", "");
                        if (!playerCache.containsKey(uuidStr)) {
                            try {
                                net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                                if (tag != null && tag.contains("Inventory")) {
                                    net.minecraft.nbt.ListTag inventory = tag.getList("Inventory", 10);
                                    Map<String, Long> currentItems = new java.util.HashMap<>();
                                    for (int i = 0; i < inventory.size(); i++) {
                                        net.minecraft.nbt.CompoundTag itemTag = inventory.getCompound(i);
                                        // In 1.21.1, items are saved with components
                                        java.util.Optional<ItemStack> optStack = ItemStack.parse(server.registryAccess(), itemTag);
                                        if (optStack.isPresent()) {
                                            ItemStack stack = optStack.get();
                                            if (!stack.isEmpty()) {
                                                if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).contains("servermanagement_creative")) continue;
                                                String id = getItemId(stack);
                                                currentItems.put(id, currentItems.getOrDefault(id, 0L) + stack.getCount());
                                            }
                                        }
                                    }
                                    updateSupplyFromDelta(uuidStr, playerCache, currentItems);
                                }
                            } catch (Exception e) {
                                ServerManagementMod.LOGGER.debug("Failed to read offline player data for economy census: " + uuidStr, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to scan offline players", e);
        }
    }
}
