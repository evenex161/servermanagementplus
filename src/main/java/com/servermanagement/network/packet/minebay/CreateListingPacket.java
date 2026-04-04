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
    private final MineBayListing.OfferType offerType;
    private final List<PriceItemEntry> priceItems;
    
    public CreateListingPacket(ItemStack itemToSell, double moneyPrice, MineBayListing.OfferType offerType, List<PriceItemEntry> priceItems) {
        this.itemToSell = itemToSell.copy();
        this.moneyPrice = moneyPrice;
        this.offerType = offerType;
        this.priceItems = new ArrayList<>(priceItems);
    }
    
    public CreateListingPacket(FriendlyByteBuf buf) {
        this.itemToSell = buf.readItem();
        this.moneyPrice = buf.readDouble();
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
                
                if (!itemToSell.isEmpty()) {
                    // Create the listing with the item sent from client
                    MineBayListing listing = manager.createListing(
                        player.getUUID(),
                        player.getName().getString(),
                        itemToSell,
                        moneyPrice,
                        priceItems, // Use the price items sent from client
                        offerType
                    );
                    
                    if (listing == null) {
                        // Max listings reached - return item to player
                        int maxListings = com.servermanagement.config.ModConfig.MAX_LISTINGS_PER_PLAYER.get();
                        player.getInventory().add(itemToSell.copy());
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
                        moneyPrice,
                        priceItems.size()
                    );
                    
                    // Clear the item from player's menu so it doesn't get returned
                    if (player.containerMenu instanceof com.servermanagement.gui.minebay.MineBayMenu menu) {
                        menu.clearOfferingItem();
                    }
                    
                    // Send success message
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§a✓ Listing created successfully!"
                        )
                    );
                    
                    // Sync listings to all online players immediately
                    manager.syncListingsToAllPlayers(player.server);
                } else {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c✗ Error: No item was placed for listing"
                        )
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
