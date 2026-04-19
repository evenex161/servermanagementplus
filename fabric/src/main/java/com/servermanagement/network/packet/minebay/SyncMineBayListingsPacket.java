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
public record SyncMineBayListingsPacket(List<MineBayListing> listings) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncMineBayListingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_mine_bay_listings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMineBayListingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMineBayListingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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
            String listingId = buf.readUtf(36);
            UUID sellerId = buf.readUUID();
            String sellerName = buf.readUtf(16);
            ItemStack itemOffered = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            double moneyPrice = buf.readDouble();
            long createdTime = buf.readLong();
            MineBayListing.OfferType offerType = buf.readEnum(MineBayListing.OfferType.class);
            int priceItemCount = buf.readInt();
            List<PriceItemEntry> priceItems = new ArrayList<>();
            for (int j = 0; j < priceItemCount; j++) {
                ItemStack priceItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
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
            buf.writeUtf(listing.getListingId(), 36);
            buf.writeUUID(listing.getSellerId());
            buf.writeUtf(listing.getSellerName(), 16);
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, listing.getItemOffered());
            buf.writeDouble(listing.getMoneyPrice());
            buf.writeLong(listing.getCreatedTime());
            buf.writeEnum(listing.getOfferType());
            
            // Write price items
            buf.writeInt(listing.getPriceItems().size());
            for (PriceItemEntry priceItem : listing.getPriceItems()) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, priceItem.getItemStack());
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
            
            // Update client-side screen if MineBay is open
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.updateListings(listings);
            }

}
}
