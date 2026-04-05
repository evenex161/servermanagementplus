package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim an item from bank inventory
 */
public class ClaimBankItemPacket implements IPacket {
    public static final CustomPacketPayload.Type<ClaimBankItemPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "claim_bank_item_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimBankItemPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimBankItemPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
        
    }
}
