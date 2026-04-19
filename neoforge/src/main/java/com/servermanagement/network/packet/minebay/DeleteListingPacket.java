package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to delete an existing listing
 */
public class DeleteListingPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeleteListingPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "delete_listing"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, DeleteListingPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), DeleteListingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final String listingId;
    
    public DeleteListingPacket(String listingId) {
        this.listingId = listingId;
    }
    
    public DeleteListingPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.listingId, 36);
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null) return;
            
            MineBayManager manager = MineBayManager.getInstance();
            MineBayListing listing = manager.getListing(listingId);
            
            if (listing == null) {
                player.sendSystemMessage(Component.literal("§cListing not found!"));
                return;
            }
            
            // Verify ownership
            if (!listing.getSellerId().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou can only delete your own listings!"));
                return;
            }
            
            // Return the item to the player
            ItemStack itemForSale = listing.getItemForSale();
            if (!itemForSale.isEmpty()) {
                boolean added = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, itemForSale);
                if (!added) {
                    player.drop(itemForSale, false);
                }
            }
            
            // Remove the listing
            manager.removeListing(listingId);
            
            // Sync to all players
            manager.syncListingsToAllPlayers(player.server);
            
            player.sendSystemMessage(Component.literal("§aListing deleted successfully!"));
        });
        // packet handled
    }
}
