package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to create a new MineBay listing
 */
public class CreateListingPacket implements IPacket {
    public static final CustomPacketPayload.Type<CreateListingPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "create_listing_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CreateListingPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), CreateListingPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
        this.itemToSell = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
        this.moneyPrice = buf.readDouble();
        this.offerType = buf.readEnum(MineBayListing.OfferType.class);
        
        // Read price items
        int priceItemCount = buf.readInt();
        this.priceItems = new ArrayList<>();
        for (int i = 0; i < priceItemCount; i++) {
            ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            int amount = buf.readInt();
            boolean useStacks = buf.readBoolean();
            if (!itemStack.isEmpty()) {
                this.priceItems.add(new PriceItemEntry(itemStack, amount, useStacks));
            }
        }
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, itemToSell);
        buf.writeDouble(moneyPrice);
        buf.writeEnum(offerType);
        
        // Write price items
        buf.writeInt(priceItems.size());
        for (PriceItemEntry priceItem : priceItems) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, priceItem.getItemStack());
            buf.writeInt(priceItem.getAmount());
            buf.writeBoolean(priceItem.isUseStacks());
        }
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
        
    }
}
