package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.gambling.GamblingManager;
import com.servermanagement.features.gambling.GamblingResult;
import com.servermanagement.features.gambling.ItemValuation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to place a gambling bet with an item
 */
public class PlaceGamblingBetWithItemPacket implements IPacket {
    public static final CustomPacketPayload.Type<PlaceGamblingBetWithItemPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "place_gambling_bet_with_item_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PlaceGamblingBetWithItemPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), PlaceGamblingBetWithItemPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final PlaceGamblingBetPacket.GameType gameType;
    private final String gameOption;
    
    public PlaceGamblingBetWithItemPacket(PlaceGamblingBetPacket.GameType gameType, String gameOption) {
        this.gameType = gameType;
        this.gameOption = gameOption;
    }
    
    public PlaceGamblingBetWithItemPacket(FriendlyByteBuf buf) {
        this.gameType = buf.readEnum(PlaceGamblingBetPacket.GameType.class);
        this.gameOption = buf.readUtf(64);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.gameType);
        buf.writeUtf(this.gameOption, 64);
    }
    
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return; // No player - reject packet
            }
            
            // Validate game option
            if (this.gameOption == null || this.gameOption.length() > 50) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid game option"));
                return;
            }
            
            if (player.containerMenu instanceof com.servermanagement.gui.gambling.MineStacksMenu) {
                com.servermanagement.gui.gambling.MineStacksMenu menu = 
                    (com.servermanagement.gui.gambling.MineStacksMenu) player.containerMenu;
                
                ItemStack bettingItem = menu.getBettingItem();
                
                // Validate item
                if (bettingItem == null || bettingItem.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cNo betting item found"));
                    return;
                }
                
                if (!ItemValuation.isItemGambleable(bettingItem)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cThis item cannot be used for gambling"));
                    return;
                }
                
                GamblingManager gamblingManager = GamblingManager.getInstance();
                
                // Create the appropriate game with error handling
                com.servermanagement.features.gambling.GamblingGame game;
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
                    if (server != null) {
                        PlaceGamblingBetPacket.getDelayedExecutor().schedule(() -> {
                            // Execute on the main server thread for thread safety
                            server.execute(() -> {
                                // Send result back to client after delay
                                com.servermanagement.network.ModNetworking.sendToPlayer(
                                    new GamblingResultPacket(result.isWon(), result.getPayout(), result.getMessage()),
                                    player
                                );
                                
                                // Sync updated balance to client
                                com.servermanagement.network.ModNetworking.sendToPlayer(
                                    new SyncBankAccountPacket(account.getBalance(), account.getRecentTransactions(10)),
                                    player
                                );
                                
                                // Update MineStacks menu balance
                                menu.updateBalance(account.getBalance());
                                
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
                                    player
                                );
                            });
                        }, 3, TimeUnit.SECONDS);
                    }
                }
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid gambling menu state"));
            }
        });
        
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
