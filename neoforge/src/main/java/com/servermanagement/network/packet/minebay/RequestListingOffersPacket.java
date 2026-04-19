package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to request offers for a specific listing.
 * Only the listing owner can request this.
 */
public record RequestListingOffersPacket(String listingId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestListingOffersPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "request_listing_offers"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestListingOffersPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestListingOffersPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public RequestListingOffersPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        });
        // packet handled
    }
}
