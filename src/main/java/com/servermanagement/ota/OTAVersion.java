package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages OTA version tracking with build numbers and Minecraft version awareness.
 * Version format: v1.0.3-b04-release (network) / v1.0.3-b04-mc1.20.1-release (display)
 */
public class OTAVersion {
    
    private final String version;
    private final int buildNumber;
    private final String minecraftVersion;
    private final String releaseType;
    private final String releaseNotes;
    
    private static OTAVersion CURRENT_VERSION = null;
    
    private OTAVersion(String version, int buildNumber, String minecraftVersion, String releaseType, String releaseNotes) {
        this.version = version;
        this.buildNumber = buildNumber;
        this.minecraftVersion = minecraftVersion;
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
                CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "unknown", "No release notes");
                return CURRENT_VERSION;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            String version = props.getProperty("ota.version", "1.0.0");
            int build = Integer.parseInt(props.getProperty("ota.build", "1"));
            String mcVersion = props.getProperty("ota.minecraft_version", "unknown");
            String releaseType = props.getProperty("ota.releaseType", "unknown");
            String releaseNotes = props.getProperty("ota.releaseNotes", "No release notes");
            
            CURRENT_VERSION = new OTAVersion(version, build, mcVersion, releaseType, releaseNotes);
            
            ServerManagementMod.LOGGER.info("Loaded OTA version: {} (build {}, MC {})", version, build, mcVersion);
            return CURRENT_VERSION;
            
        } catch (IOException | NumberFormatException e) {
            ServerManagementMod.LOGGER.error("Failed to load OTA version", e);
            CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "error", "Failed to load version info");
            return CURRENT_VERSION;
        }
    }
    
    /**
     * Get the full version string for network transmission (without MC version).
     * Format: v1.0.3-b04-release
     */
    public String getFullVersion() {
        return String.format("%s-b%02d-%s", version, buildNumber, releaseType);
    }
    
    /**
     * Get the display version string including MC version.
     * Format: v1.0.3-b04-mc1.20.1-release
     */
    public String getDisplayVersion() {
        if (minecraftVersion != null && !"unknown".equals(minecraftVersion)) {
            return String.format("%s-b%02d-mc%s-%s", version, buildNumber, minecraftVersion, releaseType);
        }
        return getFullVersion();
    }
    
    /**
     * Get just the semantic version (e.g., "v1.0.3")
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
     * Get the Minecraft version this build targets
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
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
     * Check if this version targets the same Minecraft version as another
     */
    public boolean isCompatibleWith(OTAVersion other) {
        if (other == null) return false;
        if ("unknown".equals(this.minecraftVersion) || "unknown".equals(other.minecraftVersion)) {
            return true; // Can't determine, assume compatible
        }
        return this.minecraftVersion.equals(other.minecraftVersion);
    }
    
    /**
     * Check if this version is newer than another.
     * Only compares versions targeting the same Minecraft version.
     */
    public boolean isNewerThan(OTAVersion other) {
        if (other == null) {
            return true;
        }
        
        // First compare semantic versions
        int versionCompare = compareVersionStrings(this.version, other.version);
        if (versionCompare > 0) {
            return true;
        } else if (versionCompare < 0) {
            return false;
        }
        
        // Same version number, compare build numbers
        return this.buildNumber > other.buildNumber;
    }
    
    /**
     * Parse version string from network packet or display string.
     * Supports formats:
     *   New: "v1.0.3-b04-release" or "v1.0.3-b04-mc1.20.1-release"
     *   Old: "v1.0.3-release.3"
     */
    public static OTAVersion parseFromString(String versionString) {
        try {
            // New format: contains "-b" followed by digits
            if (versionString.contains("-b") && versionString.matches(".*-b\\d+.*")) {
                String[] parts = versionString.split("-");
                String version = parts[0];
                int build = 0;
                String mcVersion = null;
                String releaseType = "unknown";
                
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].matches("b\\d+")) {
                        build = Integer.parseInt(parts[i].substring(1));
                    } else if (parts[i].startsWith("mc")) {
                        mcVersion = parts[i].substring(2);
                    } else {
                        releaseType = parts[i];
                    }
                }
                
                return new OTAVersion(version, build, mcVersion, releaseType, "");
            }
            
            // Old format: "v1.0.3-release.3" (version.build with dot separator)
            String[] parts = versionString.split("\\.");
            if (parts.length >= 4) {
                int build = Integer.parseInt(parts[parts.length - 1]);
                StringBuilder versionBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) versionBuilder.append(".");
                    versionBuilder.append(parts[i]);
                }
                return new OTAVersion(versionBuilder.toString(), build, null, "remote", "");
            }
            
            // Fallback for unrecognized format
            return new OTAVersion(versionString, 0, null, "unknown", "");
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to parse version string: {}", versionString);
            return new OTAVersion(versionString, 0, null, "error", "");
        }
    }
    
    /**
     * Compare two semantic version strings.
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
     * Parse a version part, extracting numeric portion.
     * Handles versions like "v1" or "3-release" by stripping non-numeric chars.
     */
    private static int parseVersionPart(String part) {
        String numericPart = part.replaceAll("^[^0-9]*", "").replaceAll("[^0-9].*$", "");
        try {
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return getDisplayVersion();
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
