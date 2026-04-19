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
public record SyncBankAccountPacket(double balance, List<Transaction> recentTransactions) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncBankAccountPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_bank_account_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankAccountPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankAccountPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncBankAccountPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), decodeTransactions(buf));
    }

    private static List<Transaction> decodeTransactions(FriendlyByteBuf buf) {
        int transactionCount = buf.readInt();
        List<Transaction> list = new ArrayList<>();
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
                type = TransactionType.ADMIN_GIVE;
            }
            list.add(new Transaction(type, amount, timestamp, description, otherParty));
        }
        return list;
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
