package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server packet requesting fresh economy statistics.
 * Server responds with SyncEconomyStatsPacket.
 */
public class RequestEconomyStatsPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestEconomyStatsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "request_economy_stats"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestEconomyStatsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestEconomyStatsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public RequestEconomyStatsPacket() {}

    public RequestEconomyStatsPacket(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null) return;
            // Only admins can request economy stats
            if (!player.hasPermissions(2)) return;
            SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());
        });
        // packet handled
    }
}
