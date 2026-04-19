package com.servermanagement.network.packet;

import com.servermanagement.features.gambling.GamblingGame;
import com.servermanagement.features.gambling.GamblingManager;
import com.servermanagement.features.gambling.GamblingResult;
import com.servermanagement.features.gambling.games.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to place a gambling bet
 */
public class PlaceGamblingBetPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<PlaceGamblingBetPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "place_gambling_bet_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, PlaceGamblingBetPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), PlaceGamblingBetPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static final ScheduledExecutorService DELAYED_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ServerManagement-GamblingDelay");
        t.setDaemon(true);
        return t;
    });

    private final GameType gameType;
    private final double betAmount;
    private final String gameOption; // e.g., "heads", "HIGH", "RED", etc.
    
    public enum GameType {
        COIN_FLIP,
        DICE_ROLL,
        SLOT_MACHINE,
        ROULETTE
    }
    
    public PlaceGamblingBetPacket(GameType gameType, double betAmount, String gameOption) {
        this.gameType = gameType;
        this.betAmount = betAmount;
        this.gameOption = gameOption;
    }
    
    public PlaceGamblingBetPacket(FriendlyByteBuf buf) {
        this.gameType = buf.readEnum(GameType.class);
        this.betAmount = buf.readDouble();
        this.gameOption = buf.readUtf(64);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeDouble(this.betAmount);
        buf.writeUtf(this.gameOption, 64);
    }
    
    public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) {
                return; // No player - reject packet
            }
            
            // Input validation - prevent exploits
            if (Double.isNaN(this.betAmount) || Double.isInfinite(this.betAmount)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid bet amount"));
                return;
            }
            
            if (this.betAmount < 10.0 || this.betAmount > 10000.0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cBet amount must be between $10 and $10,000"));
                return;
            }
            
            if (this.gameOption == null || this.gameOption.length() > 50) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid game option"));
                return;
            }
            
            GamblingManager gamblingManager = GamblingManager.getInstance();
            
            // Create the appropriate game with validation
            GamblingGame game;
            try {
                game = createGame(this.gameType, this.gameOption);
            } catch (Exception e) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid game parameters"));
                return;
            }
            
            if (game != null) {
                // Send tension packet immediately to start animation
                GamblingTensionPacket.GameType tensionGameType;
                switch (this.gameType) {
                    case COIN_FLIP:
                        tensionGameType = GamblingTensionPacket.GameType.COIN_FLIP;
                        break;
                    case DICE_ROLL:
                        tensionGameType = GamblingTensionPacket.GameType.DICE_ROLL;
                        break;
                    case SLOT_MACHINE:
                        tensionGameType = GamblingTensionPacket.GameType.SLOT_MACHINE;
                        break;
                    case ROULETTE:
                        tensionGameType = GamblingTensionPacket.GameType.ROULETTE;
                        break;
                    default:
                        tensionGameType = GamblingTensionPacket.GameType.COIN_FLIP;
                }
                
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new GamblingTensionPacket(tensionGameType, this.gameOption),
                    player
                );
                
                // Place the bet
                GamblingResult result = gamblingManager.placeBet(player, this.betAmount, game);
                
                // Immediately update displayed balance so player sees the deduction
                // during the tension animation (before the delayed result reveal)
                com.servermanagement.features.economy.EconomyManager economyManager = 
                    com.servermanagement.features.economy.EconomyManager.getInstance();
                com.servermanagement.features.economy.BankAccount account = 
                    economyManager.getOrCreateAccount(player.getUUID());
                if (player.containerMenu instanceof com.servermanagement.gui.gambling.MineStacksMenu) {
                    ((com.servermanagement.gui.gambling.MineStacksMenu) player.containerMenu)
                        .updateBalance(account.getBalance());
                }
                
                // Get stats for later sync
                com.servermanagement.features.gambling.GamblingStats stats = 
                    gamblingManager.getStats(player.getUUID());
                
                // Capture player UUID for safe re-lookup after delay
                java.util.UUID playerUUID = player.getUUID();
                
                // Schedule delayed result reveal (3 seconds) without blocking server thread
                MinecraftServer server = gamblingManager.getServer();
                if (server != null) {
                    DELAYED_EXECUTOR.schedule(() -> {
                        // Execute on the main server thread for thread safety
                        server.execute(() -> {
                            // Re-lookup player by UUID — original reference may be stale
                            // (player could have disconnected/reconnected during the 3s delay)
                            ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUUID);
                            if (currentPlayer == null) return; // Player disconnected
                            
                            // Re-fetch account with fresh data
                            com.servermanagement.features.economy.BankAccount freshAccount = 
                                economyManager.getOrCreateAccount(playerUUID);
                            
                            // Send result back to client after delay
                            com.servermanagement.network.ModNetworking.sendToPlayer(
                                new GamblingResultPacket(result.isWon(), result.getPayout(), result.getMessage()),
                                currentPlayer
                            );
                            
                            // Sync updated balance to client
                            com.servermanagement.network.ModNetworking.sendToPlayer(
                                new SyncBankAccountPacket(freshAccount.getBalance(), freshAccount.getTransactions()),
                                currentPlayer
                            );
                            
                            // Update MineStacks menu balance if the player has it open
                            if (currentPlayer.containerMenu instanceof com.servermanagement.gui.gambling.MineStacksMenu) {
                                ((com.servermanagement.gui.gambling.MineStacksMenu) currentPlayer.containerMenu)
                                    .updateBalance(freshAccount.getBalance());
                            }
                            
                            // Sync gambling stats to client
                            com.servermanagement.network.ModNetworking.sendToPlayer(
                                new SyncGamblingStatsPacket(
                                    stats.getTotalBets(),
                                    stats.getTotalWins(),
                                    stats.getTotalLosses(),
                                    stats.getTotalWagered(),
                                    stats.getTotalWon(),
                                    stats.getTotalLost(),
                                    stats.getBiggestWin(),
                                    stats.getBiggestLoss()
                                ),
                                currentPlayer
                            );
                        });
                    }, 3, TimeUnit.SECONDS);
                }
            }
    }
    
    private static GamblingGame createGame(GameType type, String option) {
        switch (type) {
            case COIN_FLIP:
                return new CoinFlipGame(option);
            case DICE_ROLL:
                return new DiceRollGame(DiceRollGame.BetType.valueOf(option));
            case SLOT_MACHINE:
                return new SlotMachineGame();
            case ROULETTE:
                return new RouletteGame(RouletteGame.BetType.valueOf(option.split(":")[0]), 
                                      option.contains(":") ? Integer.parseInt(option.split(":")[1]) : 0);
            default:
                return null;
        }
    }

    /**
     * Provides shared delayed executor for gambling result scheduling
     */
    public static ScheduledExecutorService getDelayedExecutor() {
        return DELAYED_EXECUTOR;
    }
}
