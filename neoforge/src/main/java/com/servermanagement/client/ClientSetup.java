package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.gui.screen.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "stats_bar"), (guiGraphics, partialTick) -> {
            com.servermanagement.gui.overlay.StatsBarOverlay.render(guiGraphics, partialTick.getGameTimeDeltaTicks());
        });
    }

    
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CONFIG_MENU.get(), ConfigScreen::new);
        event.register(ModMenuTypes.GLOBAL_SETTINGS_MENU.get(), GlobalSettingsScreen::new);
        event.register(ModMenuTypes.WORLD_LIST_MENU.get(), WorldListScreen::new);
        event.register(ModMenuTypes.WORLD_DETAIL_MENU.get(), WorldDetailScreen::new);
        event.register(ModMenuTypes.PORTAL_TIMER_MENU.get(), PortalTimerScreen::new);
        event.register(ModMenuTypes.PLAYER_MANAGER_MENU.get(), PlayerManagerScreen::new);
        event.register(ModMenuTypes.SERVER_MANAGEMENT_MENU.get(), ServerManagementScreen::new);
        event.register(ModMenuTypes.DASHBOARD_MENU.get(), DashboardScreen::new);
        event.register(ModMenuTypes.CONSOLE_MENU.get(), ConsoleScreen::new);
        event.register(ModMenuTypes.BANK_MENU.get(), com.servermanagement.gui.economy.BankScreen::new);
        event.register(ModMenuTypes.DAILY_TASKS_MENU.get(), com.servermanagement.gui.economy.DailyTasksScreen::new);
        event.register(ModMenuTypes.ACHIEVEMENTS_MENU.get(), com.servermanagement.gui.economy.AchievementsScreen::new);
        event.register(ModMenuTypes.ECONOMY_MANAGEMENT_MENU.get(), com.servermanagement.gui.economy.EconomyManagementScreen::new);
        event.register(ModMenuTypes.MINEBAY_MENU.get(), com.servermanagement.gui.minebay.MineBayScreen::new);
        event.register(ModMenuTypes.MINESTACKS_MENU.get(), com.servermanagement.gui.gambling.MineStacksScreen::new);
        event.register(ModMenuTypes.PERFORMANCE_SETTINGS_MENU.get(), PerformanceSettingsScreen::new);
        event.register(ModMenuTypes.MOTD_EDITOR_MENU.get(), MotdEditorScreen::new);
        event.register(ModMenuTypes.UPDATER_MENU.get(), UpdaterScreen::new);
        
        ServerManagementMod.LOGGER.info("Registered GUI screens");
    }
}