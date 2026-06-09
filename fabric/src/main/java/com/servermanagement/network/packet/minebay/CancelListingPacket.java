package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to cancel listing creation and return held item
 */
public record CancelListingPacket() implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "cancel_listing_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


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
