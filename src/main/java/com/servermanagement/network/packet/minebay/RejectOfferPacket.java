package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when a seller rejects an offer
 */
public class RejectOfferPacket implements IPacket {
    public static final CustomPacketPayload.Type<RejectOfferPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "reject_offer_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RejectOfferPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), RejectOfferPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String listingId;
    private final String offerId;
    
    public RejectOfferPacket(String listingId, String offerId) {
        this.listingId = listingId;
        this.offerId = offerId;
    }
    
    public RejectOfferPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
        this.offerId = buf.readUtf(36);
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeUtf(offerId, 36);
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer seller = (ServerPlayer) ctx.player();
            if (seller == null) return;
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            // Validation
            if (listing == null) {
                seller.sendSystemMessage(Component.literal("§cListing not found!"));
                return;
            }
            
            if (!listing.getSellerId().equals(seller.getUUID())) {
                seller.sendSystemMessage(Component.literal("§cYou can only reject offers on your own listings!"));
                return;
            }
            
            // Find the offer
            MineBayOffer rejectedOffer = null;
            for (MineBayOffer offer : listing.getCounteroffers()) {
                if (offer.getOfferId().equals(offerId) && offer.getStatus() == MineBayOffer.OfferStatus.PENDING) {
                    rejectedOffer = offer;
                    break;
                }
            }
            
            if (rejectedOffer == null) {
                seller.sendSystemMessage(Component.literal("§cOffer not found or already processed!"));
                return;
            }
            
            // Mark offer as rejected
            rejectedOffer.setStatus(MineBayOffer.OfferStatus.REJECTED);
            mineBayManager.saveListing(listing);
            
            // Notify seller
            seller.sendSystemMessage(Component.literal("§cOffer rejected."));
            
            // Notify buyer if online
            ServerPlayer buyer = seller.server.getPlayerList().getPlayer(rejectedOffer.getBuyerId());
            if (buyer != null) {
                buyer.sendSystemMessage(Component.literal("§c[MineBay] Your offer was rejected"));
                buyer.sendSystemMessage(Component.literal("§7" + seller.getName().getString() + 
                    " rejected your offer on " + listing.getItemForSale().getDisplayName().getString()));
            }
        });
        
    }
}
