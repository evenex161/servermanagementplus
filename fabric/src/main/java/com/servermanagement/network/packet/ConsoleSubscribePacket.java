package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
/**
 * Client-to-server packet to subscribe/unsubscribe from server log streaming.
 */
public record ConsoleSubscribePacket(boolean subscribe) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ConsoleSubscribePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "console_subscribe_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleSubscribePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleSubscribePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public ConsoleSubscribePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(subscribe);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                var manager = com.servermanagement.server.ServerConsoleManager.getInstance();
                if (subscribe) {
                    manager.subscribe(player);
                } else {
                    manager.unsubscribe(player.getUUID());
                }
            }

}
}
