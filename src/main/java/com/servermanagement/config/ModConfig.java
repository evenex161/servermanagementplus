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
    public static final DoubleValue STARTING_BALANCE;
    public static final BooleanValue ENCRYPTED_STORAGE;
    public static final IntValue TRANSACTION_HISTORY_LIMIT;
    public static final IntValue MAX_LISTINGS_PER_PLAYER;

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
        STARTING_BALANCE = BUILDER
            .comment("Starting balance for new players")
            .defineInRange("startingBalance", 1000.0, 0.0, 1000000.0);
        ENCRYPTED_STORAGE = BUILDER
            .comment("Enable AES-256-GCM encryption for economy data")
            .define("encryptedStorage", true);
        TRANSACTION_HISTORY_LIMIT = BUILDER
            .comment("Maximum number of transactions stored per player")
            .defineInRange("transactionHistoryLimit", 100, 10, 10000);
        MAX_LISTINGS_PER_PLAYER = BUILDER
            .comment("Maximum number of active MineBay listings per player")
            .defineInRange("maxListingsPerPlayer", 10, 1, 100);
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
