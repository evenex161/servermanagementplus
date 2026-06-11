package com.servermanagement.features.minebay;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.DataVersion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Represents a counteroffer made by a potential buyer
 */
public class MineBayOffer {
    private String offerId;
    private String listingId;
    private UUID buyerId;
    private String buyerName;
    private double moneyOffer;
    private List<ItemStack> itemOffers;
    private OfferStatus status;
    private long createdTimestamp;
    
    public enum OfferStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
    
    public MineBayOffer() {
        this.offerId = UUID.randomUUID().toString();
        this.itemOffers = new ArrayList<>();
        this.status = OfferStatus.PENDING;
        this.createdTimestamp = System.currentTimeMillis();
    }
    
    public MineBayOffer(String listingId, UUID buyerId, String buyerName, 
                        double moneyOffer, List<ItemStack> itemOffers) {
        this();
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.moneyOffer = moneyOffer;
        this.itemOffers = new ArrayList<>(itemOffers);
    }
    
    // Serialize to NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DataVersion.CURRENT_VERSION);
        tag.putString("OfferId", offerId);
        tag.putString("ListingId", listingId);
        com.servermanagement.util.NbtHelper.putUUID(tag, "BuyerId", buyerId);
        tag.putString("BuyerName", buyerName);
        tag.putDouble("MoneyOffer", moneyOffer);
        tag.putString("Status", status.name());
        tag.putLong("Created", createdTimestamp);
        
        // Save item offers
        CompoundTag itemOffersTag = new CompoundTag();
        for (int i = 0; i < itemOffers.size(); i++) {
            itemOffersTag.put("Item" + i, com.servermanagement.util.NbtHelper.saveItemStack(itemOffers.get(i), net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()));
        }
        itemOffersTag.putInt("Count", itemOffers.size());
        tag.put("ItemOffers", itemOffersTag);
        
        return tag;
    }
    
    // Deserialize from NBT
    public static MineBayOffer fromNBT(CompoundTag tag) {
        // Check data version
        int dataVersion = tag.getIntOr("DataVersion", 0);
        if (dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.warn("MineBayOffer data version {} is newer than supported version {}",
                dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        MineBayOffer offer = new MineBayOffer();
        offer.offerId = tag.getStringOr("OfferId", "");
        offer.listingId = tag.getStringOr("ListingId", "");
        offer.buyerId = com.servermanagement.util.NbtHelper.getUUID(tag, "BuyerId");
        offer.buyerName = tag.getStringOr("BuyerName", "");
        offer.moneyOffer = tag.getDoubleOr("MoneyOffer", 0.0);
        offer.status = OfferStatus.valueOf(tag.getStringOr("Status", OfferStatus.PENDING.name()));
        offer.createdTimestamp = tag.getLongOr("Created", 0L);
        
        // Load item offers
        CompoundTag itemOffersTag = tag.getCompoundOrEmpty("ItemOffers");
        int itemCount = itemOffersTag.getIntOr("Count", 0);
        offer.itemOffers = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            offer.itemOffers.add(com.servermanagement.util.NbtHelper.loadItemStack(itemOffersTag.getCompoundOrEmpty("Item" + i), net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()));
        }
        
        return offer;
    }
    
    // Getters
    public String getOfferId() {
        return offerId;
    }
    
    public String getListingId() {
        return listingId;
    }
    
    public UUID getBuyerId() {
        return buyerId;
    }
    
    public String getBuyerName() {
        return buyerName;
    }
    
    public double getMoneyOffer() {
        return moneyOffer;
    }
    
    public List<ItemStack> getItemOffers() {
        return new ArrayList<>(itemOffers);
    }
    
    public OfferStatus getStatus() {
        return status;
    }
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    // Setters
    public void setStatus(OfferStatus status) {
        this.status = status;
    }
    
    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }
    
    public void setCreatedTimestamp(long timestamp) {
        this.createdTimestamp = timestamp;
    }
}
