package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client: Syncs the current MOTD text before opening the editor.
 */
public record SyncMotdPacket(String motdText) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncMotdPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "sync_motd"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMotdPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMotdPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncMotdPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.handleMotdSync(motdText);
        });
        // packet handled
    }

    public String getMotdText() {
        return motdText;
    }
}
