package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to delete an existing listing
 */
public class DeleteListingPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<DeleteListingPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "delete_listing_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, DeleteListingPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), DeleteListingPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
