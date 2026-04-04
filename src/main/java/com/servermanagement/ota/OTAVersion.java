package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages OTA version tracking with build numbers
 * This allows fine-grained control over updates independent of Forge mod version
 */
public class OTAVersion {
    
    private final String version;
    private final int buildNumber;
    private final String releaseType;
    private final String releaseNotes;
    
    private static OTAVersion CURRENT_VERSION = null;
    
    private OTAVersion(String version, int buildNumber, String releaseType, String releaseNotes) {
        this.version = version;
        this.buildNumber = buildNumber;
        this.releaseType = releaseType;
        this.releaseNotes = releaseNotes;
    }
    
    /**
     * Load OTA version from properties file in JAR
     */
    public static OTAVersion loadFromResources() {
        if (CURRENT_VERSION != null) {
            return CURRENT_VERSION;
        }
        
        try (InputStream is = OTAVersion.class.getResourceAsStream("/ota.properties")) {
            if (is == null) {
                ServerManagementMod.LOGGER.warn("ota.properties not found, using fallback version");
                CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "No release notes");
                return CURRENT_VERSION;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            String version = props.getProperty("ota.version", "1.0.0");
            int build = Integer.parseInt(props.getProperty("ota.build", "1"));
            String releaseType = props.getProperty("ota.releaseType", "unknown");
            String releaseNotes = props.getProperty("ota.releaseNotes", "No release notes");
            
            CURRENT_VERSION = new OTAVersion(version, build, releaseType, releaseNotes);
            
            ServerManagementMod.LOGGER.info("Loaded OTA version: {} (build {})", version, build);
            return CURRENT_VERSION;
            
        } catch (IOException | NumberFormatException e) {
            ServerManagementMod.LOGGER.error("Failed to load OTA version", e);
            CURRENT_VERSION = new OTAVersion("1.0.0", 1, "error", "Failed to load version info");
            return CURRENT_VERSION;
        }
    }
    
    /**
     * Get the full version string (version + build)
     */
    public String getFullVersion() {
        return version + "." + buildNumber;
    }
    
    /**
     * Get just the semantic version (without build number)
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Get the build number
     */
    public int getBuildNumber() {
        return buildNumber;
    }
    
    /**
     * Get the release type (e.g., "release", "beta", "alpha")
     */
    public String getReleaseType() {
        return releaseType;
    }
    
    /**
     * Get release notes
     */
    public String getReleaseNotes() {
        return releaseNotes;
    }
    
    /**
     * Check if this version is newer than another
     * Returns true if this version should trigger an update
     */
    public boolean isNewerThan(OTAVersion other) {
        if (other == null) {
            return true;
        }
        
        // First compare semantic versions
        int versionCompare = compareVersionStrings(this.version, other.version);
        if (versionCompare > 0) {
            return true; // This version number is higher
        } else if (versionCompare < 0) {
            return false; // This version number is lower
        }
        
        // Same version number, compare build numbers
        return this.buildNumber > other.buildNumber;
    }
    
    /**
     * Parse version string from network packet (format: "version.build")
     */
    public static OTAVersion parseFromString(String versionString) {
        try {
            String[] parts = versionString.split("\\.");
            if (parts.length < 4) {
                // Fallback for old format
                return new OTAVersion(versionString, 0, "unknown", "");
            }
            
            // Extract build number (last component)
            int build = Integer.parseInt(parts[parts.length - 1]);
            
            // Reconstruct version (everything except last component)
            StringBuilder versionBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) versionBuilder.append(".");
                versionBuilder.append(parts[i]);
            }
            
            return new OTAVersion(versionBuilder.toString(), build, "remote", "");
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to parse version string: {}", versionString);
            return new OTAVersion(versionString, 0, "error", "");
        }
    }
    
    /**
     * Compare two semantic version strings
     * Returns: positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private static int compareVersionStrings(String v1, String v2) {
        String[] v1Parts = v1.split("\\.");
        String[] v2Parts = v2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
            
            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }
        
        return 0;
    }
    
    /**
     * Parse a version part, extracting numeric portion
     * Handles versions like "1.0.0-EA" by stripping non-numeric suffixes
     */
    private static int parseVersionPart(String part) {
        // Strip leading non-numeric prefix (e.g., "v" in "v1") and trailing non-numeric suffix (e.g., "-release", "-EA")
        String numericPart = part.replaceAll("^[^0-9]*", "").replaceAll("[^0-9].*$", "");
        try {
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return getFullVersion();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OTAVersion)) {
            return false;
        }
        OTAVersion other = (OTAVersion) obj;
        return this.version.equals(other.version) && this.buildNumber == other.buildNumber;
    }
    
    @Override
    public int hashCode() {
        return (version + "." + buildNumber).hashCode();
    }
}
