package com.servermanagement.network.packet;

import com.servermanagement.client.ClientMoneyRequestData;
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
 * Server â†’ Client: Sync money requests for display in the Bank GUI
 */
public record SyncMoneyRequestsPacket(List<ClientMoneyRequestData.RequestEntry> incoming, List<ClientMoneyRequestData.RequestEntry> outgoing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncMoneyRequestsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_money_requests"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMoneyRequestsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMoneyRequestsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        this(decodeEntries(buf), decodeEntries(buf));
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

    private static void writeEntry(FriendlyByteBuf buf, ClientMoneyRequestData.RequestEntry entry) {
        buf.writeUUID(entry.getRequestId());
        buf.writeUtf(entry.getPlayerName(), 32767);
        buf.writeDouble(entry.getAmount());
        buf.writeUtf(entry.getMessage(), 32767);
        buf.writeUtf(entry.getAge(), 32767);
        buf.writeUtf(entry.getStatus(), 32767);
    }

    private static ClientMoneyRequestData.RequestEntry readEntry(FriendlyByteBuf buf) {
        return new ClientMoneyRequestData.RequestEntry(
            buf.readUUID(),
            buf.readUtf(32767),
            buf.readDouble(),
            buf.readUtf(32767),
            buf.readUtf(32767),
            buf.readUtf(32767)
        );
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientMoneyRequestData.setIncomingRequests(incoming);
            ClientMoneyRequestData.setOutgoingRequests(outgoing);
        });
        // packet handled
    }

    private static List<ClientMoneyRequestData.RequestEntry> decodeEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(readEntry(buf));
        }
        return list;
    }
}
