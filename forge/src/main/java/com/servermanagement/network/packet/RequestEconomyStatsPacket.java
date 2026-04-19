package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client-to-server packet requesting fresh economy statistics.
 * Server responds with SyncEconomyStatsPacket.
 */
public record RequestEconomyStatsPacket() implements IPacket {

    public RequestEconomyStatsPacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            // Only admins can request economy stats
            if (!player.hasPermissions(2)) return;
            SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());
        });
        ctx.setPacketHandled(true);
    }
}
