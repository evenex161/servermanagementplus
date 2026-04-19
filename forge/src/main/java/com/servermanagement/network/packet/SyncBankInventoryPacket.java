package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync bank inventory contents
 */
public class SyncBankInventoryPacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Store in client-side data holder
            BankInventory inventory = BankInventory.fromNBT(inventoryData);
            com.servermanagement.client.ClientBankInventoryData.setBankInventory(inventory);
        });
        ctx.setPacketHandled(true);
    }
}
