package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to cancel listing creation and return held item
 */
public record CancelListingPacket() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<CancelListingPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "cancel_listing_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, CancelListingPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), CancelListingPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public CancelListingPacket(FriendlyByteBuf buf) {
        this();
    }
    
        public void encode(FriendlyByteBuf buf) {
        // No data needed
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                MineBayManager manager = MineBayManager.getInstance();
                
                // Release the held item back to player
                ItemStack heldItem = manager.releaseHeldItem(player.getUUID());
                
                if (!heldItem.isEmpty()) {
                    // Try to add to player inventory
                    boolean added = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, heldItem);
                    
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

}
}
