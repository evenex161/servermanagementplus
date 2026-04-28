package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync active MineBay listings
 */
public class SyncMineBayListingsPacket implements IPacket {
    private final List<MineBayListing> listings;
    
    public SyncMineBayListingsPacket(List<MineBayListing> listings) {
        this.listings = new ArrayList<>(listings);
    }
    
    public SyncMineBayListingsPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.listings = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Read listing data
            String listingId = buf.readUtf(36);
            UUID sellerId = buf.readUUID();
            String sellerName = buf.readUtf(16);
            ItemStack itemOffered = buf.readItem();
            double moneyPrice = buf.readDouble();
            long createdTime = buf.readLong();
            MineBayListing.OfferType offerType = buf.readEnum(MineBayListing.OfferType.class);
            
            // Read price items
            int priceItemCount = buf.readInt();
            List<PriceItemEntry> priceItems = new ArrayList<>();
            for (int j = 0; j < priceItemCount; j++) {
                ItemStack priceItem = buf.readItem();
                int amount = buf.readInt();
                boolean useStacks = buf.readBoolean();
                priceItems.add(new PriceItemEntry(priceItem, amount, useStacks));
            }
            
            // Read market pricing data
            double baseMarketPrice = buf.readDouble();
            double marginPercent = buf.readDouble();
            int pendingOfferCount = buf.readInt();
            
            // Create listing
            MineBayListing listing = new MineBayListing(sellerId, sellerName, itemOffered, moneyPrice, baseMarketPrice, marginPercent, priceItems, offerType);
            listing.setListingId(listingId);
            listing.setCreatedTime(createdTime);
            listing.setPendingOfferCount(pendingOfferCount);
            
            this.listings.add(listing);
        }
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(listings.size());
        
        for (MineBayListing listing : listings) {
            buf.writeUtf(listing.getListingId(), 36);
            buf.writeUUID(listing.getSellerId());
            buf.writeUtf(listing.getSellerName(), 16);
            buf.writeItem(listing.getItemOffered());
            buf.writeDouble(listing.getMoneyPrice());
            buf.writeLong(listing.getCreatedTime());
            buf.writeEnum(listing.getOfferType());
            
            // Write price items
            buf.writeInt(listing.getPriceItems().size());
            for (PriceItemEntry priceItem : listing.getPriceItems()) {
                buf.writeItem(priceItem.getItemStack());
                buf.writeInt(priceItem.getAmount());
                buf.writeBoolean(priceItem.isUseStacks());
            }
            
            // Write market pricing data
            buf.writeDouble(listing.getBaseMarketPrice());
            buf.writeDouble(listing.getMarginPercent());
            buf.writeInt(listing.getPendingOfferCount());
        }
    }
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update client-side cache
            com.servermanagement.client.ClientMineBayData.updateListings(listings);
            
            // Update client-side screen if MineBay is open
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.updateListings(listings);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
