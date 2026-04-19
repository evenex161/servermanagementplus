package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages overflow items for players whose inventory is full.
 * Items stored here persist across server restarts and player logins.
 * Players can claim items via /overflow or through the overflow GUI.
 */
public class OverflowInventoryManager {
    private static OverflowInventoryManager instance;
    private final Map<UUID, List<ItemStack>> overflowItems = new ConcurrentHashMap<>();
    private File dataDirectory;
    private MinecraftServer server;
    private volatile boolean dirty = false;
    private long lastSaveTime = 0;
    private static final long SAVE_DEBOUNCE_MS = 3000; // 3 seconds

    private OverflowInventoryManager() {}

    public static synchronized OverflowInventoryManager getInstance() {
        if (instance == null) {
            instance = new OverflowInventoryManager();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        this.dataDirectory = server.getServerDirectory().resolve("servermanagement/overflow").toFile();
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        load();
    }

    /**
     * Add an item to a player's overflow inventory
     */
    public void addItem(UUID playerId, ItemStack item) {
        if (item.isEmpty()) return;
        overflowItems.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(item.copy());
        markDirty();
        ServerManagementMod.LOGGER.info("Added overflow item for player {}: {}", playerId, item.getDisplayName().getString());
    }

    /**
     * Get all overflow items for a player
     */
    public List<ItemStack> getItems(UUID playerId) {
        List<ItemStack> items = overflowItems.get(playerId);
        if (items == null) return Collections.emptyList();
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }

    /**
     * Get the number of overflow items for a player
     */
    public int getItemCount(UUID playerId) {
        List<ItemStack> items = overflowItems.get(playerId);
        return items != null ? items.size() : 0;
    }

    /**
     * Claim a specific overflow item by index. Returns the item if successful.
     */
    public ItemStack claimItem(UUID playerId, int index) {
        List<ItemStack> items = overflowItems.get(playerId);
        if (items == null) return ItemStack.EMPTY;
        synchronized (items) {
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            ItemStack claimed = items.remove(index);
            if (items.isEmpty()) {
                overflowItems.remove(playerId);
            }
            markDirty();
            return claimed;
        }
    }

    /**
     * Safely add an item to a player's inventory, accounting for creative mode.
     * In creative mode, Inventory.add() silently destroys items and returns true
     * even when inventory is full. This method checks for actual space first.
     * @return true if the item was actually added to inventory
     */
    public static boolean safeAddToInventory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (player.getAbilities().instabuild) {
            // Creative mode: check for actual space before calling add()
            if (!hasInventorySpace(player.getInventory(), stack)) {
                return false;
            }
        }
        return player.getInventory().add(stack);
    }

    private static boolean hasInventorySpace(net.minecraft.world.entity.player.Inventory inv, ItemStack stack) {
        int remaining = stack.getCount();
        for (ItemStack invStack : inv.items) {
            if (invStack.isEmpty()) {
                return true; // Empty slot can fit the stack
            }
            if (ItemStack.isSameItemSameComponents(invStack, stack)) {
                remaining -= (invStack.getMaxStackSize() - invStack.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    /**
     * Try to deliver all overflow items to a player (e.g., on login).
     * Returns count of items that could not be delivered (still in overflow).
     */
    public int deliverItems(ServerPlayer player) {
        List<ItemStack> items = overflowItems.get(player.getUUID());
        if (items == null || items.isEmpty()) return 0;

        int remaining = 0;
        synchronized (items) {
            Iterator<ItemStack> it = items.iterator();
            while (it.hasNext()) {
                ItemStack stack = it.next();
                if (safeAddToInventory(player, stack.copy())) {
                    it.remove();
                } else {
                    remaining++;
                }
            }
            if (items.isEmpty()) {
                overflowItems.remove(player.getUUID());
            }
        }
        save();
        return remaining;
    }

    /**
     * Mark data as needing save, then save if enough time has passed.
     * Prevents disk I/O thrashing when many items are added/claimed rapidly.
     */
    private void markDirty() {
        dirty = true;
        long now = System.currentTimeMillis();
        if (now - lastSaveTime >= SAVE_DEBOUNCE_MS) {
            save();
        }
    }

    /**
     * Check if a player has any overflow items
     */
    public boolean hasItems(UUID playerId) {
        List<ItemStack> items = overflowItems.get(playerId);
        return items != null && !items.isEmpty();
    }

    public void save() {
        if (server == null || dataDirectory == null) {
            ServerManagementMod.LOGGER.error("Cannot save overflow inventory - not initialized");
            return;
        }
        dirty = false;
        lastSaveTime = System.currentTimeMillis();
        try {
            File overflowFile = new File(dataDirectory, "overflow.dat");
            CompoundTag rootTag = new CompoundTag();

            CompoundTag playersTag = new CompoundTag();
            int playerIndex = 0;
            for (Map.Entry<UUID, List<ItemStack>> entry : overflowItems.entrySet()) {
                List<ItemStack> items = entry.getValue();
                if (items.isEmpty()) continue;

                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("PlayerId", entry.getKey());

                synchronized (items) {
                    CompoundTag itemsTag = new CompoundTag();
                    for (int i = 0; i < items.size(); i++) {
                        itemsTag.put("Item" + i, items.get(i).saveOptional(
                            server.registryAccess()));
                    }
                    itemsTag.putInt("Count", items.size());
                    playerTag.put("Items", itemsTag);
                }

                playersTag.put("Player" + playerIndex, playerTag);
                playerIndex++;
            }
            playersTag.putInt("Count", playerIndex);
            rootTag.put("Players", playersTag);

            NbtIo.writeCompressed(rootTag, overflowFile.toPath());
            ServerManagementMod.LOGGER.debug("Saved overflow inventory data");
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save overflow inventory data", e);
        }
    }

    public void load() {
        if (server == null || dataDirectory == null) {
            ServerManagementMod.LOGGER.error("Cannot load overflow inventory - not initialized");
            return;
        }
        try {
            File overflowFile = new File(dataDirectory, "overflow.dat");
            if (!overflowFile.exists()) return;

            CompoundTag rootTag = NbtIo.readCompressed(overflowFile.toPath(),
                net.minecraft.nbt.NbtAccounter.create(10 * 1024 * 1024)); // 10MB limit

            CompoundTag playersTag = rootTag.getCompound("Players");
            int playerCount = playersTag.getInt("Count");
            overflowItems.clear();

            for (int p = 0; p < playerCount; p++) {
                CompoundTag playerTag = playersTag.getCompound("Player" + p);
                UUID playerId = playerTag.getUUID("PlayerId");

                CompoundTag itemsTag = playerTag.getCompound("Items");
                int itemCount = itemsTag.getInt("Count");
                List<ItemStack> items = Collections.synchronizedList(new ArrayList<>());

                for (int i = 0; i < itemCount; i++) {
                    ItemStack stack = ItemStack.parseOptional(
                        server.registryAccess(),
                        itemsTag.getCompound("Item" + i));
                    if (!stack.isEmpty()) {
                        items.add(stack);
                    }
                }

                if (!items.isEmpty()) {
                    overflowItems.put(playerId, items);
                }
            }

            ServerManagementMod.LOGGER.debug("Loaded overflow inventory data for {} players", overflowItems.size());
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to load overflow inventory data", e);
        }
    }
}
