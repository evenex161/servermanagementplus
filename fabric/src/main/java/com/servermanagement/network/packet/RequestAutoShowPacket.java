package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record RequestAutoShowPacket() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RequestAutoShowPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "request_auto_show_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestAutoShowPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestAutoShowPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public RequestAutoShowPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                boolean autoShow = com.servermanagement.config.PlayerPreferences.getAutoShow(player.getUUID());
                // Send response back - stub for now
                com.servermanagement.network.ModNetworking.sendToPlayer(player, new SyncAutoShowPacket(autoShow));
            }

}
}
