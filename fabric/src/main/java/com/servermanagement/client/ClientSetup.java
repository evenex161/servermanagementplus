package com.servermanagement.client;

import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.gui.screen.*;
import com.servermanagement.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import com.servermanagement.Constants;

public class ClientSetup implements ClientModInitializer {
    private static boolean updateChecked = false;
    private static UpdateInfo pendingUpdate = null;

    @Override
    public void onInitializeClient() {
        // Register client-side packet handlers
        ModNetworking.registerClientPackets();

        // Bug 4: Item-price tooltips never showed on Fabric because Forge wires
        // them via @SubscribeEvent on ItemTooltipEvent which has no auto-bus
        // equivalent on Fabric. Register the callback explicitly here.
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(
                (stack, tooltipType, lines) ->
                        com.servermanagement.client.ClientItemTooltipHandler.onItemTooltip(stack, tooltipType, lines)
        );

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            UpdatePreferences.load();
            UpdateManager.checkForUpdates("2.1.2-b1", "fabric", "1.20.1")
                .thenAccept(optInfo -> optInfo.ifPresent(info -> {
                    if (!UpdatePreferences.isSkipped(info.version())) {
                        pendingUpdate = info;
                    }
                }));
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraftClient) -> {
            if (com.servermanagement.features.serverperformance.GCAdvisor.isUsingSuboptimalGC() && !com.servermanagement.features.serverperformance.GCAdvisor.isDismissed()) {
                minecraftClient.tell(() -> {
                    if (minecraftClient.player != null) {
                        net.minecraft.network.chat.MutableComponent msg = net.minecraft.network.chat.Component.literal("§c" + Constants.CHAT_PREFIX + " Warning: Suboptimal Client JVM GC detected! (" + com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC().getDisplayName() + ") ")
                            .append(net.minecraft.network.chat.Component.literal("§e[Copy Optimal Flags]")
                                .withStyle(style -> style
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, com.servermanagement.features.serverperformance.GCAdvisor.getRecommendedFlags()))
                                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Copy flags to clipboard for your Launcher")))
                                ))
                            .append(" ")
                            .append(net.minecraft.network.chat.Component.literal("§7[Dismiss]")
                                .withStyle(style -> style
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm gc dismiss"))
                                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Dismiss this warning")))
                                ));
                        minecraftClient.player.sendSystemMessage(msg);
                    }
                });
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(scaledWidth, scaledHeight, false, () -> {
                    // Trigger manual update check with visual feedback
                    String ver = "2.1.2-b1";
                    com.servermanagement.updater.UpdatePreferences.load();
                    String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
                    String mcVer = net.minecraft.SharedConstants.getCurrentVersion().getName();
                    com.servermanagement.updater.UpdateManager.checkForUpdates(ver, loader, mcVer).thenAccept(opt -> {
                        if (opt.isPresent()) {
                            client.execute(() -> client.setScreen(new com.servermanagement.client.UpdateAvailableScreen(screen, opt.get(), ver)));
                        }
                    });
                });
                net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(btn);

                if (pendingUpdate != null && !updateChecked) {
                    updateChecked = true;
                    UpdateInfo info = pendingUpdate;
                    pendingUpdate = null;
                    client.tell(() -> {
                        client.setScreen(new com.servermanagement.client.UpdateAvailableScreen(screen, info, "2.1.2-b1"));
                    });
                }
            } else if (screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
                if (client.player != null && client.player.hasPermissions(2)) {
                    com.servermanagement.gui.widgets.FloatingLogoButton btn = new com.servermanagement.gui.widgets.FloatingLogoButton(scaledWidth, scaledHeight, false, () -> {
                        client.setScreen(new com.servermanagement.gui.screen.PerformanceSettingsScreen(
                            new com.servermanagement.gui.menu.PerformanceSettingsMenu(-1, client.player.getInventory()),
                            client.player.getInventory(),
                            net.minecraft.network.chat.Component.translatable("gui.servermanagement.performance_settings")
                        ));
                    });
                    net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(btn);
                }
            }
        });

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
        MenuScreens.register(com.servermanagement.gui.ModMenuTypes.ACHIEVEMENTS_MENU, com.servermanagement.gui.economy.AchievementsScreen::new);
        MenuScreens.register(com.servermanagement.gui.ModMenuTypes.ECONOMY_MANAGEMENT_MENU, com.servermanagement.gui.economy.EconomyManagementScreen::new);
        MenuScreens.register(com.servermanagement.gui.ModMenuTypes.MINEBAY_MENU, com.servermanagement.gui.minebay.MineBayScreen::new);
        MenuScreens.register(com.servermanagement.gui.ModMenuTypes.UPDATER_MENU, com.servermanagement.gui.screen.UpdaterScreen::new);
        MenuScreens.register(ModMenuTypes.MINESTACKS_MENU, com.servermanagement.gui.gambling.MineStacksScreen::new);
        MenuScreens.register(ModMenuTypes.PERFORMANCE_SETTINGS_MENU, PerformanceSettingsScreen::new);
        MenuScreens.register(ModMenuTypes.MOTD_EDITOR_MENU, MotdEditorScreen::new);
    }
}
