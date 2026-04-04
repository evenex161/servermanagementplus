package com.servermanagement.gui;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.menu.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, ServerManagementMod.MOD_ID);

    public static final RegistryObject<MenuType<ConfigMenu>> CONFIG_MENU =
        MENUS.register("config_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new ConfigMenu(windowId, inv)));

    public static final RegistryObject<MenuType<GlobalSettingsMenu>> GLOBAL_SETTINGS_MENU =
        MENUS.register("global_settings_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new GlobalSettingsMenu(windowId, inv)));

    public static final RegistryObject<MenuType<WorldListMenu>> WORLD_LIST_MENU =
        MENUS.register("world_list_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new WorldListMenu(windowId, inv)));

    public static final RegistryObject<MenuType<WorldDetailMenu>> WORLD_DETAIL_MENU =
        MENUS.register("world_detail_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new WorldDetailMenu(windowId, inv)));

    public static final RegistryObject<MenuType<PlayerManagerMenu>> PLAYER_MANAGER_MENU =
        MENUS.register("player_manager_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new PlayerManagerMenu(windowId, inv)));

    public static final RegistryObject<MenuType<PortalTimerMenu>> PORTAL_TIMER_MENU =
        MENUS.register("portal_timer_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new PortalTimerMenu(windowId, inv)));

    public static final RegistryObject<MenuType<ServerManagementMenu>> SERVER_MANAGEMENT_MENU =
        MENUS.register("server_management_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new ServerManagementMenu(windowId, inv)));

    public static final RegistryObject<MenuType<DashboardMenu>> DASHBOARD_MENU =
        MENUS.register("dashboard_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new DashboardMenu(windowId, inv)));

    public static final RegistryObject<MenuType<ConsoleMenu>> CONSOLE_MENU =
        MENUS.register("console_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new ConsoleMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.economy.BankMenu>> BANK_MENU =
        MENUS.register("bank_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.economy.BankMenu(windowId, inv, data)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.economy.DailyTasksMenu>> DAILY_TASKS_MENU =
        MENUS.register("daily_tasks_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.economy.DailyTasksMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.economy.AchievementsMenu>> ACHIEVEMENTS_MENU =
        MENUS.register("achievements_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.economy.AchievementsMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.economy.EconomyManagementMenu>> ECONOMY_MANAGEMENT_MENU =
        MENUS.register("economy_management_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.economy.EconomyManagementMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.minebay.MineBayMenu>> MINEBAY_MENU =
        MENUS.register("minebay_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.minebay.MineBayMenu(windowId, inv)));

    public static final RegistryObject<MenuType<com.servermanagement.gui.gambling.MineStacksMenu>> MINESTACKS_MENU =
        MENUS.register("minestacks_menu", () -> IForgeMenuType.create((windowId, inv, data) -> new com.servermanagement.gui.gambling.MineStacksMenu(windowId, inv)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
