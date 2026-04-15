package com.servermanagement.features.minebay;

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
 * Manages all MineBay listings and offers with persistent file storage
 */
public class MineBayManager {
    private static MineBayManager instance;
    private final Map<String, MineBayListing> activeListings = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> heldItems = new ConcurrentHashMap<>(); // Items being held during listing creation
    private final Map<UUID, MineBayListingDraft> draftListings = new ConcurrentHashMap<>(); // Persistent drafts
    private File dataDirectory;
    
    private MineBayManager() {}
    
    public static synchronized MineBayManager getInstance() {
        if (instance == null) {
            instance = new MineBayManager();
        }
        return instance;
    }
    
    public void initialize(MinecraftServer server) {
        this.dataDirectory = server.getServerDirectory().resolve("servermanagement/minebay").toFile();
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        load();
    }
    
    /**
     * Create a new listing draft for a player
     */
    public MineBayListingDraft createDraft(UUID playerId) {
        MineBayListingDraft draft = new MineBayListingDraft(playerId);
        draftListings.put(playerId, draft);
        return draft;
    }
    
    /**
     * Get existing draft or create new one
     */
    public MineBayListingDraft getDraft(UUID playerId) {
        return draftListings.computeIfAbsent(playerId, MineBayListingDraft::new);
    }
    
    /**
     * Hold an item temporarily during listing creation
     */
    public void holdItem(UUID playerId, ItemStack item) {
        heldItems.put(playerId, item.copy());
        ServerManagementMod.LOGGER.debug("Holding item for player {}: {}", playerId, item);
    }
    
    /**
     * Get the held item for a player
     */
    public ItemStack getHeldItem(UUID playerId) {
        ItemStack item = heldItems.get(playerId);
        return item != null ? item.copy() : ItemStack.EMPTY;
    }
    
    /**
     * Release held item back to player
     */
    public ItemStack releaseHeldItem(UUID playerId) {
        ItemStack item = heldItems.remove(playerId);
        ServerManagementMod.LOGGER.debug("Released held item for player {}", playerId);
        return item != null ? item : ItemStack.EMPTY;
    }
    
    /**
     * Clear draft without returning item
     */
    public void clearDraft(UUID playerId) {
        draftListings.remove(playerId);
    }
    
    /**
     * Create a listing from a completed draft.
     * Returns null if the player has reached their max listing limit.
     */
    public synchronized MineBayListing createListing(UUID sellerId, String sellerName, ItemStack item,
                                        double moneyPrice, List<PriceItemEntry> priceItems,
                                        MineBayListing.OfferType offerType) {
        // Enforce max listings per player (synchronized to prevent TOCTOU race)
        int maxListings = com.servermanagement.config.ModConfig.MAX_LISTINGS_PER_PLAYER.get();
        int activeCount = 0;
        for (MineBayListing l : activeListings.values()) {
            if (l.getSellerId().equals(sellerId) && l.getStatus() == MineBayListing.ListingStatus.ACTIVE) {
                activeCount++;
            }
        }
        if (activeCount >= maxListings) {
            ServerManagementMod.LOGGER.info("Player {} has reached max listings limit ({})", sellerName, maxListings);
            return null;
        }

        MineBayListing listing = new MineBayListing(sellerId, sellerName, item, moneyPrice, priceItems, offerType);
        activeListings.put(listing.getListingId(), listing);
        
        // Clear draft and held item
        draftListings.remove(sellerId);
        heldItems.remove(sellerId);
        
        save();
        return listing;
    }
    
    /**
     * Get all active listings
     */
    public List<MineBayListing> getActiveListings() {
        List<MineBayListing> result = new ArrayList<>();
        for (MineBayListing l : activeListings.values()) {
            if (l.getStatus() == MineBayListing.ListingStatus.ACTIVE) {
                result.add(l);
            }
        }
        result.sort(Comparator.comparingLong(MineBayListing::getCreatedTimestamp).reversed());
        return result;
    }
    
    /**
     * Get listings by seller
     */
    public List<MineBayListing> getListingsBySeller(UUID sellerId) {
        List<MineBayListing> result = new ArrayList<>();
        for (MineBayListing l : activeListings.values()) {
            if (l.getSellerId().equals(sellerId)) {
                result.add(l);
            }
        }
        result.sort(Comparator.comparingLong(MineBayListing::getCreatedTimestamp).reversed());
        return result;
    }
    
    /**
     * Get a specific listing
     */
    public MineBayListing getListing(String listingId) {
        return activeListings.get(listingId);
    }
    
    /**
     * Remove a listing (after purchase or cancellation)
     */
    public void removeListing(String listingId) {
        activeListings.remove(listingId);
        save();
        ServerManagementMod.LOGGER.info("Removed listing: {}", listingId);
    }
    
    /**
     * Sync listings to all online players
     */
    public void syncListingsToAllPlayers(MinecraftServer server) {
        List<MineBayListing> listings = getActiveListings();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket(listings),
                player
            );
        }
    }
    
    /**
     * Cancel a listing and return item to seller
     */
    public ItemStack cancelListing(String listingId, ServerPlayer seller) {
        MineBayListing listing = activeListings.get(listingId);
        if (listing != null && listing.getSellerId().equals(seller.getUUID())) {
            listing.setStatus(MineBayListing.ListingStatus.CANCELLED);
            ItemStack returnItem = listing.getItemForSale();
            activeListings.remove(listingId);
            save();
            ServerManagementMod.LOGGER.info("Cancelled listing: {}", listingId);
            return returnItem;
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Add a counteroffer to a listing
     */
    public boolean addCounteroffer(String listingId, MineBayOffer offer) {
        MineBayListing listing = activeListings.get(listingId);
        if (listing != null && listing.getOfferType() == MineBayListing.OfferType.NEGOTIABLE) {
            listing.addCounteroffer(offer);
            save();
            ServerManagementMod.LOGGER.info("Added counteroffer to listing {}: {}", listingId, offer.getOfferId());
            return true;
        }
        return false;
    }
    
    /**
     * Accept an offer (fixed price or counteroffer)
     */
    public boolean acceptOffer(String listingId, String offerId, ServerPlayer buyer) {
        MineBayListing listing = activeListings.get(listingId);
        if (listing == null || listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
            return false;
        }
        
        // Mark listing as completed
        listing.setStatus(MineBayListing.ListingStatus.COMPLETED);
        
        if (offerId != null) {
            // Mark specific counteroffer as accepted
            for (MineBayOffer o : listing.getCounteroffers()) {
                if (o.getOfferId().equals(offerId)) {
                    o.setStatus(MineBayOffer.OfferStatus.ACCEPTED);
                    break;
                }
            }
        }
        
        save();
        ServerManagementMod.LOGGER.info("Accepted offer for listing: {}", listingId);
        return true;
    }
    
    /**
     * Save all listings to file
     */
    public void save() {
        try {
            File listingsFile = new File(dataDirectory, "listings.dat");
            CompoundTag rootTag = new CompoundTag();
            
            // Save active listings
            CompoundTag listingsTag = new CompoundTag();
            int index = 0;
            for (MineBayListing listing : activeListings.values()) {
                listingsTag.put("Listing" + index, listing.toNBT());
                index++;
            }
            listingsTag.putInt("Count", index);
            rootTag.put("Listings", listingsTag);
            
            // Save held items
            CompoundTag heldItemsTag = new CompoundTag();
            index = 0;
            for (Map.Entry<UUID, ItemStack> entry : heldItems.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("PlayerId", entry.getKey());
                entryTag.put("Item", entry.getValue().saveOptional(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()));
                heldItemsTag.put("Held" + index, entryTag);
                index++;
            }
            heldItemsTag.putInt("Count", index);
            rootTag.put("HeldItems", heldItemsTag);
            
            NbtIo.writeCompressed(rootTag, listingsFile.toPath());
            ServerManagementMod.LOGGER.debug("Saved {} MineBay listings", activeListings.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save MineBay data", e);
        }
    }
    
    /**
     * Save a specific listing (updates in-memory map and saves to disk)
     */
    public void saveListing(MineBayListing listing) {
        if (listing != null && listing.getListingId() != null) {
            activeListings.put(listing.getListingId(), listing);
            save();
        }
    }
    
    /**
     * Load all listings from file
     */
    public void load() {
        try {
            File listingsFile = new File(dataDirectory, "listings.dat");
            if (!listingsFile.exists()) {
                return;
            }
            
            CompoundTag rootTag = NbtIo.readCompressed(listingsFile.toPath(), net.minecraft.nbt.NbtAccounter.create(10 * 1024 * 1024));
            
            // Load active listings
            CompoundTag listingsTag = rootTag.getCompound("Listings");
            int count = listingsTag.getInt("Count");
            activeListings.clear();
            for (int i = 0; i < count; i++) {
                MineBayListing listing = MineBayListing.fromNBT(listingsTag.getCompound("Listing" + i));
                activeListings.put(listing.getListingId(), listing);
            }
            
            // Load held items
            CompoundTag heldItemsTag = rootTag.getCompound("HeldItems");
            int heldCount = heldItemsTag.getInt("Count");
            heldItems.clear();
            for (int i = 0; i < heldCount; i++) {
                CompoundTag entryTag = heldItemsTag.getCompound("Held" + i);
                UUID playerId = entryTag.getUUID("PlayerId");
                ItemStack item = ItemStack.parseOptional(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess(), entryTag.getCompound("Item"));
                heldItems.put(playerId, item);
            }
            
            ServerManagementMod.LOGGER.info("Loaded {} MineBay listings and {} held items", activeListings.size(), heldItems.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load MineBay data", e);
        }
    }
    
    /**
     * Draft listing that persists across GUI reopens
     */
    public static class MineBayListingDraft {
        private final UUID playerId;
        private ItemStack itemForSale;
        private double moneyPrice;
        private List<ItemStack> itemPrices;
        private MineBayListing.OfferType offerType;
        private boolean itemPlaced;
        
        public MineBayListingDraft(UUID playerId) {
            this.playerId = playerId;
            this.itemPrices = new ArrayList<>();
            this.itemForSale = ItemStack.EMPTY;
            this.offerType = MineBayListing.OfferType.FIXED;
        }
        
        public UUID getPlayerId() { return playerId; }
        public ItemStack getItemForSale() { return itemForSale; }
        public void setItemForSale(ItemStack item) { this.itemForSale = item; }
        public double getMoneyPrice() { return moneyPrice; }
        public void setMoneyPrice(double price) { this.moneyPrice = price; }
        public List<ItemStack> getItemPrices() { return itemPrices; }
        public void setItemPrices(List<ItemStack> items) { this.itemPrices = new ArrayList<>(items); }
        public MineBayListing.OfferType getOfferType() { return offerType; }
        public void setOfferType(MineBayListing.OfferType type) { this.offerType = type; }
        public boolean isItemPlaced() { return itemPlaced; }
        public void setItemPlaced(boolean placed) { this.itemPlaced = placed; }
    }
}
