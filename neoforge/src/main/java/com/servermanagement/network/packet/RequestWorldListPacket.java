package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record RequestWorldListPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestWorldListPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "request_world_list"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestWorldListPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestWorldListPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public RequestWorldListPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null) {
                // Build world list and send back
                var worlds = com.servermanagement.features.worldmanager.WorldManager.getInstance()
                    .buildWorldListForClient(player.level().getServer());
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncWorldListPacket(worlds), player
                );
            }
        });
        // packet handled
    }
}
