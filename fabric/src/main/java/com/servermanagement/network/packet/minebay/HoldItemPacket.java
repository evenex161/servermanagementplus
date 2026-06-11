package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to hold an item for listing creation
 * Removes item from player inventory and stores it server-side
 */
public record HoldItemPacket(int slotIndex) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<HoldItemPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "hold_item_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, HoldItemPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), HoldItemPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public HoldItemPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                // Validate slot index
                if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) {
                    return;
                }
                
                // Get item from player inventory
                ItemStack item = player.getInventory().getItem(slotIndex);
                
                if (!item.isEmpty()) {
                    // SECURITY: Return any previously held item before holding a new one.
                    // Without this, rapidly holding different items would lose the first one
                    // because holdItem() overwrites the map entry.
                    MineBayManager manager = MineBayManager.getInstance();
                    ItemStack previouslyHeld = manager.releaseHeldItem(player.getUUID());
                    if (!previouslyHeld.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(previouslyHeld);
                    }
                    
                    // Hold the item in MineBayManager
                    manager.holdItem(player.getUUID(), item.copy());
                    
                    // Remove from player inventory
                    player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                    
                    com.servermanagement.ServerManagementMod.LOGGER.info(
                        "Player {} placed item for MineBay listing: {}", 
                        player.getName().getString(), 
                        item.getHoverName().getString()
                    );
                }
            }

}
}
