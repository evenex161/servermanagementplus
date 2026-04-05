package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to cancel listing creation and return held item
 */
public class CancelListingPacket implements IPacket {
    public static final CustomPacketPayload.Type<CancelListingPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "cancel_listing_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CancelListingPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), CancelListingPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    
    public CancelListingPacket() {}
    
    public CancelListingPacket(FriendlyByteBuf buf) {}
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        // No data needed
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                MineBayManager manager = MineBayManager.getInstance();
                
                // Release the held item back to player
                ItemStack heldItem = manager.releaseHeldItem(player.getUUID());
                
                if (!heldItem.isEmpty()) {
                    // Try to add to player inventory
                    boolean added = player.getInventory().add(heldItem);
                    
                    if (!added) {
                        // Drop at player position if inventory is full
                        player.drop(heldItem, false);
                    }
                    
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§eItem returned to inventory"
                        )
                    );
                }
                
                // Clear draft
                manager.clearDraft(player.getUUID());
            }
        });
        
    }
}
