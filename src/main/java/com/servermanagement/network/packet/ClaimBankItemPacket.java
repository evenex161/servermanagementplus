package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim an item from bank inventory
 */
public class ClaimBankItemPacket implements IPacket {
    private final int itemIndex;
    
    public ClaimBankItemPacket(int itemIndex) {
        this.itemIndex = itemIndex;
    }
    
    public ClaimBankItemPacket(FriendlyByteBuf buf) {
        this.itemIndex = buf.readInt();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(itemIndex);
    }
    
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            
            EconomyManager economyManager = EconomyManager.getInstance(player.server);
            if (economyManager == null) return;
            
            BankInventory bankInventory = economyManager.getBankInventory(player.getUUID());
            
            if (itemIndex >= 0 && itemIndex < bankInventory.getItemCount()) {
                ItemStack item = bankInventory.removeItem(itemIndex);
                
                if (!item.isEmpty()) {
                    // Try to add to player inventory
                    boolean added = player.getInventory().add(item);
                    
                    if (!added) {
                        // If inventory still full, add back to bank inventory
                        bankInventory.addItem(item, 
                            com.servermanagement.features.economy.BankInventory.ItemSource.TRANSFER, 
                            "Failed to claim - inventory full");
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c✗ Inventory is full! Cannot claim item."
                        ));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a✓ Claimed: " + item.getHoverName().getString()
                        ));
                        economyManager.save();
                    }
                    
                    // Sync updated bank inventory
                    com.servermanagement.network.ModNetworking.sendToPlayer(
                        new SyncBankInventoryPacket(bankInventory), player
                    );
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
