package com.servermanagement.gui;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(Registries.MENU, ServerManagementMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ConfigMenu>> CONFIG_MENU =
        MENUS.register("config_menu", () -> new MenuType<>(ConfigMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<GlobalSettingsMenu>> GLOBAL_SETTINGS_MENU =
        MENUS.register("global_settings_menu", () -> new MenuType<>(GlobalSettingsMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WorldListMenu>> WORLD_LIST_MENU =
        MENUS.register("world_list_menu", () -> new MenuType<>(WorldListMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<WorldDetailMenu>> WORLD_DETAIL_MENU =
        MENUS.register("world_detail_menu", () -> new MenuType<>(WorldDetailMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerManagerMenu>> PLAYER_MANAGER_MENU =
        MENUS.register("player_manager_menu", () -> new MenuType<>(PlayerManagerMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PortalTimerMenu>> PORTAL_TIMER_MENU =
        MENUS.register("portal_timer_menu", () -> new MenuType<>(PortalTimerMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ServerManagementMenu>> SERVER_MANAGEMENT_MENU =
        MENUS.register("server_management_menu", () -> new MenuType<>(ServerManagementMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<DashboardMenu>> DASHBOARD_MENU =
        MENUS.register("dashboard_menu", () -> new MenuType<>(DashboardMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<ConsoleMenu>> CONSOLE_MENU =
        MENUS.register("console_menu", () -> new MenuType<>(ConsoleMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.BankMenu>> BANK_MENU =
        MENUS.register("bank_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new com.servermanagement.gui.economy.BankMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.DailyTasksMenu>> DAILY_TASKS_MENU =
        MENUS.register("daily_tasks_menu", () -> new MenuType<>(com.servermanagement.gui.economy.DailyTasksMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.AchievementsMenu>> ACHIEVEMENTS_MENU =
        MENUS.register("achievements_menu", () -> new MenuType<>(com.servermanagement.gui.economy.AchievementsMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.economy.EconomyManagementMenu>> ECONOMY_MANAGEMENT_MENU =
        MENUS.register("economy_management_menu", () -> new MenuType<>(com.servermanagement.gui.economy.EconomyManagementMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.minebay.MineBayMenu>> MINEBAY_MENU =
        MENUS.register("minebay_menu", () -> new MenuType<>(com.servermanagement.gui.minebay.MineBayMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.servermanagement.gui.gambling.MineStacksMenu>> MINESTACKS_MENU =
        MENUS.register("minestacks_menu", () -> new MenuType<>(com.servermanagement.gui.gambling.MineStacksMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PerformanceSettingsMenu>> PERFORMANCE_SETTINGS_MENU =
        MENUS.register("performance_settings_menu", () -> new MenuType<>(PerformanceSettingsMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<MotdEditorMenu>> MOTD_EDITOR_MENU =
        MENUS.register("motd_editor_menu", () -> new MenuType<>(MotdEditorMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<UpdaterMenu>> UPDATER_MENU =
        MENUS.register("updater_menu", () -> new MenuType<>(UpdaterMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
