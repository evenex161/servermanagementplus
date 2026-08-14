package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to request offers for a specific listing.
 * Only the listing owner can request this.
 */
public record RequestListingOffersPacket(String listingId) implements IPacket {

    public RequestListingOffersPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 32767);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
