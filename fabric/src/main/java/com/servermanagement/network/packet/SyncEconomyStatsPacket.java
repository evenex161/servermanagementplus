package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyData;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.MarketPricingEngine;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;

/**
 * Packet to sync aggregate economy statistics from server to client.
 * Used by the Economy Management screen's Statistics tab.
 */
public class SyncEconomyStatsPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncEconomyStatsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_economy_stats_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncEconomyStatsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncEconomyStatsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final int totalAccounts;
    private final double totalMoneyInCirculation;
    private final double averageBalance;
    private final double richestBalance;
    private final String richestPlayerName;
    private final double inflationMultiplier;
    private final int activeListings;
    private final int totalTemplates;
    private final int enabledTemplates;
    // Transaction volume counts
    private final int totalTransactions;
    private final int purchaseCount;
    private final int saleCount;
    private final int gamblingBetCount;
    private final int gamblingWinCount;
    private final int freeRewardCount;
    private final int transferCount;
    private final double totalPurchaseVolume;
    private final double totalSaleVolume;
    private final double totalGamblingWagered;
    private final double totalGamblingWon;

    public SyncEconomyStatsPacket(int totalAccounts, double totalMoneyInCirculation, double averageBalance,
                                   double richestBalance, String richestPlayerName, double inflationMultiplier,
                                   int activeListings, int totalTemplates, int enabledTemplates,
                                   int totalTransactions, int purchaseCount, int saleCount,
                                   int gamblingBetCount, int gamblingWinCount, int freeRewardCount,
                                   int transferCount, double totalPurchaseVolume, double totalSaleVolume,
                                   double totalGamblingWagered, double totalGamblingWon) {
        this.totalAccounts = totalAccounts;
        this.totalMoneyInCirculation = totalMoneyInCirculation;
        this.averageBalance = averageBalance;
        this.richestBalance = richestBalance;
        this.richestPlayerName = richestPlayerName;
        this.inflationMultiplier = inflationMultiplier;
        this.activeListings = activeListings;
        this.totalTemplates = totalTemplates;
        this.enabledTemplates = enabledTemplates;
        this.totalTransactions = totalTransactions;
        this.purchaseCount = purchaseCount;
        this.saleCount = saleCount;
        this.gamblingBetCount = gamblingBetCount;
        this.gamblingWinCount = gamblingWinCount;
        this.freeRewardCount = freeRewardCount;
        this.transferCount = transferCount;
        this.totalPurchaseVolume = totalPurchaseVolume;
        this.totalSaleVolume = totalSaleVolume;
        this.totalGamblingWagered = totalGamblingWagered;
        this.totalGamblingWon = totalGamblingWon;
    }

    public SyncEconomyStatsPacket(FriendlyByteBuf buf) {
        this.totalAccounts = buf.readInt();
        this.totalMoneyInCirculation = buf.readDouble();
        this.averageBalance = buf.readDouble();
        this.richestBalance = buf.readDouble();
        this.richestPlayerName = buf.readUtf(64);
        this.inflationMultiplier = buf.readDouble();
        this.activeListings = buf.readInt();
        this.totalTemplates = buf.readInt();
        this.enabledTemplates = buf.readInt();
        this.totalTransactions = buf.readInt();
        this.purchaseCount = buf.readInt();
        this.saleCount = buf.readInt();
        this.gamblingBetCount = buf.readInt();
        this.gamblingWinCount = buf.readInt();
        this.freeRewardCount = buf.readInt();
        this.transferCount = buf.readInt();
        this.totalPurchaseVolume = buf.readDouble();
        this.totalSaleVolume = buf.readDouble();
        this.totalGamblingWagered = buf.readDouble();
        this.totalGamblingWon = buf.readDouble();
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(totalAccounts);
        buf.writeDouble(totalMoneyInCirculation);
        buf.writeDouble(averageBalance);
        buf.writeDouble(richestBalance);
        buf.writeUtf(richestPlayerName, 64);
        buf.writeDouble(inflationMultiplier);
        buf.writeInt(activeListings);
        buf.writeInt(totalTemplates);
        buf.writeInt(enabledTemplates);
        buf.writeInt(totalTransactions);
        buf.writeInt(purchaseCount);
        buf.writeInt(saleCount);
        buf.writeInt(gamblingBetCount);
        buf.writeInt(gamblingWinCount);
        buf.writeInt(freeRewardCount);
        buf.writeInt(transferCount);
        buf.writeDouble(totalPurchaseVolume);
        buf.writeDouble(totalSaleVolume);
        buf.writeDouble(totalGamblingWagered);
        buf.writeDouble(totalGamblingWon);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientPacketHandler.handleEconomyStats(
                totalAccounts, totalMoneyInCirculation, averageBalance,
                richestBalance, richestPlayerName, inflationMultiplier,
                activeListings, totalTemplates, enabledTemplates,
                totalTransactions, purchaseCount, saleCount,
                gamblingBetCount, gamblingWinCount, freeRewardCount,
                transferCount, totalPurchaseVolume, totalSaleVolume,
                totalGamblingWagered, totalGamblingWon
            );

}

    /**
     * Collect and send economy statistics to a player
     */
    public static void syncToPlayer(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;
        EconomyManager economyManager = EconomyManager.getInstance();
        if (economyManager == null) return;

        EconomyData data = economyManager.getData();
        if (data == null) return;
        Map<UUID, BankAccount> accounts = data.getAllAccounts();

        int totalAccounts = accounts.size();
        double totalMoney = 0;
        double richestBalance = 0;
        String richestName = "N/A";

        // Transaction counters
        int totalTx = 0;
        int purchases = 0, sales = 0, bets = 0, wins = 0, freeRewards = 0, transfers = 0;
        double purchaseVol = 0, saleVol = 0, gamblingWagered = 0, gamblingWon = 0;

        for (Map.Entry<UUID, BankAccount> entry : accounts.entrySet()) {
            BankAccount account = entry.getValue();
            double bal = account.getBalance();
            totalMoney += bal;

            if (bal > richestBalance) {
                richestBalance = bal;
                // Try to resolve player name
                var profile = server.getProfileCache();
                if (profile != null) {
                    var optional = profile.get(entry.getKey());
                    richestName = optional.map(p -> p.getName()).orElse("Unknown");
                }
            }

            // Aggregate transactions (snapshot to avoid ConcurrentModificationException)
            for (Transaction tx : new java.util.ArrayList<>(account.getTransactions())) {
                totalTx++;
                switch (tx.getType()) {
                    case MINEBAY_PURCHASE -> { purchases++; purchaseVol += tx.getAmount(); }
                    case MINEBAY_SALE -> { sales++; saleVol += tx.getAmount(); }
                    case GAMBLING_BET -> { bets++; gamblingWagered += tx.getAmount(); }
                    case GAMBLING_WIN -> { wins++; gamblingWon += tx.getAmount(); }
                    case FREE_REWARD -> freeRewards++;
                    case PLAYER_TRANSFER_SENT, PLAYER_TRANSFER_RECEIVED -> transfers++;
                    default -> {}
                }
            }
        }

        double avgBalance = totalAccounts > 0 ? totalMoney / totalAccounts : 0;
        double inflation = MarketPricingEngine.getInstance().getInflationMultiplier();
        int activeListings = MineBayManager.getInstance().getActiveListings().size();

        // Template stats
        var templateManager = economyManager.getTemplateManager();
        int totalTemplates = 0, enabledTemplates = 0;
        if (templateManager != null) {
            var stats = templateManager.getStatistics();
            totalTemplates = stats.getOrDefault("total", 0);
            enabledTemplates = stats.getOrDefault("enabled", 0);
        }

        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncEconomyStatsPacket(
                totalAccounts, totalMoney, avgBalance, richestBalance, richestName,
                inflation, activeListings, totalTemplates, enabledTemplates,
                totalTx, purchases, sales, bets, wins, freeRewards, transfers,
                purchaseVol, saleVol, gamblingWagered, gamblingWon
            ), player
        );
    }
}
