package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync active MineBay listings
 */
public class SyncMineBayListingsPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncMineBayListingsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_mine_bay_listings_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMineBayListingsPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMineBayListingsPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
            ItemStack itemOffered = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            double moneyPrice = buf.readDouble();
            long createdTime = buf.readLong();
            MineBayListing.OfferType offerType = buf.readEnum(MineBayListing.OfferType.class);
            
            // Read price items
            int priceItemCount = buf.readInt();
            List<PriceItemEntry> priceItems = new ArrayList<>();
            for (int j = 0; j < priceItemCount; j++) {
                ItemStack priceItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
                int amount = buf.readInt();
                boolean useStacks = buf.readBoolean();
                priceItems.add(new PriceItemEntry(priceItem, amount, useStacks));
            }
            
            // Create listing
            MineBayListing listing = new MineBayListing(sellerId, sellerName, itemOffered, moneyPrice, priceItems, offerType);
            listing.setListingId(listingId);
            listing.setCreatedTime(createdTime);
            
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
        }
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Update client-side cache
            com.servermanagement.client.ClientMineBayData.updateListings(listings);
            
            // Update client-side screen if MineBay is open
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.updateListings(listings);
            }
        });
        
    }
}
