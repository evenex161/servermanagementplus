package com.servermanagement.util;

/**
 * Data version constants and migration utilities
 * Update DATA_VERSION when making breaking changes to any data structure
 */
public class DataVersion {
    
    // Current data version - increment when making breaking changes
    public static final int CURRENT_VERSION = 2;
    
    // Data version history
    public static final int VERSION_1_0_0 = 1; // Initial EA release - Nov 9, 2025
    public static final int VERSION_1_0_0_RELEASE = 2; // Stable release with OTA - Nov 10, 2025
    
    /**
     * Check if data version is compatible
     */
    public static boolean isCompatible(int dataVersion) {
        return dataVersion > 0 && dataVersion <= CURRENT_VERSION;
    }
    
    /**
     * Get migration path description
     */
    public static String getMigrationPath(int fromVersion, int toVersion) {
        if (fromVersion == toVersion) {
            return "No migration needed";
        }
        if (fromVersion < 1) {
            return "Legacy data - will be migrated to version 1";
        }
        return String.format("Migrating from version %d to %d", fromVersion, toVersion);
    }
}
