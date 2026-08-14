package com.servermanagement.client;

import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.gui.screen.*;
import com.servermanagement.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            com.servermanagement.gui.overlay.StatsBarOverlay.render(guiGraphics, tickDelta.getGameTimeDeltaTicks());
        });
        com.servermanagement.client.ClientConfig.init(net.minecraft.client.Minecraft.getInstance().gameDirectory);
        // Register client-side packet handlers
        ModNetworking.registerClientPackets();

        // Bug 4: Item-price tooltips never showed on Fabric because Forge wires
        // them via @SubscribeEvent on ItemTooltipEvent which has no auto-bus
        // equivalent on Fabric. Register the callback explicitly here.
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(
                (stack, tooltipContext, tooltipType, lines) ->
                        com.servermanagement.client.ClientItemTooltipHandler.onItemTooltip(stack, tooltipType, lines)
        );

        // Register menu screens
        MenuScreens.register(ModMenuTypes.CONFIG_MENU, ConfigScreen::new);
        MenuScreens.register(ModMenuTypes.GLOBAL_SETTINGS_MENU, GlobalSettingsScreen::new);
        MenuScreens.register(ModMenuTypes.WORLD_LIST_MENU, WorldListScreen::new);
        MenuScreens.register(ModMenuTypes.WORLD_DETAIL_MENU, WorldDetailScreen::new);
        MenuScreens.register(ModMenuTypes.PORTAL_TIMER_MENU, PortalTimerScreen::new);
        MenuScreens.register(ModMenuTypes.PLAYER_MANAGER_MENU, PlayerManagerScreen::new);
        MenuScreens.register(ModMenuTypes.SERVER_MANAGEMENT_MENU, ServerManagementScreen::new);
        MenuScreens.register(ModMenuTypes.DASHBOARD_MENU, DashboardScreen::new);
        MenuScreens.register(ModMenuTypes.CONSOLE_MENU, ConsoleScreen::new);
        MenuScreens.register(ModMenuTypes.BANK_MENU, com.servermanagement.gui.economy.BankScreen::new);
        MenuScreens.register(ModMenuTypes.DAILY_TASKS_MENU, com.servermanagement.gui.economy.DailyTasksScreen::new);
        MenuScreens.register(ModMenuTypes.ACHIEVEMENTS_MENU, com.servermanagement.gui.economy.AchievementsScreen::new);
        MenuScreens.register(ModMenuTypes.ECONOMY_MANAGEMENT_MENU, com.servermanagement.gui.economy.EconomyManagementScreen::new);
        MenuScreens.register(ModMenuTypes.MINEBAY_MENU, com.servermanagement.gui.minebay.MineBayScreen::new);
        MenuScreens.register(ModMenuTypes.MINESTACKS_MENU, com.servermanagement.gui.gambling.MineStacksScreen::new);
        MenuScreens.register(ModMenuTypes.PERFORMANCE_SETTINGS_MENU, PerformanceSettingsScreen::new);
        MenuScreens.register(ModMenuTypes.MOTD_EDITOR_MENU, MotdEditorScreen::new);
        MenuScreens.register(ModMenuTypes.UPDATER_MENU, UpdaterScreen::new);

        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen) {
                com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(scaledWidth, scaledHeight, false, () -> {
                    // Trigger manual update check with visual feedback
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    String ver = com.servermanagement.ServerManagementMod.getModVersion();
                    com.servermanagement.updater.UpdatePreferences.load();
                    ClientUpdateManager.checkForUpdates(mc, ver, titleScreen);
                });
                net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(btn);

                if (!updateChecked) {
                    updateChecked = true;
                    
                    String currentVersion = com.servermanagement.ServerManagementMod.getModVersion();
                    String lastRun = ClientConfig.getLastRunVersion();
                    if (lastRun.isEmpty()) {
                        ClientConfig.setLastRunVersion(currentVersion);
                    } else if (!lastRun.equals(currentVersion)) {
                        ClientConfig.setLastRunVersion(currentVersion);
                        client.execute(() -> {
                            client.setScreen(new com.servermanagement.gui.screen.ChangelogScreen(titleScreen));
                        });
                        return; // skip update check for this launch
                    }
                    
                    com.servermanagement.updater.UpdatePreferences.load();
                    String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
                    String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
                    com.servermanagement.updater.UpdateManager.checkAllUpdates(currentVersion, loader, mcVersion).thenAccept(result -> {
                        if (result == null || (result.modrinth() == null && result.curseforge() == null)) return;
                        com.servermanagement.updater.UpdateInfo target = result.resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
                        if (target != null) {
                            btn.setNotification(true);
                            if (!com.servermanagement.updater.UpdatePreferences.isSkipped(target.version())) {
                                client.execute(() -> {
                                    client.setScreen(new UpdateAvailableScreen(titleScreen, result, target, currentVersion));
                                });
                            }
                        }
                    });
                }
            } else if (screen instanceof net.minecraft.client.gui.screens.PauseScreen pauseScreen) {
                com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(scaledWidth, scaledHeight, false, () -> {
                    client.setScreen(new com.servermanagement.gui.screen.PerformanceSettingsScreen(
                        new com.servermanagement.gui.menu.PerformanceSettingsMenu(-1, client.player.getInventory()),
                        client.player.getInventory(),
                        net.minecraft.network.chat.Component.translatable("gui.servermanagement.performance_settings")
                    ));
                });
                net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(btn);
            }
        });
    }

    private static boolean updateChecked = false;
}
