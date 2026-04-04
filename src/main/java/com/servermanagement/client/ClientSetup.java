package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.ModMenuTypes;
import com.servermanagement.gui.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register screens
            MenuScreens.register(ModMenuTypes.CONFIG_MENU.get(), ConfigScreen::new);
            MenuScreens.register(ModMenuTypes.GLOBAL_SETTINGS_MENU.get(), GlobalSettingsScreen::new);
            MenuScreens.register(ModMenuTypes.WORLD_LIST_MENU.get(), WorldListScreen::new);
            MenuScreens.register(ModMenuTypes.WORLD_DETAIL_MENU.get(), WorldDetailScreen::new);
            MenuScreens.register(ModMenuTypes.PORTAL_TIMER_MENU.get(), PortalTimerScreen::new);
            MenuScreens.register(ModMenuTypes.PLAYER_MANAGER_MENU.get(), PlayerManagerScreen::new);
            MenuScreens.register(ModMenuTypes.SERVER_MANAGEMENT_MENU.get(), ServerManagementScreen::new);
            MenuScreens.register(ModMenuTypes.DASHBOARD_MENU.get(), DashboardScreen::new);
            MenuScreens.register(ModMenuTypes.CONSOLE_MENU.get(), ConsoleScreen::new);
            MenuScreens.register(ModMenuTypes.BANK_MENU.get(), com.servermanagement.gui.economy.BankScreen::new);
            MenuScreens.register(ModMenuTypes.DAILY_TASKS_MENU.get(), com.servermanagement.gui.economy.DailyTasksScreen::new);
            MenuScreens.register(ModMenuTypes.ACHIEVEMENTS_MENU.get(), com.servermanagement.gui.economy.AchievementsScreen::new);
            MenuScreens.register(ModMenuTypes.ECONOMY_MANAGEMENT_MENU.get(), com.servermanagement.gui.economy.EconomyManagementScreen::new);
            MenuScreens.register(ModMenuTypes.MINEBAY_MENU.get(), com.servermanagement.gui.minebay.MineBayScreen::new);
            MenuScreens.register(ModMenuTypes.MINESTACKS_MENU.get(), com.servermanagement.gui.gambling.MineStacksScreen::new);
            
            ServerManagementMod.LOGGER.info("Registered GUI screens");
        });
    }
}
