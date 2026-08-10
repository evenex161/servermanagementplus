package com.servermanagement.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
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
    public static final IntValue TRANSACTION_HISTORY_LIMIT;
    public static final IntValue MAX_LISTINGS_PER_PLAYER;

    // Server Performance Settings
        public static ForgeConfigSpec.BooleanValue SHOW_MARKET_VALUE_TOOLTIPS;
    public static ForgeConfigSpec.BooleanValue MINEBAY_ENABLED;
    public static ForgeConfigSpec.BooleanValue MINESTACKS_ENABLED;
    public static ForgeConfigSpec.ConfigValue<String> TRADE_BLACKLIST;

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

    static {
        BUILDER.push("Meta");
        CONFIG_VERSION = BUILDER
            .comment("Config version for auto-migration. DO NOT EDIT MANUALLY!")
            .defineInRange("configVersion", CURRENT_CONFIG_VERSION, 0, Integer.MAX_VALUE);
        BUILDER.pop();
        
        BUILDER.push("World Manager");
        WORLD_MANAGER_ENABLED = BUILDER
            .comment("Enable World Manager features")
            .define("worldManagerEnabled", true);
        NETHER_PORTALS_ENABLED = BUILDER
            .comment("Enable Nether portal toggle")
            .define("netherPortalsEnabled", true);
        END_PORTALS_ENABLED = BUILDER
            .comment("Enable End portal toggle")
            .define("endPortalsEnabled", true);
        WORLD_TIMERS_ENABLED = BUILDER
            .comment("Enable world timers")
            .define("worldTimersEnabled", true);
        CHAT_ISOLATION_ENABLED = BUILDER
            .comment("Enable chat isolation per world")
            .define("chatIsolationEnabled", true);
        TAB_ISOLATION_ENABLED = BUILDER
            .comment("Enable tab list isolation per world")
            .define("tabIsolationEnabled", true);
        TELEPORT_COOLDOWN = BUILDER
            .comment("Cooldown in seconds between world teleports")
            .defineInRange("teleportCooldown", 5, 0, 300);
        BUILDER.pop();

        BUILDER.push("Player Manager");
        PLAYER_MANAGER_ENABLED = BUILDER
            .comment("Enable Player Manager features")
            .define("playerManagerEnabled", true);
        SPECTATE_ENABLED = BUILDER
            .comment("Enable spectate command")
            .define("spectateEnabled", true);
        VIEW_INVENTORY_ENABLED = BUILDER
            .comment("Enable view inventory command")
            .define("viewInventoryEnabled", true);
        SLIME_HEADS_ENABLED = BUILDER
            .comment("Enable slime heads feature")
            .define("slimeHeadsEnabled", true);
        BUILDER.pop();

        BUILDER.push("Economy");
        ECONOMY_ENABLED = BUILDER
            .comment("Enable the Economy system (Bank, MineBay, MineStacks)")
            .define("economyEnabled", true);
        STARTING_BALANCE = BUILDER
            .comment("Starting balance for new players")
            .defineInRange("startingBalance", 1000.0, 0.0, 1000000.0);
        ENCRYPTED_STORAGE = BUILDER
                        .comment("Enable AES-256-GCM encryption for economy data")
            .define("encryptedStorage", true);
            
        SHOW_MARKET_VALUE_TOOLTIPS = BUILDER
            .comment("Show market value and stack value in item tooltips")
            .define("showMarketValueTooltips", true);
            
        MINEBAY_ENABLED = BUILDER
            .comment("Enable the MineBay player market system")
            .define("minebayEnabled", true);
            
        MINESTACKS_ENABLED = BUILDER
            .comment("Enable the MineStacks gambling system")
            .define("minestacksEnabled", true);
            
        TRADE_BLACKLIST = BUILDER
            .comment("Comma-separated list of item IDs that cannot be traded on MineBay")
            .define("tradeBlacklist", "");
        TRANSACTION_HISTORY_LIMIT = BUILDER
            .comment("Maximum number of transactions stored per player")
            .defineInRange("transactionHistoryLimit", 100, 10, 10000);
        MAX_LISTINGS_PER_PLAYER = BUILDER
            .comment("Maximum number of active MineBay listings per player")
            .defineInRange("maxListingsPerPlayer", 10, 1, 100);
        BUILDER.pop();

        BUILDER.push("Server Performance");
        SERVER_PERFORMANCE_ENABLED = BUILDER
            .comment("Enable the Server Performance optimization system")
            .define("serverPerformanceEnabled", true);

        BUILDER.push("Item Merging");
        ITEM_MERGING_ENABLED = BUILDER
            .comment("Merge nearby dropped item entities of the same type to reduce entity count")
            .define("itemMergingEnabled", true);
        ITEM_MERGE_RADIUS = BUILDER
            .comment("Radius in blocks to search for mergeable items (default: 3.0)")
            .defineInRange("itemMergeRadius", 3.0, 1.0, 10.0);
        ITEM_MERGE_INTERVAL = BUILDER
            .comment("How often to run item merging, in ticks (20 = once per second)")
            .defineInRange("itemMergeInterval", 40, 10, 200);
        BUILDER.pop();

        BUILDER.push("Mob Spawn Limiter");
        MOB_SPAWN_LIMITER_ENABLED = BUILDER
            .comment("Enable mob spawn rate limiting to reduce entity count")
            .define("mobSpawnLimiterEnabled", false);
        MOB_CAP_MULTIPLIER = BUILDER
            .comment("Mob cap multiplier as percentage of vanilla (100 = vanilla, 50 = half, 25 = quarter)")
            .defineInRange("mobCapMultiplier", 75, 10, 100);
        BUILDER.pop();

        BUILDER.push("Entity Activation Range");
        ENTITY_ACTIVATION_RANGE_ENABLED = BUILDER
            .comment("Only fully tick entities within a certain range of players. Distant entities tick at reduced frequency.")
            .define("entityActivationRangeEnabled", false);
        MONSTER_ACTIVATION_RANGE = BUILDER
            .comment("Range in blocks for full monster AI ticking (default: 32)")
            .defineInRange("monsterActivationRange", 32, 8, 128);
        ANIMAL_ACTIVATION_RANGE = BUILDER
            .comment("Range in blocks for full animal AI ticking (default: 16)")
            .defineInRange("animalActivationRange", 16, 8, 128);
        MISC_ACTIVATION_RANGE = BUILDER
            .comment("Range in blocks for full misc entity ticking (default: 8)")
            .defineInRange("miscActivationRange", 8, 4, 64);
        BUILDER.pop();

        BUILDER.push("Villager Throttle");
        VILLAGER_THROTTLE_ENABLED = BUILDER
            .comment("Reduce villager AI tick frequency to improve performance in villages")
            .define("villagerThrottleEnabled", false);
        VILLAGER_TICK_INTERVAL = BUILDER
            .comment("Only run full villager AI every N ticks (vanilla = 1, recommended: 3-4)")
            .defineInRange("villagerTickInterval", 3, 1, 10);
        BUILDER.pop();

        BUILDER.push("Redstone Throttle");
        REDSTONE_THROTTLE_ENABLED = BUILDER
            .comment("Limit the number of redstone updates per tick to prevent lag machines")
            .define("redstoneThrottleEnabled", false);
        REDSTONE_UPDATES_PER_TICK = BUILDER
            .comment("Maximum redstone neighbor updates per tick per world (default: 1000)")
            .defineInRange("redstoneUpdatesPerTick", 1000, 100, 100000);
        BUILDER.pop();

        BUILDER.push("TPS Monitor");
        TPS_MONITOR_ENABLED = BUILDER
            .comment("Enable real-time TPS monitoring with warnings and auto-optimization")
            .define("tpsMonitorEnabled", true);
        TPS_WARNING_THRESHOLD = BUILDER
            .comment("TPS below this value triggers a warning to admins (default: 18.0)")
            .defineInRange("tpsWarningThreshold", 18.0, 5.0, 20.0);
        TPS_CRITICAL_THRESHOLD = BUILDER
            .comment("TPS below this value triggers critical alerts (default: 15.0)")
            .defineInRange("tpsCriticalThreshold", 15.0, 5.0, 20.0);
        TPS_AUTO_OPTIMIZE = BUILDER
            .comment("Automatically enable more aggressive optimizations when TPS drops below critical threshold")
            .define("tpsAutoOptimize", false);
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.push("MOTD");
        MOTD_ENABLED = BUILDER
            .comment("Enable the MOTD Editor feature")
            .define("motdEnabled", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Instance methods for easier access
    public boolean isWorldManagerEnabled() {
        return WORLD_MANAGER_ENABLED.get();
    }

    public boolean areNetherPortalsEnabled() {
        return NETHER_PORTALS_ENABLED.get();
    }

    public boolean areEndPortalsEnabled() {
        return END_PORTALS_ENABLED.get();
    }

    public boolean areWorldTimersEnabled() {
        return WORLD_TIMERS_ENABLED.get();
    }

    public boolean isChatIsolationEnabled() {
        return CHAT_ISOLATION_ENABLED.get();
    }

    public boolean isTabIsolationEnabled() {
        return TAB_ISOLATION_ENABLED.get();
    }

    public boolean isPlayerManagerEnabled() {
        return PLAYER_MANAGER_ENABLED.get();
    }

    public boolean isSpectateEnabled() {
        return SPECTATE_ENABLED.get();
    }

    public boolean isViewInventoryEnabled() {
        return VIEW_INVENTORY_ENABLED.get();
    }

    public boolean areSlimeHeadsEnabled() {
        return SLIME_HEADS_ENABLED.get();
    }

    public double getStartingBalance() {
        return STARTING_BALANCE.get();
    }

    public boolean isEncryptedStorage() {
        return ENCRYPTED_STORAGE.get();
    }

    public int getTransactionHistoryLimit() {
        return TRANSACTION_HISTORY_LIMIT.get();
    }

    public int getMaxListingsPerPlayer() {
        return MAX_LISTINGS_PER_PLAYER.get();
    }

    public int getTeleportCooldown() {
        return TELEPORT_COOLDOWN.get();
    }
}


