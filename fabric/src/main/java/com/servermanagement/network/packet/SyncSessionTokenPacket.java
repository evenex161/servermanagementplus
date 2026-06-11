package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public record SyncSessionTokenPacket(String token) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncSessionTokenPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "sync_session_token_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncSessionTokenPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncSessionTokenPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public SyncSessionTokenPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token, 256);
    }

    public void handle(net.minecraft.server.level.ServerPlayer player) {
        com.servermanagement.client.ClientSessionRegistry.setSessionToken(token);
    }
}
