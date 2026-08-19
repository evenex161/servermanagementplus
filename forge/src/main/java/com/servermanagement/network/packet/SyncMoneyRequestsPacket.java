package com.servermanagement.network.packet;

import com.servermanagement.client.ClientMoneyRequestData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client: Sync money requests for display in the Bank GUI
 */
public record SyncMoneyRequestsPacket(List<ClientMoneyRequestData.RequestEntry> incoming,
                                       List<ClientMoneyRequestData.RequestEntry> outgoing) implements IPacket {

    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        this(readEntries(buf), readEntries(buf));
    }

    private static List<ClientMoneyRequestData.RequestEntry> readEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ClientMoneyRequestData.RequestEntry(
                buf.readUUID(), buf.readUtf(32767), buf.readDouble(),
                buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(32767)
            ));
        }
        return list;
    }

    @Override
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

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            ClientMoneyRequestData.setIncomingRequests(incoming);
            ClientMoneyRequestData.setOutgoingRequests(outgoing);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
