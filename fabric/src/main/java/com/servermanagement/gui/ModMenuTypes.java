package com.servermanagement.gui;

import com.servermanagement.ServerManagementModFabric;
import com.servermanagement.gui.menu.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static MenuType<ConfigMenu> CONFIG_MENU;
    public static MenuType<GlobalSettingsMenu> GLOBAL_SETTINGS_MENU;
    public static MenuType<WorldListMenu> WORLD_LIST_MENU;
    public static MenuType<WorldDetailMenu> WORLD_DETAIL_MENU;
    public static MenuType<PlayerManagerMenu> PLAYER_MANAGER_MENU;
    public static MenuType<PortalTimerMenu> PORTAL_TIMER_MENU;
    public static MenuType<ServerManagementMenu> SERVER_MANAGEMENT_MENU;
    public static MenuType<DashboardMenu> DASHBOARD_MENU;
    public static MenuType<ConsoleMenu> CONSOLE_MENU;
    public static MenuType<com.servermanagement.gui.economy.BankMenu> BANK_MENU;
    public static MenuType<com.servermanagement.gui.economy.DailyTasksMenu> DAILY_TASKS_MENU;
    public static MenuType<com.servermanagement.gui.economy.AchievementsMenu> ACHIEVEMENTS_MENU;
    public static MenuType<com.servermanagement.gui.economy.EconomyManagementMenu> ECONOMY_MANAGEMENT_MENU;
    public static MenuType<com.servermanagement.gui.minebay.MineBayMenu> MINEBAY_MENU;
    public static MenuType<com.servermanagement.gui.gambling.MineStacksMenu> MINESTACKS_MENU;
    public static MenuType<PerformanceSettingsMenu> PERFORMANCE_SETTINGS_MENU;
    public static MenuType<MotdEditorMenu> MOTD_EDITOR_MENU;

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> registerSimple(String name, SimpleMenuFactory<T> factory) {
        MenuType<T> type = new MenuType<>(factory::create, net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        return Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(ServerManagementModFabric.MOD_ID, name), type);
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> registerExtended(String name, ExtendedMenuFactory<T> factory) {
        ExtendedScreenHandlerType<T, net.minecraft.network.RegistryFriendlyByteBuf> type = 
            new ExtendedScreenHandlerType<>(factory::create, net.minecraft.network.codec.StreamCodec.of(
                (buf, data) -> {}, buf -> buf
            ));
        return Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(ServerManagementModFabric.MOD_ID, name), type);
    }

    @FunctionalInterface
    private interface SimpleMenuFactory<T> {
        T create(int windowId, net.minecraft.world.entity.player.Inventory inv);
    }

    @FunctionalInterface
    private interface ExtendedMenuFactory<T> {
        T create(int windowId, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf data);
    }

    public static void register() {
        CONFIG_MENU = registerSimple("config_menu", ConfigMenu::new);
        GLOBAL_SETTINGS_MENU = registerSimple("global_settings_menu", GlobalSettingsMenu::new);
        WORLD_LIST_MENU = registerSimple("world_list_menu", WorldListMenu::new);
        WORLD_DETAIL_MENU = registerSimple("world_detail_menu", WorldDetailMenu::new);
        PLAYER_MANAGER_MENU = registerSimple("player_manager_menu", PlayerManagerMenu::new);
        PORTAL_TIMER_MENU = registerSimple("portal_timer_menu", PortalTimerMenu::new);
        SERVER_MANAGEMENT_MENU = registerSimple("server_management_menu", ServerManagementMenu::new);
        DASHBOARD_MENU = registerSimple("dashboard_menu", DashboardMenu::new);
        CONSOLE_MENU = registerSimple("console_menu", ConsoleMenu::new);
        BANK_MENU = registerSimple("bank_menu", com.servermanagement.gui.economy.BankMenu::new);
        DAILY_TASKS_MENU = registerSimple("daily_tasks_menu", com.servermanagement.gui.economy.DailyTasksMenu::new);
        ACHIEVEMENTS_MENU = registerSimple("achievements_menu", com.servermanagement.gui.economy.AchievementsMenu::new);
        ECONOMY_MANAGEMENT_MENU = registerSimple("economy_management_menu", com.servermanagement.gui.economy.EconomyManagementMenu::new);
        MINEBAY_MENU = registerSimple("minebay_menu", com.servermanagement.gui.minebay.MineBayMenu::new);
        MINESTACKS_MENU = registerSimple("minestacks_menu", com.servermanagement.gui.gambling.MineStacksMenu::new);
        PERFORMANCE_SETTINGS_MENU = registerSimple("performance_settings_menu", PerformanceSettingsMenu::new);
        MOTD_EDITOR_MENU = registerSimple("motd_editor_menu", MotdEditorMenu::new);
    }
}
