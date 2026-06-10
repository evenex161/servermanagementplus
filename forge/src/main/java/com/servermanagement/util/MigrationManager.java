package com.servermanagement.util;

import com.servermanagement.ServerManagementMod;
import net.minecraft.server.MinecraftServer;

/**
 * Centralized migration manager for handling data structure upgrades across versions.
 * This class provides utilities and orchestration for migrating data from older versions
 * to newer versions of the ServerManagement mod.
 * 
 * When adding a new version:
 * 1. Add the new version constant to DataVersion
 * 2. Create migration methods here (e.g., migrateV1ToV2())
 * 3. Add migration logic to executeMigration()
 * 4. Document the changes in MIGRATION_GUIDE.md
 */
public class MigrationManager {
    
    /**
     * Execute migrations for all data structures from one version to another.
     * This is the central orchestration point for all migrations.
     * 
     * @param server The Minecraft server instance
     * @param fromVersion The source data version
     * @param toVersion The target data version
     */
    public static void executeMigration(MinecraftServer server, int fromVersion, int toVersion) {
        ServerManagementMod.LOGGER.info("Executing centralized migration from v{} to v{}", fromVersion, toVersion);
        
        // Migration chain: execute each version increment
        int currentVersion = fromVersion;
        
        while (currentVersion < toVersion) {
            int nextVersion = currentVersion + 1;
            ServerManagementMod.LOGGER.info("Migrating data structures from v{} to v{}", currentVersion, nextVersion);
            
            // Execute version-specific migrations
            switch (nextVersion) {
                case DataVersion.VERSION_1_0_0:
                    migrateLegacyToV1(server);
                    break;
                
                case DataVersion.VERSION_1_0_0_RELEASE:
                    migrateV1EAToV1Release(server);
                    break;
                    
                // Future versions:
                // case DataVersion.VERSION_1_1_0:
                //     migrateV1ToV1_1(server);
                //     break;
                //
                // case DataVersion.VERSION_2_0_0:
                //     migrateV1_1ToV2(server);
                //     break;
                
                default:
                    ServerManagementMod.LOGGER.warn("No migration path defined for v{} to v{}", 
                        currentVersion, nextVersion);
                    break;
            }
            
            currentVersion = nextVersion;
        }
        
        ServerManagementMod.LOGGER.info("Migration completed successfully");
    }
    
    /**
     * Migrate legacy data (version 0) to version 1.0.0
     * This handles the initial versioning system introduction.
     */
    private static void migrateLegacyToV1(MinecraftServer server) {
        ServerManagementMod.LOGGER.info("Migrating legacy data to v1.0.0 (Early Access)");
        
        // Legacy data doesn't need transformation, just version tagging
        // All data loaders will automatically add the dataVersion field
        // when they save next time
        
        ServerManagementMod.LOGGER.info("Legacy to v1 migration: Data will be version-tagged on next save");
    }
    
    /**
     * Migrate from v1.0.0-EA to v1.0.0-release
     * This migration handles bug fixes and improvements.
     * No structural data changes required.
     */
    private static void migrateV1EAToV1Release(MinecraftServer server) {
        ServerManagementMod.LOGGER.info("Migrating from v1.0.0-EA to v1.0.0-release");
        
        // v1.0.0-release is a bug fix and improvement release
        // No data structure changes needed
        // Just version tagging
        
        ServerManagementMod.LOGGER.info("EA to Release migration: No data transformations needed");
        ServerManagementMod.LOGGER.info("Changes in this release:");
        ServerManagementMod.LOGGER.info("  - Added OTA (Over-The-Air) update system");
        ServerManagementMod.LOGGER.info("  - Improved client-server version synchronization");
        ServerManagementMod.LOGGER.info("  - Various bug fixes and stability improvements");
    }
    
    /**
     * Example future migration: v1.0 to v1.1
     * Uncomment and modify when creating version 1.1
     */
    /*
    private static void migrateV1ToV1_1(MinecraftServer server) {
        ServerManagementMod.LOGGER.info("Migrating data from v1.0 to v1.1");
        
        // Example migration tasks:
        // - Load all data structures
        // - Transform data as needed
        // - Save with new version
        
        // Example: If EconomyData needs a new field in v1.1
        // EconomyData data = EconomyData.load(server);
        // data.performV1ToV1_1Migration();
        // data.save(server);
        
        ServerManagementMod.LOGGER.info("v1.0 to v1.1 migration completed");
    }
    */
    
    /**
     * Verify all data structures are compatible with the current version
     */
    public static boolean verifyDataCompatibility(MinecraftServer server) {
        ServerManagementMod.LOGGER.info("Verifying data compatibility...");
        
        // This could be expanded to check each data file's version
        // and report any incompatibilities before loading
        
        return true;
    }
    
    /**
     * Backup all data files before a major migration
     */
    public static void backupDataFiles(MinecraftServer server) {
        ServerManagementMod.LOGGER.info("Creating backup of all data files...");
        
        try {
            java.io.File serverDir = server.getServerDirectory();
            java.io.File serverDataDir = new java.io.File(serverDir, "servermanagement");
            java.io.File configDir = new java.io.File(serverDir, "config/servermanagement");
            
            java.io.File backupDir = new java.io.File(serverDir, "serverdata/servermanagement/backups/backup_" + System.currentTimeMillis());
            
            if (serverDataDir.exists()) {
                copyDirectory(serverDataDir, new java.io.File(backupDir, "servermanagement"));
            }
            if (configDir.exists()) {
                copyDirectory(configDir, new java.io.File(backupDir, "config/servermanagement"));
            }
            
            ServerManagementMod.LOGGER.info("Backup completed successfully to: {}", backupDir.getAbsolutePath());
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to create backup of data files", e);
        }
    }

    private static void copyDirectory(java.io.File sourceDir, java.io.File destDir) throws java.io.IOException {
        if (sourceDir.isDirectory()) {
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            String[] children = sourceDir.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new java.io.File(sourceDir, child), new java.io.File(destDir, child));
                }
            }
        } else {
            java.nio.file.Files.copy(sourceDir.toPath(), destDir.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
