package com.servermanagement.config;

import com.servermanagement.ServerManagementMod;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles automatic migration of config files between different mod versions.
 * Each migration updates the config version number and applies necessary transformations.
 */
public class ConfigMigration {
    
    /**
     * Migration interface - each version migration implements this
     */
    private interface Migration {
        void apply() throws Exception;
        String getDescription();
    }
    
    // Registry of all migrations - add new ones here for future versions
    private static final Map<Integer, Migration> MIGRATIONS = new HashMap<>();
    
    static {
        // Example migrations for future versions:
        
        // Migration from version 0 (no version field) to version 1 (current)
        MIGRATIONS.put(0, new Migration() {
            @Override
            public void apply() throws Exception {
                ServerManagementMod.LOGGER.debug("Applying migration 0->1: Adding config version field");
                // Version 0 configs don't have a version field at all
                // This migration is implicit - just adding the version field updates it
                // Guard against com.servermanagement.config.ModConfig not being bound yet (during mod construction)
                if (true) {
                    ModConfig.CONFIG_VERSION.set(1);
                    ModConfig.saveConfig();
                }
                // If not loaded yet, the version will be set to default (1) when config loads
            }
            
            @Override
            public String getDescription() {
                return "Initial config version - adding version tracking";
            }
        });
        
        // Future migrations would go here:
        // MIGRATIONS.put(1, new Migration() { ... }); // v1 -> v2
        // MIGRATIONS.put(2, new Migration() { ... }); // v2 -> v3
        // etc.
    }
    
    /**
     * Checks if migration is needed and performs it automatically.
     * 
     * @return true if migration was successful or not needed, false if migration failed
     */
    public static boolean checkAndMigrate() {
        try {
            // Cannot migrate if config spec isn't loaded yet
            if (!true) {
                ServerManagementMod.LOGGER.debug("Config spec not loaded yet, skipping migration");
                return true;
            }
            
            Path configPath = getConfigPath();
            File configFile = configPath.toFile();
            
            // If config doesn't exist, no migration needed (will be created fresh)
            if (!configFile.exists()) {
                ServerManagementMod.LOGGER.debug("Config file does not exist, no migration needed");
                return true;
            }
            
            // Read current config version
            int currentVersion = getCurrentConfigVersion();
            int targetVersion = ModConfig.CURRENT_CONFIG_VERSION;
            
            ServerManagementMod.LOGGER.debug("Config version check: Current={}, Target={}", currentVersion, targetVersion);
            
            // No migration needed if versions match
            if (currentVersion == targetVersion) {
                ServerManagementMod.LOGGER.debug("Config is up to date (version {})", currentVersion);
                return true;
            }
            
            // Version is newer than expected - might be from a future mod version
            if (currentVersion > targetVersion) {
                ServerManagementMod.LOGGER.warn("Config version {} is newer than expected {}! You may have downgraded the mod.", 
                    currentVersion, targetVersion);
                ServerManagementMod.LOGGER.warn("Attempting to use config as-is, but issues may occur.");
                return true;
            }
            
            // Migration needed
            ServerManagementMod.LOGGER.info("Migrating config from version {} to {}", currentVersion, targetVersion);
            
            return performMigration(currentVersion, targetVersion, configFile);
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Critical error during migration check!", e);
            return false;
        }
    }
    
    /**
     * Gets the current version from the config file.
     * Returns 0 if version field doesn't exist (pre-versioning configs).
     */
    private static int getCurrentConfigVersion() {
        try {
            // Try to read the version field
            int version = ModConfig.CONFIG_VERSION.get();
            ServerManagementMod.LOGGER.debug("Read config version from file: {}", version);
            return version;
        } catch (Exception e) {
            // If reading fails, this is likely a pre-versioning config
            ServerManagementMod.LOGGER.debug("No version field found in config - treating as version 0");
            return 0;
        }
    }
    
    /**
     * Performs the migration from oldVersion to newVersion.
     */
    private static boolean performMigration(int oldVersion, int newVersion, File configFile) {
        try {
            // Create backup before migration
            if (!createMigrationBackup(configFile, oldVersion)) {
                ServerManagementMod.LOGGER.error("Failed to create migration backup! Aborting migration for safety.");
                return false;
            }
            
            // Apply all migrations in sequence
            int currentVersion = oldVersion;
            while (currentVersion < newVersion) {
                Migration migration = MIGRATIONS.get(currentVersion);
                
                if (migration == null) {
                    ServerManagementMod.LOGGER.error("No migration defined for version {} -> {}!", 
                        currentVersion, currentVersion + 1);
                    return false;
                }
                
                ServerManagementMod.LOGGER.debug("Applying migration {}->{}: {}", 
                    currentVersion, currentVersion + 1, migration.getDescription());
                
                try {
                    migration.apply();
                    currentVersion++;
                    ServerManagementMod.LOGGER.debug("Migration {}->{} completed successfully", 
                        currentVersion - 1, currentVersion);
                } catch (Exception e) {
                    ServerManagementMod.LOGGER.error("Migration {}->{} failed!", 
                        currentVersion, currentVersion + 1, e);
                    return false;
                }
            }
            
            // Final version update
            ModConfig.CONFIG_VERSION.set(newVersion);
            ModConfig.saveConfig();
            
            ServerManagementMod.LOGGER.info("Config migrated successfully from version {} to {}", oldVersion, newVersion);
            
            return true;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Critical error during migration!", e);
            return false;
        }
    }
    
    /**
     * Creates a backup before migration.
     */
    private static boolean createMigrationBackup(File configFile, int version) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = configFile.getName() + ".pre-migration_v" + version + "_" + timestamp;
            Path backupPath = configFile.toPath().getParent().resolve(backupName);
            
            Files.copy(configFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            ServerManagementMod.LOGGER.debug("Created migration backup: {}", backupPath);
            
            return true;
            
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to create migration backup", e);
            return false;
        }
    }
    
    /**
     * Gets the path to the config file.
     */
    private static Path getConfigPath() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("servermanagement-common.toml");
    }
    
    /**
     * Utility method to read a value from the old config before migration.
     * Useful for preserving user settings during migration.
     */
    @SuppressWarnings("unused")
    private static String readOldConfigValue(File configFile, String section, String key) {
        try {
            String content = Files.readString(configFile.toPath());
            
            // Find the section
            String sectionMarker = "[\"" + section + "\"]";
            int sectionIndex = content.indexOf(sectionMarker);
            if (sectionIndex == -1) {
                sectionMarker = "[" + section + "]";
                sectionIndex = content.indexOf(sectionMarker);
            }
            
            if (sectionIndex == -1) {
                ServerManagementMod.LOGGER.warn("Section not found in config: {}", section);
                return null;
            }
            
            // Find the key within this section
            String keyMarker = key + " = ";
            int keyIndex = content.indexOf(keyMarker, sectionIndex);
            if (keyIndex == -1) {
                ServerManagementMod.LOGGER.warn("Key not found in section {}: {}", section, key);
                return null;
            }
            
            // Extract the value (up to newline)
            int valueStart = keyIndex + keyMarker.length();
            int valueEnd = content.indexOf('\n', valueStart);
            if (valueEnd == -1) {
                valueEnd = content.length();
            }
            
            String value = content.substring(valueStart, valueEnd).trim();
            ServerManagementMod.LOGGER.debug("Read old config value: [{}] {} = {}", section, key, value);
            return value;
            
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to read old config value", e);
            return null;
        }
    }
    
    /**
     * Gets a human-readable migration report.
     */
    public static String getMigrationReport(int fromVersion, int toVersion) {
        StringBuilder report = new StringBuilder();
        report.append("Config Migration Report:\n");
        report.append("=======================\n");
        
        for (int v = fromVersion; v < toVersion; v++) {
            Migration migration = MIGRATIONS.get(v);
            if (migration != null) {
                report.append(String.format("v%d -> v%d: %s\n", v, v + 1, migration.getDescription()));
            }
        }
        
        return report.toString();
    }
    
    /**
     * Checks if a config file needs migration without applying it.
     */
    public static boolean needsMigration() {
        try {
            // Cannot check migration if config spec isn't loaded yet
            if (!true) {
                return false;
            }
            
            Path configPath = getConfigPath();
            if (!configPath.toFile().exists()) {
                return false; // New config, no migration needed
            }
            
            int currentVersion = getCurrentConfigVersion();
            int targetVersion = ModConfig.CURRENT_CONFIG_VERSION;
            
            return currentVersion < targetVersion;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Error checking migration status", e);
            return false;
        }
    }
}
