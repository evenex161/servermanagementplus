package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync bank inventory contents
 */
public record SyncBankInventoryPacket(CompoundTag inventoryData) implements IPacket {
    
    public SyncBankInventoryPacket(BankInventory inventory) {
        this(inventory.toNBT());
    }
    
    public SyncBankInventoryPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(inventoryData);
    }
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            // Store in client-side data holder
            BankInventory inventory = BankInventory.fromNBT(inventoryData);
            com.servermanagement.client.ClientBankInventoryData.setBankInventory(inventory);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
