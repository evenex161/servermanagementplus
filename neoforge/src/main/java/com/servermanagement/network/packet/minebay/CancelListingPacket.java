package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to cancel listing creation and return held item
 */
public record CancelListingPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CancelListingPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "cancel_listing"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CancelListingPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), CancelListingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public CancelListingPacket(FriendlyByteBuf buf) {
        this();
    }
    
        public void encode(FriendlyByteBuf buf) {
        // No data needed
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        });
        // packet handled
    }
}
