package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks historical margin percentages used by sellers on MineBay listings.
 * Stores per-item margin history for future use in pricing recommendations,
 * average margin calculations, and market trend analysis.
 * 
 * Each entry records: itemId, marginPercent, moneyPrice, baseMarketPrice, timestamp, sellerUUID
 */
public class MarginHistoryTracker {
    private static MarginHistoryTracker instance;
    
    // Item ID ÔåÆ list of margin entries (capped per item)
    private final ConcurrentHashMap<String, List<MarginEntry>> marginHistory = new ConcurrentHashMap<>();
    
    // Per-item averages cache (recalculated periodically)
    private final ConcurrentHashMap<String, Double> averageMargins = new ConcurrentHashMap<>();
    
    private static final int MAX_ENTRIES_PER_ITEM = 100; // Keep last 100 entries per item
    private volatile boolean dirty = false;
    
    private MarginHistoryTracker() {
    }
    
    public static MarginHistoryTracker getInstance() {
        if (instance == null) {
            instance = new MarginHistoryTracker();
        }
        return instance;
    }
    
    /**
     * Record a margin entry when a listing is created.
     */
    public void recordMargin(String itemId, double marginPercent, double moneyPrice, 
                             double baseMarketPrice, UUID sellerId) {
        MarginEntry entry = new MarginEntry(marginPercent, moneyPrice, baseMarketPrice, 
            System.currentTimeMillis(), sellerId);
        
        marginHistory.compute(itemId, (key, entries) -> {
            if (entries == null) {
                entries = new ArrayList<>();
            }
            entries.add(entry);
            // Trim to max entries (keep most recent)
            while (entries.size() > MAX_ENTRIES_PER_ITEM) {
                entries.remove(0);
            }
            return entries;
        });
        
        // Update average cache
        recalculateAverage(itemId);
        dirty = true;
    }
    
    /**
     * Record a margin using an ItemStack for convenience.
     */
    public void recordMargin(ItemStack stack, double marginPercent, double moneyPrice,
                             double baseMarketPrice, UUID sellerId) {
        if (stack.isEmpty()) return;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        recordMargin(itemId, marginPercent, moneyPrice, baseMarketPrice, sellerId);
    }
    
    /**
     * Get the average margin for an item across all historical listings.
     * Returns 10.0 (default) if no history exists.
     */
    public double getAverageMargin(String itemId) {
        return averageMargins.getOrDefault(itemId, 10.0);
    }
    
    /**
     * Get the average margin for an ItemStack.
     */
    public double getAverageMargin(ItemStack stack) {
        if (stack.isEmpty()) return 10.0;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return getAverageMargin(itemId);
    }
    
    /**
     * Get the number of margin entries for an item.
     */
    public int getEntryCount(String itemId) {
        List<MarginEntry> entries = marginHistory.get(itemId);
        return entries != null ? entries.size() : 0;
    }
    
    /**
     * Get all margin entries for an item (read-only).
     */
    public List<MarginEntry> getEntries(String itemId) {
        List<MarginEntry> entries = marginHistory.get(itemId);
        return entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }
    
    /**
     * Get global average margin across all items and all history.
     */
    public double getGlobalAverageMargin() {
        if (averageMargins.isEmpty()) return 10.0;
        double sum = 0.0;
        int count = 0;
        for (double avg : averageMargins.values()) {
            sum += avg;
            count++;
        }
        return count > 0 ? sum / count : 10.0;
    }
    
    private void recalculateAverage(String itemId) {
        List<MarginEntry> entries = marginHistory.get(itemId);
        if (entries == null || entries.isEmpty()) {
            averageMargins.remove(itemId);
            return;
        }
        double sum = 0.0;
        for (MarginEntry e : entries) {
            sum += e.marginPercent;
        }
        averageMargins.put(itemId, sum / entries.size());
    }
    
    // --- Persistence ---
    
    public void save(MinecraftServer server) {
        if (!dirty) return;
        
        try {
            Path dir = server.getServerDirectory().toPath().resolve("servermanagement");
            Files.createDirectories(dir);
            Path file = dir.resolve("margin_history.dat");
            
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeInt(marginHistory.size());
                for (Map.Entry<String, List<MarginEntry>> mapEntry : marginHistory.entrySet()) {
                    out.writeUTF(mapEntry.getKey());
                    List<MarginEntry> entries = mapEntry.getValue();
                    out.writeInt(entries.size());
                    for (MarginEntry entry : entries) {
                        out.writeDouble(entry.marginPercent);
                        out.writeDouble(entry.moneyPrice);
                        out.writeDouble(entry.baseMarketPrice);
                        out.writeLong(entry.timestamp);
                        out.writeUTF(entry.sellerId.toString());
                    }
                }
            }
            
            dirty = false;
            ServerManagementMod.LOGGER.debug("Saved margin history: {} items tracked", marginHistory.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save margin history", e);
        }
    }
    
    public void load(MinecraftServer server) {
        try {
            Path file = server.getServerDirectory().toPath()
                .resolve("servermanagement").resolve("margin_history.dat");
            
            if (!Files.exists(file)) {
                ServerManagementMod.LOGGER.debug("No margin history found, starting fresh");
                return;
            }
            
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(file)))) {
                int itemCount = in.readInt();
                marginHistory.clear();
                for (int i = 0; i < itemCount; i++) {
                    String itemId = in.readUTF();
                    int entryCount = in.readInt();
                    List<MarginEntry> entries = new ArrayList<>(entryCount);
                    for (int j = 0; j < entryCount; j++) {
                        double marginPercent = in.readDouble();
                        double moneyPrice = in.readDouble();
                        double baseMarketPrice = in.readDouble();
                        long timestamp = in.readLong();
                        UUID sellerId = UUID.fromString(in.readUTF());
                        entries.add(new MarginEntry(marginPercent, moneyPrice, baseMarketPrice, timestamp, sellerId));
                    }
                    marginHistory.put(itemId, entries);
                }
            }
            
            // Rebuild average cache
            for (String itemId : marginHistory.keySet()) {
                recalculateAverage(itemId);
            }
            
            ServerManagementMod.LOGGER.debug("Loaded margin history: {} items, global avg margin: {}%", 
                marginHistory.size(), String.format("%.1f", getGlobalAverageMargin()));
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load margin history", e);
        }
    }
    
    /**
     * Shutdown and save.
     */
    public void shutdown(MinecraftServer server) {
        save(server);
        instance = null;
    }
    
    /**
     * A single margin history entry.
     */
    public static class MarginEntry {
        public final double marginPercent;
        public final double moneyPrice;
        public final double baseMarketPrice;
        public final long timestamp;
        public final UUID sellerId;
        
        public MarginEntry(double marginPercent, double moneyPrice, double baseMarketPrice, 
                          long timestamp, UUID sellerId) {
            this.marginPercent = marginPercent;
            this.moneyPrice = moneyPrice;
            this.baseMarketPrice = baseMarketPrice;
            this.timestamp = timestamp;
            this.sellerId = sellerId;
        }
    }
}
