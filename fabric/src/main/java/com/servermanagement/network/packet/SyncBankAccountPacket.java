package com.servermanagement.network.packet;

import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet to sync bank account data from server to client
 */
public class SyncBankAccountPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncBankAccountPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_bank_account_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankAccountPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankAccountPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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
            String typeName = buf.readUtf(64);
            double amount = buf.readDouble();
            long timestamp = buf.readLong();
            String description = buf.readUtf(256);
            boolean hasOtherParty = buf.readBoolean();
            UUID otherParty = hasOtherParty ? buf.readUUID() : null;
            
            TransactionType type;
            try {
                type = TransactionType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                type = TransactionType.ADMIN_GIVE; // fallback
            }
            recentTransactions.add(new Transaction(type, amount, timestamp, description, otherParty));
        }
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(balance);
        buf.writeInt(recentTransactions.size());
        for (Transaction t : recentTransactions) {
            buf.writeUtf(t.getType().name(), 64);
            buf.writeDouble(t.getAmount());
            buf.writeLong(t.getTimestamp());
            buf.writeUtf(t.getDescription() != null ? t.getDescription() : "", 256);
            buf.writeBoolean(t.getOtherParty() != null);
            if (t.getOtherParty() != null) {
                buf.writeUUID(t.getOtherParty());
            }
        }
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            com.servermanagement.client.ClientBankData.setBalance(balance);
            com.servermanagement.client.ClientBankData.setTransactions(recentTransactions);

}
}
