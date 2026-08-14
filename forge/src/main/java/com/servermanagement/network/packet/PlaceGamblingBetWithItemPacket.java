package com.servermanagement.network.packet;

import com.servermanagement.features.gambling.GamblingManager;
import com.servermanagement.features.gambling.GamblingResult;
import com.servermanagement.features.gambling.ItemValuation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to place a gambling bet with an item
 */
public record PlaceGamblingBetWithItemPacket(PlaceGamblingBetPacket.GameType gameType, String gameOption) implements IPacket {
    
    public PlaceGamblingBetWithItemPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(PlaceGamblingBetPacket.GameType.class), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeUtf(this.gameOption, 32767);
    }
    
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return; // No player - reject packet
            }
            
            // Validate game option
            if (this.gameOption == null || this.gameOption.length() > 50) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Â§cInvalid game option"));
                return;
            }
            
            if (player.containerMenu instanceof com.servermanagement.gui.gambling.MineStacksMenu) {
                com.servermanagement.gui.gambling.MineStacksMenu menu = 
                    (com.servermanagement.gui.gambling.MineStacksMenu) player.containerMenu;
                
                ItemStack bettingItem = menu.getBettingItem();
                
                // Validate item
                if (bettingItem == null || bettingItem.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Â§cNo betting item found"));
                    return;
                }
                
                if (!ItemValuation.isItemGambleable(bettingItem)) {
                    // Double-check with market pricing engine for items not in hardcoded list
                    double marketValue = com.servermanagement.features.economy.MarketPricingEngine.getInstance()
                        .getBasePrice(bettingItem) * bettingItem.getCount();
                    if (marketValue < GamblingManager.MIN_BET) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Â§cThis item cannot be used for gambling (value: $" + 
                            String.format("%.2f", marketValue) + ", min: $" + 
                            String.format("%.0f", GamblingManager.MIN_BET) + ")"));
                        return;
                    }
                }
                
                GamblingManager gamblingManager = GamblingManager.getInstance();
                
                // Create the appropriate game with error handling
                com.servermanagement.features.gambling.GamblingGame game;
                try {
                    game = createGame(this.gameType, this.gameOption);
                } catch (Exception e) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Â§cInvalid game parameters"));
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
                    
                    // Place the bet with item
                    GamblingResult result = gamblingManager.placeBetWithItem(player, bettingItem, game);
                    
                    // Clear the betting slot
                    menu.clearBettingSlot();
                    
                    // Immediately update displayed balance so player sees it during tension animation
                    com.servermanagement.features.economy.EconomyManager economyManager = 
                        com.servermanagement.features.economy.EconomyManager.getInstance();
                    com.servermanagement.features.economy.BankAccount account = 
                        economyManager.getOrCreateAccount(player.getUUID());
                    menu.updateBalance(account.getBalance());
                    
                    // Get stats for later sync
                    com.servermanagement.features.gambling.GamblingStats stats = 
                        gamblingManager.getStats(player.getUUID());
                    
                    // Schedule delayed result reveal (3 seconds) without blocking server thread
                    net.minecraft.server.MinecraftServer server = gamblingManager.getServer();
                    final java.util.UUID playerUUID = player.getUUID();
                    if (server != null) {
                        PlaceGamblingBetPacket.getDelayedExecutor().schedule(() -> {
                            // Execute on the main server thread for thread safety
                            server.execute(() -> {
                                // Re-lookup player to avoid stale reference
                                ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerUUID);
                                if (onlinePlayer == null) return; // Player disconnected
                                
                                // Send result back to client after delay
                                com.servermanagement.network.ModNetworking.sendToPlayer(
                                    new GamblingResultPacket(result.isWon(), result.getPayout(), result.getMessage()),
                                    onlinePlayer
                                );
                                
                                // Sync updated balance to client
                                com.servermanagement.network.ModNetworking.sendToPlayer(
                                    new SyncBankAccountPacket(account.getBalance(), account.getTransactions()),
                                    onlinePlayer
                                );
                                
                                // Update MineStacks menu balance if the player still has it open
                                if (onlinePlayer.containerMenu instanceof com.servermanagement.gui.gambling.MineStacksMenu mineStacksMenu) {
                                    mineStacksMenu.updateBalance(account.getBalance());
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
                                    onlinePlayer
                                );
                            });
                        }, 3, TimeUnit.SECONDS);
                    }
                }
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Â§cInvalid gambling menu state"));
            }
        });
        ctx.setPacketHandled(true);
    }
    
    private static com.servermanagement.features.gambling.GamblingGame createGame(
            PlaceGamblingBetPacket.GameType type, String option) {
        switch (type) {
            case COIN_FLIP:
                return new com.servermanagement.features.gambling.games.CoinFlipGame(option);
            case DICE_ROLL:
                return new com.servermanagement.features.gambling.games.DiceRollGame(
                    com.servermanagement.features.gambling.games.DiceRollGame.BetType.valueOf(option));
            case SLOT_MACHINE:
                return new com.servermanagement.features.gambling.games.SlotMachineGame();
            case ROULETTE:
                return new com.servermanagement.features.gambling.games.RouletteGame(
                    com.servermanagement.features.gambling.games.RouletteGame.BetType.valueOf(option.split(":")[0]), 
                    option.contains(":") ? Integer.parseInt(option.split(":")[1]) : 0);
            default:
                return null;
        }
    }
}
