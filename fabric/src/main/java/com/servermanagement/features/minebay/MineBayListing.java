package com.servermanagement.features.minebay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Represents a listing on MineBay - an item being sold with a price
 */
public class MineBayListing {
    private String listingId;
    private UUID sellerId;
    private String sellerName;
    private ItemStack itemForSale;
    private double moneyPrice;
    private double baseMarketPrice; // Dynamic market price from MarketPricingEngine
    private double marginPercent;   // Seller's margin percentage on top of market price
    private List<PriceItemEntry> priceItems; // Up to 3 item requirements (max 3)
    private OfferType offerType;
    private ListingStatus status;
    private long createdTimestamp;
    private List<MineBayOffer> counteroffers;
    private int pendingOfferCount; // Cached count for client sync (not persisted)
    
    public static final int MAX_PRICE_ITEMS = 3;
    
    public enum OfferType {
        FIXED,          // First come first serve, no counteroffers
        NEGOTIABLE      // Accepts counteroffers
    }
    
    public enum ListingStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
    
    public MineBayListing() {
        this.listingId = UUID.randomUUID().toString();
        this.priceItems = new ArrayList<>();
        this.counteroffers = new ArrayList<>();
        this.status = ListingStatus.ACTIVE;
        this.createdTimestamp = System.currentTimeMillis();
    }
    
    public MineBayListing(UUID sellerId, String sellerName, ItemStack itemForSale, 
                          double moneyPrice, List<PriceItemEntry> priceItems, OfferType offerType) {
        this();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemForSale = itemForSale.copy();
        this.moneyPrice = moneyPrice;
        this.priceItems = new ArrayList<>(priceItems);
        this.offerType = offerType;
    }

    public MineBayListing(UUID sellerId, String sellerName, ItemStack itemForSale,
                          double moneyPrice, double baseMarketPrice, double marginPercent,
                          List<PriceItemEntry> priceItems, OfferType offerType) {
        this(sellerId, sellerName, itemForSale, moneyPrice, priceItems, offerType);
        this.baseMarketPrice = baseMarketPrice;
        this.marginPercent = marginPercent;
    }
    
    // Serialize to NBT for saving
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", com.servermanagement.util.DataVersion.CURRENT_VERSION);
        tag.putString("ListingId", listingId);
        com.servermanagement.util.NbtHelper.putUUID(tag, "SellerId", sellerId);
        tag.putString("SellerName", sellerName);
        tag.put("ItemForSale", com.servermanagement.util.NbtHelper.saveItemStack(itemForSale, com.servermanagement.ServerManagementModFabric.getServer().registryAccess()));
        tag.putDouble("MoneyPrice", moneyPrice);
        tag.putDouble("BaseMarketPrice", baseMarketPrice);
        tag.putDouble("MarginPercent", marginPercent);
        tag.putString("OfferType", offerType.name());
        tag.putString("Status", status.name());
        tag.putLong("Created", createdTimestamp);
        
        // Save price items
        CompoundTag priceItemsTag = new CompoundTag();
        for (int i = 0; i < priceItems.size() && i < MAX_PRICE_ITEMS; i++) {
            priceItemsTag.put("Item" + i, priceItems.get(i).toNBT());
        }
        priceItemsTag.putInt("Count", Math.min(priceItems.size(), MAX_PRICE_ITEMS));
        tag.put("PriceItems", priceItemsTag);
        
        // Save counteroffers
        CompoundTag counteroffersTag = new CompoundTag();
        for (int i = 0; i < counteroffers.size(); i++) {
            counteroffersTag.put("Offer" + i, counteroffers.get(i).toNBT());
        }
        counteroffersTag.putInt("Count", counteroffers.size());
        tag.put("Counteroffers", counteroffersTag);
        
        return tag;
    }
    
    // Deserialize from NBT with version checking
    public static MineBayListing fromNBT(CompoundTag tag) {
        // Check data version for future migrations
        int dataVersion = tag.getIntOr("DataVersion", 0);
        if (dataVersion > com.servermanagement.util.DataVersion.CURRENT_VERSION) {
            com.servermanagement.ServerManagementMod.LOGGER.warn(
                "MineBay listing data version {} is newer than supported version {}",
                dataVersion, com.servermanagement.util.DataVersion.CURRENT_VERSION);
        }
        
        MineBayListing listing = new MineBayListing();
        listing.listingId = tag.getStringOr("ListingId", "");
        listing.sellerId = com.servermanagement.util.NbtHelper.getUUID(tag, "SellerId");
        listing.sellerName = tag.getStringOr("SellerName", "");
        listing.itemForSale = com.servermanagement.util.NbtHelper.loadItemStack(tag.getCompoundOrEmpty("ItemForSale"), com.servermanagement.ServerManagementModFabric.getServer().registryAccess());
        listing.moneyPrice = tag.getDoubleOr("MoneyPrice", 0.0);
        listing.baseMarketPrice = tag.getDoubleOr("BaseMarketPrice", 0.0);
        listing.marginPercent = tag.getDoubleOr("MarginPercent", 0.0);
        listing.offerType = OfferType.valueOf(tag.getStringOr("OfferType", OfferType.FIXED.name()));
        listing.status = ListingStatus.valueOf(tag.getStringOr("Status", ListingStatus.ACTIVE.name()));
        listing.createdTimestamp = tag.getLongOr("Created", 0L);
        
        // Load price items
        CompoundTag priceItemsTag = tag.getCompoundOrEmpty("PriceItems");
        int itemCount = priceItemsTag.getIntOr("Count", 0);
        listing.priceItems = new ArrayList<>();
        for (int i = 0; i < itemCount && i < MAX_PRICE_ITEMS; i++) {
            listing.priceItems.add(PriceItemEntry.fromNBT(priceItemsTag.getCompoundOrEmpty("Item" + i)));
        }
        
        // Load counteroffers
        CompoundTag counteroffersTag = tag.getCompoundOrEmpty("Counteroffers");
        int offerCount = counteroffersTag.getIntOr("Count", 0);
        listing.counteroffers = new ArrayList<>();
        for (int i = 0; i < offerCount; i++) {
            listing.counteroffers.add(MineBayOffer.fromNBT(counteroffersTag.getCompoundOrEmpty("Offer" + i)));
        }
        
        return listing;
    }
    
    public void addCounteroffer(MineBayOffer offer) {
        this.counteroffers.add(offer);
    }
    
    // Getters
    public String getListingId() {
        return listingId;
    }
    
    public UUID getSellerId() {
        return sellerId;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public ItemStack getItemForSale() {
        return itemForSale.copy();
    }
    
    public double getMoneyPrice() {
        return moneyPrice;
    }
    
    public List<PriceItemEntry> getPriceItems() {
        return new ArrayList<>(priceItems);
    }
    
    public void addPriceItem(PriceItemEntry entry) {
        if (priceItems.size() < MAX_PRICE_ITEMS && !entry.isEmpty()) {
            priceItems.add(entry);
        }
    }
    
    public void removePriceItem(int index) {
        if (index >= 0 && index < priceItems.size()) {
            priceItems.remove(index);
        }
    }
    
    public void setPriceItem(int index, PriceItemEntry entry) {
        if (index >= 0 && index < MAX_PRICE_ITEMS) {
            while (priceItems.size() <= index) {
                priceItems.add(new PriceItemEntry());
            }
            priceItems.set(index, entry);
        }
    }
    
    public PriceItemEntry getPriceItem(int index) {
        if (index >= 0 && index < priceItems.size()) {
            return priceItems.get(index);
        }
        return new PriceItemEntry();
    }
    
    public OfferType getOfferType() {
        return offerType;
    }
    
    public ListingStatus getStatus() {
        return status;
    }
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public List<MineBayOffer> getCounteroffers() {
        return new ArrayList<>(counteroffers);
    }
    
    public int getPendingOfferCount() {
        // If cached count is set (from sync), use it; otherwise compute from counteroffers
        if (pendingOfferCount > 0 || counteroffers.isEmpty()) {
            return pendingOfferCount;
        }
        int count = 0;
        for (MineBayOffer offer : counteroffers) {
            if (offer.getStatus() == MineBayOffer.OfferStatus.PENDING) count++;
        }
        return count;
    }
    
    public void setPendingOfferCount(int count) {
        this.pendingOfferCount = count;
    }
    
    // Setters
    public void setStatus(ListingStatus status) {
        this.status = status;
    }
    
    public void setListingId(String listingId) {
        this.listingId = listingId;
    }
    
    public void setCreatedTime(long timestamp) {
        this.createdTimestamp = timestamp;
    }
    
    public double getBaseMarketPrice() {
        return baseMarketPrice;
    }

    public void setBaseMarketPrice(double baseMarketPrice) {
        this.baseMarketPrice = baseMarketPrice;
    }

    public double getMarginPercent() {
        return marginPercent;
    }

    public void setMarginPercent(double marginPercent) {
        this.marginPercent = marginPercent;
    }

    public void setMoneyPrice(double moneyPrice) {
        this.moneyPrice = moneyPrice;
    }

    // Getters for renamed methods
    public ItemStack getItemOffered() {
        return getItemForSale();
    }
    
    public long getCreatedTime() {
        return getCreatedTimestamp();
    }
}
