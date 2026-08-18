package com.servermanagement.integration.dh;

import com.servermanagement.Constants;

/**
 * Detects and hooks into Distant Horizons via reflection.
 * All methods are safe to call regardless of whether DH is installed.
 * Uses reflection exclusively to avoid compile-time dependency.
 */
public class DistantHorizonsHook {

    private static boolean isDHAvailable = false;
    private static String dhVersion = "Unknown";

    static {
        try {
            // Try the DH 2.x API entry point first
            Class<?> dhApiClass = Class.forName("com.seibel.distanthorizons.api.DhApi");
            isDHAvailable = true;

            // Attempt to read DH version
            try {
                var versionMethod = dhApiClass.getDeclaredMethod("getModVersion");
                Object version = versionMethod.invoke(null);
                if (version != null) {
                    dhVersion = version.toString();
                }
            } catch (Exception e) {
                // Version method may not exist in all DH builds
                Constants.LOG.debug("DH API detected but could not read version.");
            }

            Constants.LOG.info("Distant Horizons detected (v{}). LOD integration enabled.", dhVersion);
        } catch (ClassNotFoundException e) {
            // Try legacy DH 1.x / early 2.x detection
            try {
                Class.forName("com.seibel.distanthorizons.core.DistantHorizons");
                isDHAvailable = true;
                dhVersion = "1.x (legacy)";
                Constants.LOG.info("Distant Horizons detected (legacy). LOD integration enabled.");
            } catch (ClassNotFoundException e2) {
                Constants.LOG.debug("Distant Horizons not found. LOD integration disabled.");
            }
        }
    }

    /**
     * Returns true if Distant Horizons is loaded in the current environment.
     */
    public static boolean isAvailable() {
        return isDHAvailable;
    }

    /**
     * Returns the detected DH version string, or "Unknown" / "Not Installed".
     */
    public static String getVersion() {
        return isDHAvailable ? dhVersion : "Not Installed";
    }
}
