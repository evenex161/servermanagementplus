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
public record DeleteListingPacket(String listingId) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "delete_listing_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public DeleteListingPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.listingId, 32767);
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
