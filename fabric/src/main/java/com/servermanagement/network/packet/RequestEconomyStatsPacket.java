package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
/**
 * Client-to-server packet requesting fresh economy statistics.
 * Server responds with SyncEconomyStatsPacket.
 */
public record RequestEconomyStatsPacket() implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "request_economy_stats_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public RequestEconomyStatsPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {}

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) return;
            // Only admins can request economy stats
            if (!player.hasPermissions(2)) return;
            SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());

}
}
