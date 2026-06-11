package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.DataVersion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fallback inventory for items that couldn't be delivered to player inventory.
 * Stores items from transactions, daily rewards, and tasks.
 * Unlimited capacity, accessible through Bank GUI.
 */
public class BankInventory {
    public UUID playerId; // Package-private for EconomyManager access
    private List<StoredItem> items;
    
    public BankInventory(UUID playerId) {
        this.playerId = playerId;
        this.items = new ArrayList<>();
    }
    
    /**
     * Add an item to bank inventory with source tracking
     */
    public void addItem(ItemStack item, ItemSource source, String details) {
        if (item != null && !item.isEmpty()) {
            items.add(new StoredItem(item.copy(), source, details, System.currentTimeMillis()));
        }
    }
    
    /**
     * Get all stored items
     */
    public List<StoredItem> getItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Remove an item by index
     */
    public ItemStack removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            StoredItem stored = items.remove(index);
            return stored.itemStack.copy();
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Get total number of items stored
     */
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Check if inventory is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Clear all items (admin function)
     */
    public void clear() {
        items.clear();
    }
    
    /**
     * Serialize to NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DataVersion.CURRENT_VERSION);
        com.servermanagement.util.NbtHelper.putUUID(tag, "PlayerId", playerId);
        
        ListTag itemsTag = new ListTag();
        for (StoredItem storedItem : items) {
            itemsTag.add(storedItem.toNBT());
        }
        tag.put("Items", itemsTag);
        
        return tag;
    }
    
    /**
     * Deserialize from NBT
     */
    public static BankInventory fromNBT(CompoundTag tag) {
        // Check data version
        int dataVersion = tag.getIntOr("DataVersion", 0);
        if (dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.warn("BankInventory data version {} is newer than supported version {}",
                dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        UUID playerId = com.servermanagement.util.NbtHelper.getUUID(tag, "PlayerId");
        BankInventory inventory = new BankInventory(playerId);
        
        ListTag itemsTag = tag.getListOrEmpty("Items");
        for (int i = 0; i < itemsTag.size(); i++) {
            inventory.items.add(StoredItem.fromNBT(itemsTag.getCompoundOrEmpty(i)));
        }
        
        return inventory;
    }
    
    /**
     * Item source tracking
     */
    public enum ItemSource {
        MINEBAY_PURCHASE("MineBay Purchase"),
        MINEBAY_SALE("MineBay Sale"),
        DAILY_TASK("Daily Task Reward"),
        FREE_REWARD("Free Daily Reward"),
        ACHIEVEMENT("Achievement Reward"),
        TRANSFER("Player Transfer"),
        ADMIN_GRANT("Admin Grant");
        
        private final String displayName;
        
        ItemSource(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Stored item with metadata
     */
    public static class StoredItem {
        private ItemStack itemStack;
        private ItemSource source;
        private String details;
        private long timestamp;
        
        public StoredItem(ItemStack itemStack, ItemSource source, String details, long timestamp) {
            this.itemStack = itemStack;
            this.source = source;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        public ItemStack getItemStack() {
            return itemStack.copy();
        }
        
        public ItemSource getSource() {
            return source;
        }
        
        public String getDetails() {
            return details;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("Item", com.servermanagement.util.NbtHelper.saveItemStack(itemStack, net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()));
            tag.putString("Source", source.name());
            tag.putString("Details", details);
            tag.putLong("Timestamp", timestamp);
            return tag;
        }
        
        public static StoredItem fromNBT(CompoundTag tag) {
            ItemStack item = com.servermanagement.util.NbtHelper.loadItemStack(tag.getCompoundOrEmpty("Item"), net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess());
            ItemSource source = ItemSource.valueOf(tag.getStringOr("Source", ItemSource.ADMIN_GRANT.name()));
            String details = tag.getStringOr("Details", "");
            long timestamp = tag.getLongOr("Timestamp", 0L);
            return new StoredItem(item, source, details, timestamp);
        }
    }
}
