package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages OTA version tracking with build numbers and Minecraft version awareness.
 * This allows fine-grained control over updates independent of Forge mod version,
 * and ensures OTA updates only apply to matching Minecraft versions.
 */
public class OTAVersion {
    
    private final String version;
    private final int buildNumber;
    private final String releaseType;
    private final String releaseNotes;
    private final String minecraftVersion;
    
    private static OTAVersion CURRENT_VERSION = null;
    
    private OTAVersion(String version, int buildNumber, String releaseType, String releaseNotes, String minecraftVersion) {
        this.version = version;
        this.buildNumber = buildNumber;
        this.releaseType = releaseType;
        this.releaseNotes = releaseNotes;
        this.minecraftVersion = minecraftVersion != null ? minecraftVersion : "unknown";
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
                CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "No release notes", "unknown");
                return CURRENT_VERSION;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            String version = props.getProperty("ota.version", "1.0.0");
            int build = Integer.parseInt(props.getProperty("ota.build", "1"));
            String releaseType = props.getProperty("ota.releaseType", "unknown");
            String releaseNotes = props.getProperty("ota.releaseNotes", "No release notes");
            String mcVersion = props.getProperty("ota.minecraft_version", "unknown");
            
            CURRENT_VERSION = new OTAVersion(version, build, releaseType, releaseNotes, mcVersion);
            
            ServerManagementMod.LOGGER.info("Loaded OTA version: {} (build {}, MC {})", version, build, mcVersion);
            return CURRENT_VERSION;
            
        } catch (IOException | NumberFormatException e) {
            ServerManagementMod.LOGGER.error("Failed to load OTA version", e);
            CURRENT_VERSION = new OTAVersion("1.0.0", 1, "error", "Failed to load version info", "unknown");
            return CURRENT_VERSION;
        }
    }
    
    /**
     * Get the full version string for network transmission (version + build + mcVersion)
     * Format: "1.0.3.3:1.21.1"
     */
    public String getFullVersion() {
        return version + "." + buildNumber + ":" + minecraftVersion;
    }
    
    /**
     * Get the display version string (human-readable)
     * Format: "v1.0.3-mc1.21.1-release (build 3)"
     */
    public String getDisplayVersion() {
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
     * Get the Minecraft version this OTA version targets
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    /**
     * Check if this version targets the same Minecraft version as another
     */
    public boolean isCompatibleWith(OTAVersion other) {
        if (other == null) {
            return false;
        }
        return this.minecraftVersion.equals(other.minecraftVersion);
    }
    
    /**
     * Check if this version is newer than another.
     * Returns true if this version should trigger an update.
     * Only considers versions targeting the same Minecraft version.
     */
    public boolean isNewerThan(OTAVersion other) {
        if (other == null) {
            return true;
        }
        
        // Only allow updates within the same Minecraft version
        if (!this.minecraftVersion.equals("unknown") && !other.minecraftVersion.equals("unknown")
                && !this.minecraftVersion.equals("remote") && !other.minecraftVersion.equals("remote")) {
            if (!this.minecraftVersion.equals(other.minecraftVersion)) {
                ServerManagementMod.LOGGER.warn("MC version mismatch: {} vs {} - skipping update",
                    this.minecraftVersion, other.minecraftVersion);
                return false;
            }
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
     * Parse version string from network packet (format: "version.build" or "version.build:mcVersion")
     */
    public static OTAVersion parseFromString(String versionString) {
        try {
            // Check for MC version suffix (format: "1.0.3.3:1.21.1")
            String mcVersion = "remote";
            String vPart = versionString;
            if (versionString.contains(":")) {
                String[] colonParts = versionString.split(":", 2);
                vPart = colonParts[0];
                mcVersion = colonParts[1];
            }
            
            String[] parts = vPart.split("\\.");
            if (parts.length < 4) {
                // Fallback for old format
                return new OTAVersion(vPart, 0, "unknown", "", mcVersion);
            }
            
            // Extract build number (last component)
            int build = Integer.parseInt(parts[parts.length - 1]);
            
            // Reconstruct version (everything except last component)
            StringBuilder versionBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) versionBuilder.append(".");
                versionBuilder.append(parts[i]);
            }
            
            return new OTAVersion(versionBuilder.toString(), build, "remote", "", mcVersion);
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to parse version string: {}", versionString);
            return new OTAVersion(versionString, 0, "error", "", "unknown");
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
        return this.version.equals(other.version) 
            && this.buildNumber == other.buildNumber
            && this.minecraftVersion.equals(other.minecraftVersion);
    }
    
    @Override
    public int hashCode() {
        return (version + "." + buildNumber + ":" + minecraftVersion).hashCode();
    }
}
