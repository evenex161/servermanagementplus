package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
/**
 * Client-to-server packet requesting fresh economy statistics.
 * Server responds with SyncEconomyStatsPacket.
 */
public class RequestEconomyStatsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RequestEconomyStatsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "request_economy_stats_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestEconomyStatsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestEconomyStatsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }


    public RequestEconomyStatsPacket() {}

    public RequestEconomyStatsPacket(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) return;
            // Only admins can request economy stats
            if (!player.hasPermissions(2)) return;
            SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());

}
}
