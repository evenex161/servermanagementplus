package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

public record OpenGuiPacket(GuiType guiType, String data) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "open_gui_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


// Can hold dimension ID or other data
    public OpenGuiPacket(GuiType guiType) {
        this(guiType, "");
    }
    public OpenGuiPacket(FriendlyByteBuf buf) {
        this(decodeGuiType(buf.readInt()), buf.readUtf(32767));
    }

    private static GuiType decodeGuiType(int ordinal) {
        GuiType[] values = GuiType.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : GuiType.DASHBOARD;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(guiType.ordinal());
        buf.writeUtf(data);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                // Admin GUIs require OP level 2
                if (guiType.isAdminOnly() && !player.hasPermissions(2)) {
                    return;
                }
                // Open the appropriate GUI
                switch (guiType) {
                    case CONFIG:
                        // Send feature states before opening config
                        syncFeatureStates(player);
                        player.openMenu(new com.servermanagement.gui.ConfigMenuProvider());
                        break;
                    case DASHBOARD:
                        player.openMenu(new com.servermanagement.gui.provider.DashboardMenuProvider());
                        break;
                    case GLOBAL_SETTINGS:
                        // Send global settings sync before opening
                        syncGlobalSettings(player);
                        player.openMenu(new com.servermanagement.gui.GlobalSettingsMenuProvider());
                        break;
                    case WORLD_LIST:
                        // Send world list sync before opening
                        syncWorldList(player);
                        player.openMenu(new com.servermanagement.gui.WorldListMenuProvider());
                        break;
                    case WORLD_DETAIL:
                        // Send world detail sync before opening
                        syncWorldDetail(player, data);
                        player.openMenu(new com.servermanagement.gui.WorldDetailMenuProvider(data));
                        break;
                    case PLAYER_MANAGER:
                        player.openMenu(new com.servermanagement.gui.PlayerManagerMenuProvider());
                        break;
                    case CONSOLE:
                        player.openMenu(new com.servermanagement.gui.provider.ConsoleMenuProvider());
                        break;
                    case PORTAL_TIMER:
                        // Send portal timer sync before opening
                        syncWorldDetail(player, data);
                        player.openMenu(new com.servermanagement.gui.PortalTimerMenuProvider(data));
                        break;
                    case BANK:
                        // Send bank account sync before opening
                        syncBankAccount(player);
                        // Send bank inventory sync
                        syncBankInventory(player);
                        // Send money requests sync
                        syncMoneyRequests(player);
                        player.openMenu(new com.servermanagement.gui.economy.BankMenuProvider());
                        break;
                    case DAILY_TASKS:
                        // Send daily tasks sync before opening
                        syncDailyTasks(player);
                        player.openMenu(new com.servermanagement.gui.economy.DailyTasksMenuProvider());
                        break;
                    case ACHIEVEMENTS:
                        // Send achievements sync before opening
                        syncAchievements(player);
                        player.openMenu(new com.servermanagement.gui.economy.AchievementsMenuProvider());
                        break;
                    case ECONOMY_MANAGEMENT:
                        SyncEconomyTemplatesPacket.syncToPlayer(player, player.getServer());
                        SyncEconomyStatsPacket.syncToPlayer(player, player.getServer());
                        player.openMenu(new com.servermanagement.gui.economy.EconomyManagementMenuProvider());
                        break;
                    case MINEBAY:
                        // Available to all players
                        syncBankAccount(player); // Sync balance for price display
                        syncMineBayListings(player);
                        player.openMenu(new com.servermanagement.gui.minebay.MineBayMenuProvider());
                        break;
                    case MINESTACKS:
                        // Available to all players - gambling system
                        syncBankAccount(player); // Sync balance for display
                        syncGamblingStats(player); // Sync gambling statistics
                        com.servermanagement.gui.gambling.MineStacksMenuProvider.open(player);
                        break;
                    case PERFORMANCE_SETTINGS:
                        SyncPerformanceSettingsPacket.syncToPlayer(player);
                        player.openMenu(new com.servermanagement.gui.provider.PerformanceSettingsMenuProvider());
                        break;
                    case MOTD_EDITOR:
                        syncMotd(player);
                        player.openMenu(new com.servermanagement.gui.MotdEditorMenuProvider());
                        break;
                    case UPDATER:
                        player.openMenu(new com.servermanagement.gui.provider.UpdaterMenuProvider());
                        break;
                    case HUD_EDIT:
                        // Sent from server to client to open HUD edit screen (client-side only, no menu provider)
                        break;
                }
            } else {
                if (guiType == GuiType.HUD_EDIT) {
                    com.servermanagement.client.ClientScreenManager.openHudEditScreen();
                }
            }
}
    
    private void syncWorldList(ServerPlayer player) {
        var worldManager = com.servermanagement.features.worldmanager.WorldManager.getInstance();
        var worlds = worldManager.buildWorldListForClient(player.getServer());
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncWorldListPacket(worlds), player
        );
    }
    
    private void syncWorldDetail(ServerPlayer player, String dimensionId) {
        var worldManager = com.servermanagement.features.worldmanager.WorldManager.getInstance();
        var data = worldManager.getData();
        
        boolean netherPortalsEnabled = data.areNetherPortalsEnabled(dimensionId);
        boolean endPortalsEnabled = data.areEndPortalsEnabled(dimensionId);
        boolean hasTimer = data.hasActiveTimer(dimensionId);
        int timerSeconds = (int) data.getRemainingTime(dimensionId);
        boolean chatConnected = data.isDimensionChatConnected(dimensionId);
        String timerPortalType = data.getTimerPortalType(dimensionId);
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncWorldDetailPacket(dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType),
            player
        );
    }
    
    private void syncMotd(ServerPlayer player) {
        var motdManager = com.servermanagement.features.motd.MotdManager.getInstance();
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncMotdPacket(motdManager.getMotdText()),
            player
        );
    }

    private void syncGlobalSettings(ServerPlayer player) {
        var worldManager = com.servermanagement.features.worldmanager.WorldManager.getInstance();
        var data = worldManager.getData();
        
        boolean chatIsolation = data.isChatIsolationEnabled();
        boolean tabIsolation = data.isTabIsolationEnabled();
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncGlobalSettingsPacket(chatIsolation, tabIsolation),
            player
        );
    }
    
    private void syncFeatureStates(ServerPlayer player) {
        var featureStates = com.servermanagement.features.FeatureManager.getFeatureStates();
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncFeatureStatesPacket(featureStates),
            player
        );
    }
    
    private void syncBankAccount(ServerPlayer player) {
        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
        var account = economyManager.getOrCreateAccount(player.getUUID());
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncBankAccountPacket(
                account.getBalance(),
                account.getTransactions()
            ),
            player
        );
    }
    
    private void syncGamblingStats(ServerPlayer player) {
        var gamblingManager = com.servermanagement.features.gambling.GamblingManager.getInstance();
        var stats = gamblingManager.getStats(player.getUUID());
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncGamblingStatsPacket(
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
    }
    
    private void syncDailyTasks(ServerPlayer player) {
        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
        var dailyTasksManager = economyManager.getDailyTasksManager();
        var playerTasks = dailyTasksManager.getOrCreatePlayerTasks(player.getUUID());
        var templateManager = economyManager.getTemplateManager();
        
        // Use admin-configurable free reward amount from template manager
        int freeRewardAmount = templateManager != null
            ? (int) templateManager.getFreeRewardAmount()
            : playerTasks.getFreeRewardAmount();
        
        // Calculate reset time (current time + time until refresh)
        long resetTime = System.currentTimeMillis() + playerTasks.getTimeUntilTaskRefresh();
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncDailyTasksPacket(
                playerTasks.getTasks(),
                resetTime,
                playerTasks.isFreeRewardAvailable(),
                freeRewardAmount,
                playerTasks.getTimeUntilFreeReward()
            ),
            player
        );
    }
    
    private void syncAchievements(ServerPlayer player) {
        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
        var achievementTracker = economyManager.getAchievementTracker();
        var earnedAchievements = achievementTracker.getRewardedAchievements(player.getUUID());
        
        // Calculate total rewards (assuming average reward per achievement for now)
        // In a real implementation, you'd track actual reward amounts
        int totalRewards = earnedAchievements.size() * 100; // $100 average per achievement
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncAchievementsPacket(
                earnedAchievements,
                totalRewards
            ),
            player
        );
    }
    
    private void syncMineBayListings(ServerPlayer player) {
        var mineBayManager = com.servermanagement.features.minebay.MineBayManager.getInstance();
        var listings = mineBayManager.getActiveListings();
        
        // Send listings to client
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket(listings),
            player
        );
    }
    
    private void syncBankInventory(ServerPlayer player) {
        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance(player.server);
        var bankInventory = economyManager.getBankInventory(player.getUUID());
        
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncBankInventoryPacket(bankInventory),
            player
        );
    }

    private void syncMoneyRequests(ServerPlayer player) {
        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance(player.server);
        SendMoneyRequestPacket.syncRequestsToPlayer(player, economyManager);
    }

    public enum GuiType {
        CONFIG,
        DASHBOARD,
        GLOBAL_SETTINGS,
        WORLD_LIST,
        WORLD_DETAIL,
        PLAYER_MANAGER,
        CONSOLE,
        PORTAL_TIMER,
        BANK,
        DAILY_TASKS,
        ACHIEVEMENTS,
        ECONOMY_MANAGEMENT,
        MINEBAY,
        MINESTACKS,
        PERFORMANCE_SETTINGS,
        MOTD_EDITOR,
        UPDATER,
        HUD_EDIT;

        public boolean isAdminOnly() {
            return switch (this) {
                case BANK, DAILY_TASKS, ACHIEVEMENTS, MINEBAY, MINESTACKS, HUD_EDIT -> false;
                default -> true;
            };
        }
    }
}
