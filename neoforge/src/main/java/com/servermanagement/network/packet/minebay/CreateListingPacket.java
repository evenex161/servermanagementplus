package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to create a new MineBay listing
 */
public record CreateListingPacket(ItemStack itemToSell, double moneyPrice, double marginPercent, MineBayListing.OfferType offerType, List<PriceItemEntry> priceItems) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CreateListingPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "create_listing"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CreateListingPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), CreateListingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
// Seller's desired margin %
    
    public CreateListingPacket {
        itemToSell = itemToSell.copy();
        priceItems = new ArrayList<>(priceItems);
    }
    
    public CreateListingPacket(FriendlyByteBuf buf) {
        this(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf), buf.readDouble(), buf.readDouble(), buf.readEnum(MineBayListing.OfferType.class), decodePriceItems(buf));
    }
    
        public void encode(FriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, itemToSell);
        buf.writeDouble(moneyPrice);
        buf.writeDouble(marginPercent);
        buf.writeEnum(offerType);
        
        // Write price items
        buf.writeInt(priceItems.size());
        for (PriceItemEntry priceItem : priceItems) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, priceItem.getItemStack());
            buf.writeInt(priceItem.getAmount());
            buf.writeBoolean(priceItem.isUseStacks());
        }
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null) {
                MineBayManager manager = MineBayManager.getInstance();
                
                // SECURITY: Read item from the server-side menu container, NOT from packet data.
                // The client-sent itemToSell could be spoofed with arbitrary items.
                ItemStack serverItem = ItemStack.EMPTY;
                if (player.containerMenu instanceof com.servermanagement.gui.minebay.MineBayMenu menu) {
                    serverItem = menu.getOfferingItem();
                    // Immediately prevent the item from being returned on menu close.
                    // This MUST happen before any listing creation to prevent the race condition
                    // where removed() fires between listing creation and clearOfferingItem(),
                    // which would duplicate the item (returned to player AND listed on MineBay).
                    menu.clearOfferingItem();
                }
                
                if (serverItem.isEmpty()) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c✗ Error: No item was placed for listing"
                        )
                    );
                    return;
                }
                
                // Calculate dynamic market pricing on the server
                com.servermanagement.features.economy.MarketPricingEngine pricingEngine = 
                    com.servermanagement.features.economy.MarketPricingEngine.getInstance();
                pricingEngine.ensureFresh(player.level().getServer());
                
                double baseMarketPrice = pricingEngine.getStackPrice(serverItem);
                double clampedMargin = Math.max(-50.0, Math.min(200.0, marginPercent));
                double finalPrice = pricingEngine.calculateFinalPrice(baseMarketPrice, clampedMargin);
                
                // Create the listing with the server-validated item
                MineBayListing listing = manager.createListing(
                    player.getUUID(),
                    player.getName().getString(),
                    serverItem,
                    finalPrice,
                    priceItems,
                    offerType
                );
                
                if (listing != null) {
                    // Set market pricing fields
                    listing.setBaseMarketPrice(baseMarketPrice);
                    listing.setMarginPercent(clampedMargin);
                    listing.setMoneyPrice(finalPrice);
                    
                    // Log margin to history for future pricing calculations
                    com.servermanagement.features.economy.MarginHistoryTracker.getInstance()
                        .recordMargin(serverItem, clampedMargin, finalPrice, baseMarketPrice, player.getUUID());
                }
                
                if (listing == null) {
                    // Max listings reached - return item to player
                    int maxListings = com.servermanagement.config.ModConfig.MAX_LISTINGS_PER_PLAYER.get();
                    player.getInventory().placeItemBackInInventory(serverItem);
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c✗ You have reached the maximum number of active listings (" + maxListings + ")"
                        )
                    );
                    return;
                }
                
                com.servermanagement.ServerManagementMod.LOGGER.info(
                    "Created MineBay listing {} by {} for ${} with {} price items", 
                    listing.getListingId(),
                    player.getName().getString(),
                    finalPrice,
                    priceItems.size()
                );
                
                // Send success message
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§a§l✓ §r§6[MineBay] §aListing created for §f" + listing.getItemForSale().getHoverName().getString()
                    ), true
                );
                
                // Sync listings to all online players immediately
                manager.syncListingsToAllPlayers(player.level().getServer());
            }
        });
        // packet handled
    }

    private static List<PriceItemEntry> decodePriceItems(FriendlyByteBuf buf) {
        int priceItemCount = Math.min(buf.readInt(), 54);
        List<PriceItemEntry> priceItems = new ArrayList<>();
        for (int i = 0; i < priceItemCount; i++) {
            ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            int amount = buf.readInt();
            boolean useStacks = buf.readBoolean();
            if (!itemStack.isEmpty()) {
                priceItems.add(new PriceItemEntry(itemStack, amount, useStacks));
            }
        }
        return priceItems;
    }
}
