package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayOffer;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from server to client with the list of pending offers for a specific listing.
 * Only sent to the listing owner.
 */
public class SyncListingOffersPacket implements IPacket {
    private final String listingId;
    private final List<MineBayOffer> offers;

    public SyncListingOffersPacket(String listingId, List<MineBayOffer> offers) {
        this.listingId = listingId;
        this.offers = new ArrayList<>(offers);
    }

    public SyncListingOffersPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0; // Cap to prevent memory exhaustion
        this.offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(36);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(16);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();

            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0; // Cap items per offer
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(buf.readItem());
            }

            MineBayOffer offer = new MineBayOffer(listingId, buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            this.offers.add(offer);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeInt(offers.size());
        for (MineBayOffer offer : offers) {
            buf.writeUtf(offer.getOfferId(), 36);
            buf.writeUUID(offer.getBuyerId());
            buf.writeUtf(offer.getBuyerName(), 16);
            buf.writeDouble(offer.getMoneyOffer());
            buf.writeLong(offer.getCreatedTimestamp());

            List<ItemStack> items = offer.getItemOffers();
            buf.writeInt(items.size());
            for (ItemStack stack : items) {
                buf.writeItem(stack);
            }
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.servermanagement.gui.minebay.MineBayScreen screen) {
                screen.receiveOffers(listingId, offers);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
