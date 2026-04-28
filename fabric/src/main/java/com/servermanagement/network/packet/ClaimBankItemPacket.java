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
public record ClaimBankItemPacket(int itemIndex) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "claim_bank_item_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ClaimBankItemPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
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
                            "┬ºcÔ£ù Inventory is full! Cannot claim item."
                        ));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºaÔ£ô Claimed: " + item.getHoverName().getString()
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
