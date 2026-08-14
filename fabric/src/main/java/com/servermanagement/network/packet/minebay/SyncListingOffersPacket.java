package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from server to client with the list of pending offers for a specific listing.
 * Only sent to the listing owner.
 */
public record SyncListingOffersPacket(String listingId, List<MineBayOffer> offers) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncListingOffersPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_listing_offers_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncListingOffersPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncListingOffersPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncListingOffersPacket {
        offers = new ArrayList<>(offers);
    }
    public SyncListingOffersPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), decodeOffers(buf));
    }

    private static List<MineBayOffer> decodeOffers(FriendlyByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0;
        List<MineBayOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(32767);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(32767);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();
            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0;
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf));
            }
            MineBayOffer offer = new MineBayOffer("", buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            offers.add(offer);
        }
        return offers;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 32767);
        buf.writeInt(offers.size());
        for (MineBayOffer offer : offers) {
            buf.writeUtf(offer.getOfferId(), 32767);
            buf.writeUUID(offer.getBuyerId());
            buf.writeUtf(offer.getBuyerName(), 32767);
            buf.writeDouble(offer.getMoneyOffer());
            buf.writeLong(offer.getCreatedTimestamp());

            List<ItemStack> items = offer.getItemOffers();
            buf.writeInt(items.size());
            for (ItemStack stack : items) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf, stack);
            }
        }
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.receiveOffers(listingId, offers);
            }

}
}
