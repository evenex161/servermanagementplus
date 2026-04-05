package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync bank inventory contents
 */
public class SyncBankInventoryPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncBankInventoryPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_bank_inventory_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankInventoryPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankInventoryPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final CompoundTag inventoryData;
    
    public SyncBankInventoryPacket(BankInventory inventory) {
        this.inventoryData = inventory.toNBT();
    }
    
    public SyncBankInventoryPacket(FriendlyByteBuf buf) {
        this.inventoryData = buf.readNbt();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(inventoryData);
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Store in client-side data holder
            BankInventory inventory = BankInventory.fromNBT(inventoryData);
            com.servermanagement.client.ClientBankInventoryData.setBankInventory(inventory);
        });
        
    }
}
