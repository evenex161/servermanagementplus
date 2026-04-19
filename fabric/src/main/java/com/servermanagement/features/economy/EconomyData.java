package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages persistent storage of economy data (bank accounts, balances, transactions).
 * Version 1.0.0 - Initial release with versioning support
 */
public class EconomyData {
    // Data version for migration support
    private int dataVersion = DataVersion.CURRENT_VERSION;
    
    private Map<UUID, BankAccount> accounts = new HashMap<>();

    public EconomyData() {
        this.accounts = new HashMap<>();
        this.dataVersion = DataVersion.CURRENT_VERSION;
    }

    /**
     * Load economy data from disk with automatic decryption and migration
     */
    public static EconomyData load(MinecraftServer server) {
        File file = getDataFile(server);
        EconomyData data = SecureDataStorage.load(file, EconomyData.class, new EconomyData());
        
        // Ensure maps are initialized
        if (data.accounts == null) {
            ServerManagementMod.LOGGER.warn("Account data was null, initializing empty map");
            data.accounts = new HashMap<>();
        }
        
        // Check version and migrate if needed
        if (data.dataVersion == 0) {
            ServerManagementMod.LOGGER.debug("Migrating legacy economy data to version {}", DataVersion.CURRENT_VERSION);
            data.dataVersion = DataVersion.CURRENT_VERSION;
        } else if (data.dataVersion < DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.debug("Migrating economy data from version {} to {}", 
                data.dataVersion, DataVersion.CURRENT_VERSION);
            data.migrateData(data.dataVersion, DataVersion.CURRENT_VERSION);
            data.dataVersion = DataVersion.CURRENT_VERSION;
        } else if (data.dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.error("Economy data version {} is newer than supported version {}! Data may be incompatible.",
                data.dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        ServerManagementMod.LOGGER.debug("Loaded economy data v{} with {} accounts", 
            data.dataVersion, data.accounts.size());
        return data;
    }
    
    /**
     * Migrate data between versions
     */
    private void migrateData(int fromVersion, int toVersion) {
        ServerManagementMod.LOGGER.debug("Performing economy data migration: {}", 
            DataVersion.getMigrationPath(fromVersion, toVersion));
        
        // Future version migrations will be added here
        // Example: if (fromVersion <= 1 && toVersion >= 2) { /* migrate v1 to v2 */ }
    }

    /**
     * Save economy data to disk with encryption
     */
    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        SecureDataStorage.save(this, file, EconomyData.class);
        ServerManagementMod.LOGGER.debug("Saved encrypted economy data v{} with {} accounts", 
            dataVersion, accounts.size());
    }

    private static File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/economy.json");
    }
    
    public int getDataVersion() {
        return dataVersion;
    }

    /**
     * Get or create a bank account for a player.
     * New accounts receive the configured starting balance.
     */
    public BankAccount getOrCreateAccount(UUID playerUUID) {
        return accounts.computeIfAbsent(playerUUID, uuid -> {
            double startingBalance = com.servermanagement.config.ModConfig.STARTING_BALANCE.get();
            BankAccount account = new BankAccount(uuid, startingBalance);
            if (startingBalance > 0) {
                account.addTransaction(new Transaction(
                    TransactionType.ADMIN_GIVE,
                    startingBalance,
                    "Starting Balance"
                ));
            }
            return account;
        });
    }

    /**
     * Get a bank account (null if doesn't exist)
     */
    public BankAccount getAccount(UUID playerUUID) {
        return accounts.get(playerUUID);
    }

    /**
     * Check if player has an account
     */
    public boolean hasAccount(UUID playerUUID) {
        return accounts.containsKey(playerUUID);
    }

    /**
     * Get all accounts
     */
    public Map<UUID, BankAccount> getAllAccounts() {
        return Collections.unmodifiableMap(accounts);
    }
}
