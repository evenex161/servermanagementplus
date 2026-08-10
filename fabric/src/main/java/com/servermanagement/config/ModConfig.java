package com.servermanagement.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric-compatible config system that mimics ForgeConfigSpec's API surface.
 * Stores config in JSON format in the Fabric config directory.
 */
public class ModConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject configData = new JsonObject();
    private static Path configPath;
    private static final List<ConfigValue<?>> ALL_VALUES = new ArrayList<>();

    // Config version for auto-migration
    public static final int CURRENT_CONFIG_VERSION = 1;
    public static final IntValue CONFIG_VERSION;

    // World Manager Settings
    public static final BooleanValue WORLD_MANAGER_ENABLED;
    public static final BooleanValue NETHER_PORTALS_ENABLED;
    public static final BooleanValue END_PORTALS_ENABLED;
    public static final BooleanValue WORLD_TIMERS_ENABLED;
    public static final BooleanValue CHAT_ISOLATION_ENABLED;
    public static final BooleanValue TAB_ISOLATION_ENABLED;
    public static final IntValue TELEPORT_COOLDOWN;

    // Player Manager Settings
    public static final BooleanValue PLAYER_MANAGER_ENABLED;
    public static final BooleanValue SPECTATE_ENABLED;
    public static final BooleanValue VIEW_INVENTORY_ENABLED;
    public static final BooleanValue SLIME_HEADS_ENABLED;

    // Economy Settings
    public static final BooleanValue ECONOMY_ENABLED;
    public static final DoubleValue STARTING_BALANCE;
    public static final BooleanValue ENCRYPTED_STORAGE;
    public static final BooleanValue SHOW_MARKET_VALUE_TOOLTIPS;
    public static final BooleanValue MINEBAY_ENABLED;
    public static final BooleanValue MINESTACKS_ENABLED;
    public static final StringValue TRADE_BLACKLIST;
    public static final IntValue TRANSACTION_HISTORY_LIMIT;
    public static final IntValue MAX_LISTINGS_PER_PLAYER;

    // Server Performance Settings
    public static final BooleanValue SERVER_PERFORMANCE_ENABLED;

    // MOTD Settings
    public static final BooleanValue MOTD_ENABLED;
    public static final BooleanValue ITEM_MERGING_ENABLED;
    public static final DoubleValue ITEM_MERGE_RADIUS;
    public static final IntValue ITEM_MERGE_INTERVAL;
    public static final BooleanValue MOB_SPAWN_LIMITER_ENABLED;
    public static final IntValue MOB_CAP_MULTIPLIER;
    public static final BooleanValue ENTITY_ACTIVATION_RANGE_ENABLED;
    public static final IntValue MONSTER_ACTIVATION_RANGE;
    public static final IntValue ANIMAL_ACTIVATION_RANGE;
    public static final IntValue MISC_ACTIVATION_RANGE;
    public static final BooleanValue VILLAGER_THROTTLE_ENABLED;
    public static final IntValue VILLAGER_TICK_INTERVAL;
    public static final BooleanValue REDSTONE_THROTTLE_ENABLED;
    public static final IntValue REDSTONE_UPDATES_PER_TICK;
    public static final BooleanValue TPS_MONITOR_ENABLED;
    public static final DoubleValue TPS_WARNING_THRESHOLD;
    public static final DoubleValue TPS_CRITICAL_THRESHOLD;
    public static final BooleanValue TPS_AUTO_OPTIMIZE;

    // Stub SPEC field for compatibility with ConfigMigration/ConfigValidator
    public static final Object SPEC = new Object();

    static {
        CONFIG_VERSION = new IntValue("meta.configVersion", CURRENT_CONFIG_VERSION, 0, Integer.MAX_VALUE);

        WORLD_MANAGER_ENABLED = new BooleanValue("worldManager.worldManagerEnabled", true);
        NETHER_PORTALS_ENABLED = new BooleanValue("worldManager.netherPortalsEnabled", true);
        END_PORTALS_ENABLED = new BooleanValue("worldManager.endPortalsEnabled", true);
        WORLD_TIMERS_ENABLED = new BooleanValue("worldManager.worldTimersEnabled", true);
        CHAT_ISOLATION_ENABLED = new BooleanValue("worldManager.chatIsolationEnabled", true);
        TAB_ISOLATION_ENABLED = new BooleanValue("worldManager.tabIsolationEnabled", true);
        TELEPORT_COOLDOWN = new IntValue("worldManager.teleportCooldown", 5, 0, 300);

        PLAYER_MANAGER_ENABLED = new BooleanValue("playerManager.playerManagerEnabled", true);
        SPECTATE_ENABLED = new BooleanValue("playerManager.spectateEnabled", true);
        VIEW_INVENTORY_ENABLED = new BooleanValue("playerManager.viewInventoryEnabled", true);
        SLIME_HEADS_ENABLED = new BooleanValue("playerManager.slimeHeadsEnabled", true);

        ECONOMY_ENABLED = new BooleanValue("economy.economyEnabled", true);
        STARTING_BALANCE = new DoubleValue("economy.startingBalance", 1000.0, 0.0, 1000000.0);
        ENCRYPTED_STORAGE = new BooleanValue("economy.encryptedStorage", true);
        SHOW_MARKET_VALUE_TOOLTIPS = new BooleanValue("economy.showMarketValueTooltips", true);
        MINEBAY_ENABLED = new BooleanValue("economy.minebayEnabled", true);
        MINESTACKS_ENABLED = new BooleanValue("economy.minestacksEnabled", true);
        TRADE_BLACKLIST = new StringValue("economy.tradeBlacklist", "");
        TRANSACTION_HISTORY_LIMIT = new IntValue("economy.transactionHistoryLimit", 100, 10, 10000);
        MAX_LISTINGS_PER_PLAYER = new IntValue("economy.maxListingsPerPlayer", 10, 1, 100);

        SERVER_PERFORMANCE_ENABLED = new BooleanValue("serverPerformance.serverPerformanceEnabled", true);

        ITEM_MERGING_ENABLED = new BooleanValue("serverPerformance.itemMergingEnabled", true);
        ITEM_MERGE_RADIUS = new DoubleValue("serverPerformance.itemMergeRadius", 3.0, 1.0, 10.0);
        ITEM_MERGE_INTERVAL = new IntValue("serverPerformance.itemMergeInterval", 40, 10, 200);
        MOB_SPAWN_LIMITER_ENABLED = new BooleanValue("serverPerformance.mobSpawnLimiterEnabled", false);
        MOB_CAP_MULTIPLIER = new IntValue("serverPerformance.mobCapMultiplier", 75, 10, 100);
        ENTITY_ACTIVATION_RANGE_ENABLED = new BooleanValue("serverPerformance.entityActivationRangeEnabled", false);
        MONSTER_ACTIVATION_RANGE = new IntValue("serverPerformance.monsterActivationRange", 32, 8, 128);
        ANIMAL_ACTIVATION_RANGE = new IntValue("serverPerformance.animalActivationRange", 16, 8, 128);
        MISC_ACTIVATION_RANGE = new IntValue("serverPerformance.miscActivationRange", 8, 4, 64);
        VILLAGER_THROTTLE_ENABLED = new BooleanValue("serverPerformance.villagerThrottleEnabled", false);
        VILLAGER_TICK_INTERVAL = new IntValue("serverPerformance.villagerTickInterval", 3, 1, 10);
        REDSTONE_THROTTLE_ENABLED = new BooleanValue("serverPerformance.redstoneThrottleEnabled", false);
        REDSTONE_UPDATES_PER_TICK = new IntValue("serverPerformance.redstoneUpdatesPerTick", 1000, 100, 100000);
        TPS_MONITOR_ENABLED = new BooleanValue("serverPerformance.tpsMonitorEnabled", true);
        TPS_WARNING_THRESHOLD = new DoubleValue("serverPerformance.tpsWarningThreshold", 18.0, 5.0, 20.0);
        TPS_CRITICAL_THRESHOLD = new DoubleValue("serverPerformance.tpsCriticalThreshold", 15.0, 5.0, 20.0);
        TPS_AUTO_OPTIMIZE = new BooleanValue("serverPerformance.tpsAutoOptimize", false);

        MOTD_ENABLED = new BooleanValue("motd.motdEnabled", true);

        loadConfig();
    }

    private static void loadConfig() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("servermanagement.json");
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                configData = GSON.fromJson(json, JsonObject.class);
                if (configData == null) configData = new JsonObject();
                for (ConfigValue<?> value : ALL_VALUES) {
                    value.loadFrom(configData);
                }
                LOGGER.info("Loaded config from {}", configPath);
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                configData = new JsonObject();
            }
        } else {
            LOGGER.info("No config file found, creating with defaults");
            saveConfig();
        }
    }

    public static void saveConfig() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("servermanagement.json");
        }
        try {
            JsonObject obj = new JsonObject();
            for (ConfigValue<?> value : ALL_VALUES) {
                value.saveTo(obj);
            }
            Files.writeString(configPath, GSON.toJson(obj));
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    // Instance methods for easier access
    public boolean isWorldManagerEnabled() { return WORLD_MANAGER_ENABLED.get(); }
    public boolean areNetherPortalsEnabled() { return NETHER_PORTALS_ENABLED.get(); }
    public boolean areEndPortalsEnabled() { return END_PORTALS_ENABLED.get(); }
    public boolean areWorldTimersEnabled() { return WORLD_TIMERS_ENABLED.get(); }
    public boolean isChatIsolationEnabled() { return CHAT_ISOLATION_ENABLED.get(); }
    public boolean isTabIsolationEnabled() { return TAB_ISOLATION_ENABLED.get(); }
    public boolean isPlayerManagerEnabled() { return PLAYER_MANAGER_ENABLED.get(); }
    public boolean isSpectateEnabled() { return SPECTATE_ENABLED.get(); }
    public boolean isViewInventoryEnabled() { return VIEW_INVENTORY_ENABLED.get(); }
    public boolean areSlimeHeadsEnabled() { return SLIME_HEADS_ENABLED.get(); }
    public double getStartingBalance() { return STARTING_BALANCE.get(); }
    public boolean isEncryptedStorage() { return ENCRYPTED_STORAGE.get(); }
    public int getTransactionHistoryLimit() { return TRANSACTION_HISTORY_LIMIT.get(); }
    public int getMaxListingsPerPlayer() { return MAX_LISTINGS_PER_PLAYER.get(); }

    // ============ Config Value Types ============

    public static abstract class ConfigValue<T> {
        protected final String key;
        protected T value;
        protected final T defaultValue;

        protected ConfigValue(String key, T defaultValue) {
            this.key = key;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            ALL_VALUES.add(this);
        }

        public T get() { return value; }
        public void set(T value) {
            this.value = value;
            saveConfig();
        }

        abstract void loadFrom(JsonObject obj);
        abstract void saveTo(JsonObject obj);
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        public BooleanValue(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override void loadFrom(JsonObject obj) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                value = obj.get(key).getAsBoolean();
            }
        }

        @Override void saveTo(JsonObject obj) {
            obj.add(key, new JsonPrimitive(value));
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        private final int min, max;

        public IntValue(String key, int defaultValue, int min, int max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override public void set(Integer value) {
            super.set(Math.max(min, Math.min(max, value)));
        }

        @Override void loadFrom(JsonObject obj) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                value = Math.max(min, Math.min(max, obj.get(key).getAsInt()));
            }
        }

        @Override void saveTo(JsonObject obj) {
            obj.add(key, new JsonPrimitive(value));
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        private final double min, max;

        public DoubleValue(String key, double defaultValue, double min, double max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override public void set(Double value) {
            super.set(Math.max(min, Math.min(max, value)));
        }

        @Override void loadFrom(JsonObject obj) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                value = Math.max(min, Math.min(max, obj.get(key).getAsDouble()));
            }
        }

        @Override void saveTo(JsonObject obj) {
            obj.add(key, new JsonPrimitive(value));
        }
    }

    public static class StringValue extends ConfigValue<String> {
        public StringValue(String key, String defaultValue) {
            super(key, defaultValue);
        }

        @Override void loadFrom(JsonObject obj) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                value = obj.get(key).getAsString();
            }
        }

        @Override void saveTo(JsonObject obj) {
            obj.add(key, new JsonPrimitive(value));
        }
    }
}
