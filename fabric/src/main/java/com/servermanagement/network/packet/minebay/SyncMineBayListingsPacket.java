package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.PriceItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync active MineBay listings
 */
public record SyncMineBayListingsPacket(List<MineBayListing> listings) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_mine_bay_listings_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncMineBayListingsPacket {
        listings = new ArrayList<>(listings);
    }
    public SyncMineBayListingsPacket(FriendlyByteBuf buf) {
        this(decodeListings(buf));
    }

    private static List<MineBayListing> decodeListings(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<MineBayListing> listings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String listingId = buf.readUtf(32767);
            UUID sellerId = buf.readUUID();
            String sellerName = buf.readUtf(32767);
            ItemStack itemOffered = buf.readItem();
            double moneyPrice = buf.readDouble();
            long createdTime = buf.readLong();
            MineBayListing.OfferType offerType = buf.readEnum(MineBayListing.OfferType.class);
            int priceItemCount = buf.readInt();
            List<PriceItemEntry> priceItems = new ArrayList<>();
            for (int j = 0; j < priceItemCount; j++) {
                ItemStack priceItem = buf.readItem();
                int amount = buf.readInt();
                boolean useStacks = buf.readBoolean();
                priceItems.add(new PriceItemEntry(priceItem, amount, useStacks));
            }
            double baseMarketPrice = buf.readDouble();
            double marginPercent = buf.readDouble();
            int pendingOfferCount = buf.readInt();
            MineBayListing listing = new MineBayListing(sellerId, sellerName, itemOffered, moneyPrice, baseMarketPrice, marginPercent, priceItems, offerType);
            listing.setListingId(listingId);
            listing.setCreatedTime(createdTime);
            listing.setPendingOfferCount(pendingOfferCount);
            listings.add(listing);
        }
        return listings;
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(listings.size());
        
        for (MineBayListing listing : listings) {
            buf.writeUtf(listing.getListingId(), 32767);
            buf.writeUUID(listing.getSellerId());
            buf.writeUtf(listing.getSellerName(), 32767);
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
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Update client-side cache
            com.servermanagement.client.ClientMineBayData.updateListings(listings);
            com.servermanagement.client.ClientScreenManager.refreshOpenScreen();
            
            // Update client-side screen if MineBay is open
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.updateListings(listings);
            }

}
}
