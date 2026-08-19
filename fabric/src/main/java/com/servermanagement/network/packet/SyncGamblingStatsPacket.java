package com.servermanagement.network.packet;

import com.servermanagement.client.ClientGamblingData;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync gambling statistics
 */
public record SyncGamblingStatsPacket(long totalBets, long totalWins, long totalLosses, double totalWagered, double totalWon, double totalLost, double biggestWin, double biggestLoss) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_gambling_stats_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncGamblingStatsPacket(FriendlyByteBuf buf) {
        this(buf.readLong(), buf.readLong(), buf.readLong(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(this.totalBets);
        buf.writeLong(this.totalWins);
        buf.writeLong(this.totalLosses);
        buf.writeDouble(this.totalWagered);
        buf.writeDouble(this.totalWon);
        buf.writeDouble(this.totalLost);
        buf.writeDouble(this.biggestWin);
        buf.writeDouble(this.biggestLoss);
    }

    public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Update client-side gambling stats
            ClientGamblingData.updateStats(
                totalBets, totalWins, totalLosses,
                totalWagered, totalWon, totalLost,
                biggestWin, biggestLoss
            );
            com.servermanagement.client.ClientScreenManager.refreshOpenScreen();

}
}
