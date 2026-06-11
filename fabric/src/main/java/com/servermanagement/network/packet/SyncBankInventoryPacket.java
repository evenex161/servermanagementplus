package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync bank inventory contents
 */
public record SyncBankInventoryPacket(CompoundTag inventoryData) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncBankInventoryPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "sync_bank_inventory_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankInventoryPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankInventoryPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncBankInventoryPacket(BankInventory inventory) {
        this(inventory.toNBT());
    }
    public SyncBankInventoryPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(inventoryData);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Store in client-side data holder
            BankInventory inventory = BankInventory.fromNBT(inventoryData);
            com.servermanagement.client.ClientBankInventoryData.setBankInventory(inventory);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}
}
