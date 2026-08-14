package com.servermanagement.network.packet;

import com.servermanagement.client.ClientMoneyRequestData;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server â†’ Client: Sync money requests for display in the Bank GUI
 */
public record SyncMoneyRequestsPacket(List<ClientMoneyRequestData.RequestEntry> incoming, List<ClientMoneyRequestData.RequestEntry> outgoing) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncMoneyRequestsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_money_requests_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMoneyRequestsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMoneyRequestsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        this(decodeEntries(buf), decodeEntries(buf));
    }

    private static List<ClientMoneyRequestData.RequestEntry> decodeEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ClientMoneyRequestData.RequestEntry(
                buf.readUUID(), buf.readUtf(32767), buf.readDouble(),
                buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(32767)));
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
        buf.writeUtf(entry.getPlayerName(), 32767);
        buf.writeDouble(entry.getAmount());
        buf.writeUtf(entry.getMessage(), 32767);
        buf.writeUtf(entry.getAge(), 32767);
        buf.writeUtf(entry.getStatus(), 32767);
    }

    private ClientMoneyRequestData.RequestEntry readEntry(FriendlyByteBuf buf) {
        return new ClientMoneyRequestData.RequestEntry(
            buf.readUUID(),
            buf.readUtf(32767),
            buf.readDouble(),
            buf.readUtf(32767),
            buf.readUtf(32767),
            buf.readUtf(32767)
        );
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientMoneyRequestData.setIncomingRequests(incoming);
            ClientMoneyRequestData.setOutgoingRequests(outgoing);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}
}
