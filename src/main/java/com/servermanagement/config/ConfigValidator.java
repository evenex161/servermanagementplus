package com.servermanagement.config;

import com.servermanagement.ServerManagementMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Validates and repairs corrupted or invalid configuration files.
 * Automatically creates backups and regenerates configs when issues are detected.
 */
public class ConfigValidator {
    
    private static final String CONFIG_FILENAME = "servermanagement-common.toml";
    private static final String BACKUP_SUFFIX = ".backup";
    
    /**
     * Validates the configuration file and repairs it if necessary.
     * Called during mod initialization.
     * 
     * @return true if config is valid or was successfully repaired, false otherwise
     */
    public static boolean validateAndRepair() {
        try {
            Path configPath = getConfigPath();
            File configFile = configPath.toFile();
            
            ServerManagementMod.LOGGER.info("Validating configuration file: {}", configPath);
            
            // Check if config file exists
            if (!configFile.exists()) {
                ServerManagementMod.LOGGER.info("Config file does not exist. It will be created with defaults.");
                return true; // ModConfigSpec will create it
            }
            
            // Validate config structure first
            if (!validateConfigStructure(configFile)) {
                ServerManagementMod.LOGGER.warn("Config validation failed! Attempting to repair...");
                return repairConfig(configFile);
            }
            
            // Check if migration is needed BEFORE validating values
            // (old configs might have different structure)
            if (ConfigMigration.needsMigration()) {
                ServerManagementMod.LOGGER.info("Config migration is needed");
                if (!ConfigMigration.checkAndMigrate()) {
                    ServerManagementMod.LOGGER.error("Config migration failed!");
                    return false;
                }
            }
            
            // Validate config values after migration
            if (!validateConfigValues()) {
                ServerManagementMod.LOGGER.warn("Config values are invalid! Attempting to repair...");
                return repairConfig(configFile);
            }
            
            ServerManagementMod.LOGGER.info("Configuration validated successfully");
            return true;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Critical error during config validation!", e);
            return false;
        }
    }
    
    /**
     * Validates the structure of the config file (can be read as TOML, not corrupted).
     */
    private static boolean validateConfigStructure(File configFile) {
        try {
            // Check if file is readable
            if (!configFile.canRead()) {
                ServerManagementMod.LOGGER.error("Config file is not readable: {}", configFile);
                return false;
            }
            
            // Check file size (0 bytes = corrupted)
            if (configFile.length() == 0) {
                ServerManagementMod.LOGGER.error("Config file is empty (0 bytes)");
                return false;
            }
            
            // Check if file size is suspiciously small (less than 100 bytes)
            if (configFile.length() < 100) {
                ServerManagementMod.LOGGER.warn("Config file is suspiciously small: {} bytes", configFile.length());
                return false;
            }
            
            // Try to read the file content
            String content = Files.readString(configFile.toPath());
            
            // Check for TOML syntax errors (basic validation)
            if (!content.contains("[") || !content.contains("]")) {
                ServerManagementMod.LOGGER.error("Config file does not contain valid TOML sections");
                return false;
            }
            
            // Check for required sections
            if (!content.contains("World Manager") && !content.contains("Player Manager")) {
                ServerManagementMod.LOGGER.error("Config file is missing required sections");
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to read config file", e);
            return false;
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Unexpected error validating config structure", e);
            return false;
        }
    }
    
    /**
     * Validates that all config values can be read and are of the correct type.
     */
    private static boolean validateConfigValues() {
        try {
            // Try to read all config values
            // If any throw an exception, the config is corrupted
            boolean worldManager = ModConfig.WORLD_MANAGER_ENABLED.get();
            boolean netherPortals = ModConfig.NETHER_PORTALS_ENABLED.get();
            boolean endPortals = ModConfig.END_PORTALS_ENABLED.get();
            boolean worldTimers = ModConfig.WORLD_TIMERS_ENABLED.get();
            boolean chatIsolation = ModConfig.CHAT_ISOLATION_ENABLED.get();
            boolean tabIsolation = ModConfig.TAB_ISOLATION_ENABLED.get();
            
            boolean playerManager = ModConfig.PLAYER_MANAGER_ENABLED.get();
            boolean spectate = ModConfig.SPECTATE_ENABLED.get();
            boolean viewInventory = ModConfig.VIEW_INVENTORY_ENABLED.get();
            boolean slimeHeads = ModConfig.SLIME_HEADS_ENABLED.get();
            
            ServerManagementMod.LOGGER.debug("Config values read successfully");
            return true;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to read config values", e);
            return false;
        }
    }
    
    /**
     * Repairs a corrupted config file by backing it up and creating a fresh one.
     */
    private static boolean repairConfig(File configFile) {
        try {
            ServerManagementMod.LOGGER.warn("=== REPAIRING CORRUPTED CONFIG ===");
            
            // Create backup
            boolean backupCreated = createBackup(configFile);
            if (backupCreated) {
                ServerManagementMod.LOGGER.info("Backup created successfully");
            } else {
                ServerManagementMod.LOGGER.warn("Failed to create backup, but continuing with repair");
            }
            
            // Delete the corrupted config
            if (configFile.delete()) {
                ServerManagementMod.LOGGER.info("Deleted corrupted config file");
            } else {
                ServerManagementMod.LOGGER.error("Failed to delete corrupted config file!");
                return false;
            }
            
            // Force save to create new config with defaults
            try {
                ModConfig.SPEC.save();
                ServerManagementMod.LOGGER.info("Created fresh config file with default values");
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to save new config file!", e);
                return false;
            }
            
            // Verify the new config
            if (configFile.exists() && validateConfigStructure(configFile)) {
                ServerManagementMod.LOGGER.info("=== CONFIG REPAIR SUCCESSFUL ===");
                ServerManagementMod.LOGGER.info("A backup of your old config was saved with timestamp");
                return true;
            } else {
                ServerManagementMod.LOGGER.error("=== CONFIG REPAIR FAILED ===");
                return false;
            }
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Critical error during config repair!", e);
            return false;
        }
    }
    
    /**
     * Creates a timestamped backup of the config file.
     */
    private static boolean createBackup(File configFile) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = configFile.getName() + BACKUP_SUFFIX + "_" + timestamp;
            Path backupPath = configFile.toPath().getParent().resolve(backupName);
            
            Files.copy(configFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            ServerManagementMod.LOGGER.info("Created config backup: {}", backupPath);
            
            return true;
            
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to create config backup", e);
            return false;
        }
    }
    
    /**
     * Gets the path to the config file.
     */
    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILENAME);
    }
    
    /**
     * Cleans up old backup files (keeps only the 5 most recent).
     */
    public static void cleanupOldBackups() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File[] backups = configDir.toFile().listFiles((dir, name) -> 
                name.startsWith(CONFIG_FILENAME + BACKUP_SUFFIX));
            
            if (backups != null && backups.length > 5) {
                ServerManagementMod.LOGGER.info("Found {} config backups, cleaning up old ones", backups.length);
                
                // Sort by last modified time
                java.util.Arrays.sort(backups, (a, b) -> 
                    Long.compare(b.lastModified(), a.lastModified()));
                
                // Delete all but the 5 most recent
                for (int i = 5; i < backups.length; i++) {
                    if (backups[i].delete()) {
                        ServerManagementMod.LOGGER.debug("Deleted old backup: {}", backups[i].getName());
                    }
                }
            }
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to cleanup old backups", e);
        }
    }
    
    /**
     * Forces a complete config reset to defaults.
     * USE WITH CAUTION - This will delete the current config!
     */
    public static boolean forceReset() {
        try {
            ServerManagementMod.LOGGER.warn("=== FORCING CONFIG RESET ===");
            Path configPath = getConfigPath();
            File configFile = configPath.toFile();
            
            if (configFile.exists()) {
                createBackup(configFile);
                if (configFile.delete()) {
                    ServerManagementMod.LOGGER.info("Deleted existing config");
                }
            }
            
            ModConfig.SPEC.save();
            ServerManagementMod.LOGGER.info("Created fresh config with defaults");
            
            return true;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to force reset config!", e);
            return false;
        }
    }
}
