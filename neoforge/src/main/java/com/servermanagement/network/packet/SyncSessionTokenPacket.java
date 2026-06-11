package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;

public record SyncSessionTokenPacket(String token) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncSessionTokenPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "sync_session_token"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncSessionTokenPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncSessionTokenPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public SyncSessionTokenPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token, 256);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            com.servermanagement.client.ClientSessionRegistry.setSessionToken(token);
        });
    }
}
