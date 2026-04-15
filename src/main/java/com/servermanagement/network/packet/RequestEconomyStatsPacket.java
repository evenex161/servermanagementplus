package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client-to-server packet requesting fresh economy statistics.
 * Server responds with SyncEconomyStatsPacket.
 */
public class RequestEconomyStatsPacket implements IPacket {

    public RequestEconomyStatsPacket() {}

    public RequestEconomyStatsPacket(FriendlyByteBuf buf) {}

    @Override
    public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // Only admins can request economy stats
            if (!player.hasPermissions(2)) return;
            SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());
        });
        ctx.get().setPacketHandled(true);
    }
}
