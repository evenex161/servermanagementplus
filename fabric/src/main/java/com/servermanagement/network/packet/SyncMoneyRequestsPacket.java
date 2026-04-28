package com.servermanagement.network.packet;

import com.servermanagement.client.ClientMoneyRequestData;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server ÔåÆ Client: Sync money requests for display in the Bank GUI
 */
public record SyncMoneyRequestsPacket(List<ClientMoneyRequestData.RequestEntry> incoming, List<ClientMoneyRequestData.RequestEntry> outgoing) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_money_requests_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        this(decodeEntries(buf), decodeEntries(buf));
    }

    private static List<ClientMoneyRequestData.RequestEntry> decodeEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ClientMoneyRequestData.RequestEntry(
                buf.readUUID(), buf.readUtf(16), buf.readDouble(),
                buf.readUtf(256), buf.readUtf(64), buf.readUtf(32)));
        }
        return list;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(incoming.size());
        for (ClientMoneyRequestData.RequestEntry entry : incoming) {
            writeEntry(buf, entry);
        }
        buf.writeInt(outgoing.size());
        for (ClientMoneyRequestData.RequestEntry entry : outgoing) {
            writeEntry(buf, entry);
        }
    }

    private void writeEntry(FriendlyByteBuf buf, ClientMoneyRequestData.RequestEntry entry) {
        buf.writeUUID(entry.getRequestId());
        buf.writeUtf(entry.getPlayerName(), 16);
        buf.writeDouble(entry.getAmount());
        buf.writeUtf(entry.getMessage(), 256);
        buf.writeUtf(entry.getAge(), 64);
        buf.writeUtf(entry.getStatus(), 32);
    }

    private ClientMoneyRequestData.RequestEntry readEntry(FriendlyByteBuf buf) {
        return new ClientMoneyRequestData.RequestEntry(
            buf.readUUID(),
            buf.readUtf(16),
            buf.readDouble(),
            buf.readUtf(256),
            buf.readUtf(64),
            buf.readUtf(32)
        );
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientMoneyRequestData.setIncomingRequests(incoming);
            ClientMoneyRequestData.setOutgoingRequests(outgoing);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}
}
