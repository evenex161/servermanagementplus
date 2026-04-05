package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to delete an existing listing
 */
public class DeleteListingPacket implements IPacket {
    public static final CustomPacketPayload.Type<DeleteListingPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "delete_listing_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, DeleteListingPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), DeleteListingPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String listingId;
    
    public DeleteListingPacket(String listingId) {
        this.listingId = listingId;
    }
    
    public DeleteListingPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.listingId, 36);
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
                boolean added = player.getInventory().add(itemForSale);
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
        
    }
}
