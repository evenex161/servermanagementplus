package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync bank inventory contents
 */
public record SyncBankInventoryPacket(CompoundTag inventoryData) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_bank_inventory_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


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
            com.servermanagement.client.ClientScreenManager.refreshOpenScreen();

}
}
