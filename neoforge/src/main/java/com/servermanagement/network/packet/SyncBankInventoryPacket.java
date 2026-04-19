package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from server to client to sync bank inventory contents
 */
public record SyncBankInventoryPacket(CompoundTag inventoryData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncBankInventoryPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_bank_inventory"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankInventoryPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankInventoryPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public SyncBankInventoryPacket(BankInventory inventory) {
        this(inventory.toNBT());
    }
    
    public SyncBankInventoryPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(inventoryData);
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store in client-side data holder
            BankInventory inventory = BankInventory.fromNBT(inventoryData);
            com.servermanagement.client.ClientBankInventoryData.setBankInventory(inventory);
        });
        // packet handled
    }
}
