package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to create a new MineBay listing
 */
public class CreateListingPacket implements IPacket {
    private final ItemStack itemToSell;
    private final double moneyPrice;
    private final double marginPercent; // Seller's desired margin %
    private final MineBayListing.OfferType offerType;
    private final List<PriceItemEntry> priceItems;
    
    public CreateListingPacket(ItemStack itemToSell, double moneyPrice, double marginPercent, MineBayListing.OfferType offerType, List<PriceItemEntry> priceItems) {
        this.itemToSell = itemToSell.copy();
        this.moneyPrice = moneyPrice;
        this.marginPercent = marginPercent;
        this.offerType = offerType;
        this.priceItems = new ArrayList<>(priceItems);
    }
    
    public CreateListingPacket(FriendlyByteBuf buf) {
        this.itemToSell = buf.readItem();
        this.moneyPrice = buf.readDouble();
        this.marginPercent = buf.readDouble();
        this.offerType = buf.readEnum(MineBayListing.OfferType.class);
        
        // Read price items
        int priceItemCount = buf.readInt();
        this.priceItems = new ArrayList<>();
        for (int i = 0; i < priceItemCount; i++) {
            ItemStack itemStack = buf.readItem();
            int amount = buf.readInt();
            boolean useStacks = buf.readBoolean();
            if (!itemStack.isEmpty()) {
                this.priceItems.add(new PriceItemEntry(itemStack, amount, useStacks));
            }
        }
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(itemToSell);
        buf.writeDouble(moneyPrice);
        buf.writeDouble(marginPercent);
        buf.writeEnum(offerType);
        
        // Write price items
        buf.writeInt(priceItems.size());
        for (PriceItemEntry priceItem : priceItems) {
            buf.writeItem(priceItem.getItemStack());
            buf.writeInt(priceItem.getAmount());
            buf.writeBoolean(priceItem.isUseStacks());
        }
    }
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
                pricingEngine.ensureFresh(player.server);
                
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
                manager.syncListingsToAllPlayers(player.server);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
