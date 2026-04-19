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
 * Server → Client: Sync money requests for display in the Bank GUI
 */
public class SyncMoneyRequestsPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncMoneyRequestsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_money_requests"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMoneyRequestsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMoneyRequestsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final List<ClientMoneyRequestData.RequestEntry> incoming;
    private final List<ClientMoneyRequestData.RequestEntry> outgoing;

    public SyncMoneyRequestsPacket(List<ClientMoneyRequestData.RequestEntry> incoming,
                                    List<ClientMoneyRequestData.RequestEntry> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        int inCount = buf.readInt();
        this.incoming = new ArrayList<>();
        for (int i = 0; i < inCount; i++) {
            incoming.add(readEntry(buf));
        }
        int outCount = buf.readInt();
        this.outgoing = new ArrayList<>();
        for (int i = 0; i < outCount; i++) {
            outgoing.add(readEntry(buf));
        }
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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientMoneyRequestData.setIncomingRequests(incoming);
            ClientMoneyRequestData.setOutgoingRequests(outgoing);
        });
        // packet handled
    }
}
