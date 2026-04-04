package com.servermanagement.network.packet;

import com.servermanagement.client.ClientGamblingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync gambling statistics
 */
public class SyncGamblingStatsPacket implements IPacket {
    private final long totalBets;
    private final long totalWins;
    private final long totalLosses;
    private final double totalWagered;
    private final double totalWon;
    private final double totalLost;
    private final double biggestWin;
    private final double biggestLoss;

    public SyncGamblingStatsPacket(long totalBets, long totalWins, long totalLosses,
                                   double totalWagered, double totalWon, double totalLost,
                                   double biggestWin, double biggestLoss) {
        this.totalBets = totalBets;
        this.totalWins = totalWins;
        this.totalLosses = totalLosses;
        this.totalWagered = totalWagered;
        this.totalWon = totalWon;
        this.totalLost = totalLost;
        this.biggestWin = biggestWin;
        this.biggestLoss = biggestLoss;
    }

    public SyncGamblingStatsPacket(FriendlyByteBuf buf) {
        this.totalBets = buf.readLong();
        this.totalWins = buf.readLong();
        this.totalLosses = buf.readLong();
        this.totalWagered = buf.readDouble();
        this.totalWon = buf.readDouble();
        this.totalLost = buf.readDouble();
        this.biggestWin = buf.readDouble();
        this.biggestLoss = buf.readDouble();
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update client-side gambling stats
            ClientGamblingData.updateStats(
                totalBets, totalWins, totalLosses,
                totalWagered, totalWon, totalLost,
                biggestWin, biggestLoss
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
