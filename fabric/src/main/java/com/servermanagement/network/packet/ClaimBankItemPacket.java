package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim an item from bank inventory
 */
public class ClaimBankItemPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ClaimBankItemPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "claim_bank_item_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimBankItemPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimBankItemPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final int itemIndex;
    
    public ClaimBankItemPacket(int itemIndex) {
        this.itemIndex = itemIndex;
    }
    
    public ClaimBankItemPacket(FriendlyByteBuf buf) {
        this.itemIndex = buf.readInt();
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(itemIndex);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) return;
            
            EconomyManager economyManager = EconomyManager.getInstance(player.server);
            if (economyManager == null) return;
            
            BankInventory bankInventory = economyManager.getBankInventory(player.getUUID());
            
            if (itemIndex >= 0 && itemIndex < bankInventory.getItemCount()) {
                ItemStack item = bankInventory.removeItem(itemIndex);
                
                if (!item.isEmpty()) {
                    // Try to add to player inventory
                    boolean added = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, item);
                    
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

}
}
