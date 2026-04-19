package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class RequestWorldListPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RequestWorldListPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "request_world_list_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestWorldListPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestWorldListPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }


    public RequestWorldListPacket() {
    }

    public RequestWorldListPacket(FriendlyByteBuf buf) {
    }

        public void encode(FriendlyByteBuf buf) {
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                // Build world list and send back
                var worlds = com.servermanagement.features.worldmanager.WorldManager.getInstance()
                    .buildWorldListForClient(player.server);
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncWorldListPacket(worlds), player
                );
            }

}
}
