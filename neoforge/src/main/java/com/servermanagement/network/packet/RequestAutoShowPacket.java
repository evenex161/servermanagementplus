package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class RequestAutoShowPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestAutoShowPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "request_auto_show"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestAutoShowPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestAutoShowPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public RequestAutoShowPacket() {
    }

    public RequestAutoShowPacket(FriendlyByteBuf buf) {
    }

        public void encode(FriendlyByteBuf buf) {
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null) {
                boolean autoShow = com.servermanagement.config.PlayerPreferences.getAutoShow(player.getUUID());
                // Send response back - stub for now
                com.servermanagement.network.ModNetworking.sendToPlayer(new SyncAutoShowPacket(autoShow), player);
            }
        });
        // packet handled
    }
}
