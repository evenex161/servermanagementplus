package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to hold an item for listing creation
 * Removes item from player inventory and stores it server-side
 */
public class HoldItemPacket implements IPacket {
    private final int slotIndex;
    
    public HoldItemPacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }
    
    public HoldItemPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
        });
        ctx.get().setPacketHandled(true);
    }
}
