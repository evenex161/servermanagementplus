package com.servermanagement.network.packet;

import com.servermanagement.features.economy.Transaction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to sync bank account data from server to client
 */
public class SyncBankAccountPacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Store balance on client side for GUI display
            com.servermanagement.client.ClientBankData.setBalance(balance);
            com.servermanagement.client.ClientBankData.setTransactions(recentTransactions);
        });
        ctx.setPacketHandled(true);
    }
}
