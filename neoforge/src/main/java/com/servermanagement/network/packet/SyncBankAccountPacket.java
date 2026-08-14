package com.servermanagement.network.packet;

import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet to sync bank account data from server to client
 */
public record SyncBankAccountPacket(double balance, List<Transaction> recentTransactions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncBankAccountPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_bank_account"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncBankAccountPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncBankAccountPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncBankAccountPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), decodeTransactions(buf));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(balance);
        buf.writeInt(recentTransactions.size());
        for (Transaction t : recentTransactions) {
            buf.writeUtf(t.getType().name(), 32767);
            buf.writeDouble(t.getAmount());
            buf.writeLong(t.getTimestamp());
            buf.writeUtf(t.getDescription() != null ? t.getDescription() : "", 32767);
            buf.writeBoolean(t.getOtherParty() != null);
            if (t.getOtherParty() != null) {
                buf.writeUUID(t.getOtherParty());
            }
        }
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            com.servermanagement.client.ClientBankData.setBalance(balance);
            com.servermanagement.client.ClientBankData.setTransactions(recentTransactions);
        });
        // packet handled
    }

    private static List<Transaction> decodeTransactions(FriendlyByteBuf buf) {
        int transactionCount = buf.readInt();
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < transactionCount; i++) {
            String typeName = buf.readUtf(32767);
            double amount = buf.readDouble();
            long timestamp = buf.readLong();
            String description = buf.readUtf(32767);
            boolean hasOtherParty = buf.readBoolean();
            UUID otherParty = hasOtherParty ? buf.readUUID() : null;
            TransactionType type;
            try {
                type = TransactionType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                type = TransactionType.ADMIN_GIVE;
            }
            transactions.add(new Transaction(type, amount, timestamp, description, otherParty));
        }
        return transactions;
    }
}
