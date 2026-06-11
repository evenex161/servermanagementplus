package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankInventory;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to claim an item from bank inventory
 */
public record ClaimBankItemPacket(int itemIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimBankItemPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "claim_bank_item"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimBankItemPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimBankItemPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public ClaimBankItemPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(itemIndex);
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null) return;
            
            EconomyManager economyManager = EconomyManager.getInstance(player.level().getServer());
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
        });
        // packet handled
    }
}
