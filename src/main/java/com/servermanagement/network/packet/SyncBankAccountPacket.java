package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.Transaction;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to sync bank account data from server to client
 */
public class SyncBankAccountPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncBankAccountPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_bank_account_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankAccountPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankAccountPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final double balance;
    private final List<Transaction> recentTransactions;

    public SyncBankAccountPacket(double balance, List<Transaction> recentTransactions) {
        this.balance = balance;
        this.recentTransactions = recentTransactions;
    }

    public SyncBankAccountPacket(FriendlyByteBuf buf) {
        this.balance = buf.readDouble();
        
        int transactionCount = buf.readInt();
        this.recentTransactions = new ArrayList<>();
        for (int i = 0; i < transactionCount; i++) {
            // Read transaction data - for now just skip as we'll display in GUI
            // In a full implementation, we'd serialize Transaction objects
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(balance);
        buf.writeInt(recentTransactions.size());
        // For now, we don't serialize full transactions
        // They'll be loaded from server-side data when GUI opens
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Store balance on client side for GUI display
            com.servermanagement.client.ClientBankData.setBalance(balance);
            com.servermanagement.client.ClientBankData.setTransactions(recentTransactions);
        });
        
    }
}
