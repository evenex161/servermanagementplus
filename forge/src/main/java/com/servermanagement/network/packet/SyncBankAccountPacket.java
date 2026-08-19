package com.servermanagement.network.packet;

import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet to sync bank account data from server to client
 */
public record SyncBankAccountPacket(double balance, List<Transaction> recentTransactions) implements IPacket {

    public SyncBankAccountPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), readTransactions(buf));
    }

    private static List<Transaction> readTransactions(FriendlyByteBuf buf) {
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

    @Override
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

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.servermanagement.client.ClientBankData.setBalance(balance);
            com.servermanagement.client.ClientBankData.setTransactions(recentTransactions);
        });
        ctx.get().setPacketHandled(true);
    }
}
