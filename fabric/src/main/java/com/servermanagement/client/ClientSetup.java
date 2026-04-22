package com.servermanagement.client;

import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.gui.screen.*;
import com.servermanagement.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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
    }
}
