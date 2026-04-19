package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyData;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.MarketPricingEngine;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.minebay.MineBayManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;

/**
 * Packet to sync aggregate economy statistics from server to client.
 * Used by the Economy Management screen's Statistics tab.
 */
public record SyncEconomyStatsPacket(int totalAccounts, double totalMoneyInCirculation, double averageBalance, double richestBalance, String richestPlayerName, double inflationMultiplier, int activeListings, int totalTemplates, int enabledTemplates, int totalTransactions, int purchaseCount, int saleCount, int gamblingBetCount, int gamblingWinCount, int freeRewardCount, int transferCount, double totalPurchaseVolume, double totalSaleVolume, double totalGamblingWagered, double totalGamblingWon) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncEconomyStatsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_economy_stats"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncEconomyStatsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncEconomyStatsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    // Transaction volume counts


    public SyncEconomyStatsPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf(64), buf.readDouble(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.handleEconomyStats(
                totalAccounts, totalMoneyInCirculation, averageBalance,
                richestBalance, richestPlayerName, inflationMultiplier,
                activeListings, totalTemplates, enabledTemplates,
                totalTransactions, purchaseCount, saleCount,
                gamblingBetCount, gamblingWinCount, freeRewardCount,
                transferCount, totalPurchaseVolume, totalSaleVolume,
                totalGamblingWagered, totalGamblingWon
            );
        });
        // packet handled
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
