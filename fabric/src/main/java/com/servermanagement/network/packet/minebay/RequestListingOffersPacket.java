package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to request offers for a specific listing.
 * Only the listing owner can request this.
 */
public class RequestListingOffersPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RequestListingOffersPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "request_listing_offers_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestListingOffersPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestListingOffersPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String listingId;

    public RequestListingOffersPacket(String listingId) {
        this.listingId = listingId;
    }

    public RequestListingOffersPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) return;

            MineBayManager manager = MineBayManager.getInstance();
            MineBayListing listing = manager.getListing(listingId);

            if (listing == null || !listing.getSellerId().equals(player.getUUID())) {
                return; // Only owner can view offers
            }

            // Send offers back to the requesting player (snapshot to avoid CME from scheduled tasks)
            List<MineBayOffer> pendingOffers = new ArrayList<>();
            for (MineBayOffer offer : new ArrayList<>(listing.getCounteroffers())) {
                if (offer.getStatus() == MineBayOffer.OfferStatus.PENDING) {
                    pendingOffers.add(offer);
                }
            }

            com.servermanagement.network.ModNetworking.sendToPlayer(
                new SyncListingOffersPacket(listingId, pendingOffers), player);

}
}
