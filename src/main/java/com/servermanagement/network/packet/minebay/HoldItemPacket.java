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
 * Packet sent from client to server to hold an item for listing creation
 * Removes item from player inventory and stores it server-side
 */
public class HoldItemPacket implements IPacket {
    public static final CustomPacketPayload.Type<HoldItemPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "hold_item_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, HoldItemPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), HoldItemPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
        
    }
}
