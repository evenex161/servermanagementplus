package com.servermanagement.gui;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(Registries.MENU, ServerManagementMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ConfigMenu>> CONFIG_MENU =
        MENUS.register("config_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ConfigMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<GlobalSettingsMenu>> GLOBAL_SETTINGS_MENU =
        MENUS.register("global_settings_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new GlobalSettingsMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<WorldListMenu>> WORLD_LIST_MENU =
        MENUS.register("world_list_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new WorldListMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<WorldDetailMenu>> WORLD_DETAIL_MENU =
        MENUS.register("world_detail_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new WorldDetailMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerManagerMenu>> PLAYER_MANAGER_MENU =
        MENUS.register("player_manager_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new PlayerManagerMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<PortalTimerMenu>> PORTAL_TIMER_MENU =
        MENUS.register("portal_timer_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new PortalTimerMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<ServerManagementMenu>> SERVER_MANAGEMENT_MENU =
        MENUS.register("server_management_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ServerManagementMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<DashboardMenu>> DASHBOARD_MENU =
        MENUS.register("dashboard_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new DashboardMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<ConsoleMenu>> CONSOLE_MENU =
        MENUS.register("console_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ConsoleMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.BankMenu>> BANK_MENU =
        MENUS.register("bank_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.economy.BankMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.DailyTasksMenu>> DAILY_TASKS_MENU =
        MENUS.register("daily_tasks_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.economy.DailyTasksMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.AchievementsMenu>> ACHIEVEMENTS_MENU =
        MENUS.register("achievements_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.economy.AchievementsMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.EconomyManagementMenu>> ECONOMY_MANAGEMENT_MENU =
        MENUS.register("economy_management_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.economy.EconomyManagementMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.minebay.MineBayMenu>> MINEBAY_MENU =
        MENUS.register("minebay_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.minebay.MineBayMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.gambling.MineStacksMenu>> MINESTACKS_MENU =
        MENUS.register("minestacks_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.gambling.MineStacksMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<PerformanceSettingsMenu>> PERFORMANCE_SETTINGS_MENU =
        MENUS.register("performance_settings_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new PerformanceSettingsMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<MotdEditorMenu>> MOTD_EDITOR_MENU =
        MENUS.register("motd_editor_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new MotdEditorMenu(windowId, inv)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
