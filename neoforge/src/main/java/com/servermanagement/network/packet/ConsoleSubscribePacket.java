package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet to subscribe/unsubscribe from server log streaming.
 */
public record ConsoleSubscribePacket(boolean subscribe) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConsoleSubscribePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "console_subscribe"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ConsoleSubscribePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ConsoleSubscribePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ConsoleSubscribePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(subscribe);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                var manager = com.servermanagement.server.ServerConsoleManager.getInstance();
                if (subscribe) {
                    manager.subscribe(player);
                } else {
                    manager.unsubscribe(player.getUUID());
                }
            }
        });
        // packet handled
    }
}
