package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages OTA version tracking with build numbers, Minecraft version, and mod loader awareness.
 * Version format: v1.0.3-b04-release (network) / v1.0.3-b04-mc1.21.1-forge-release (display)
 */
public class OTAVersion {
    
    private final String version;
    private final int buildNumber;
    private final String releaseType;
    private final String releaseNotes;
    private final String minecraftVersion;
    private final String modLoader;
    
    private static OTAVersion CURRENT_VERSION = null;
    
    private OTAVersion(String version, int buildNumber, String minecraftVersion, String modLoader, String releaseType, String releaseNotes) {
        this.version = version;
        this.buildNumber = buildNumber;
        this.minecraftVersion = minecraftVersion != null ? minecraftVersion : "unknown";
        this.modLoader = modLoader != null ? modLoader : "unknown";
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
                CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "unknown", "unknown", "No release notes");
                return CURRENT_VERSION;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            String version = props.getProperty("ota.version", "1.0.0");
            int build = Integer.parseInt(props.getProperty("ota.build", "1"));
            String releaseType = props.getProperty("ota.releaseType", "unknown");
            String releaseNotes = props.getProperty("ota.releaseNotes", "No release notes");
            String mcVersion = props.getProperty("ota.minecraft_version", "unknown");
            String loader = props.getProperty("ota.mod_loader", "unknown");
            
            CURRENT_VERSION = new OTAVersion(version, build, mcVersion, loader, releaseType, releaseNotes);
            
            ServerManagementMod.LOGGER.info("Loaded OTA version: {} (build {}, MC {}, loader {})", version, build, mcVersion, loader);
            return CURRENT_VERSION;
            
        } catch (IOException | NumberFormatException e) {
            ServerManagementMod.LOGGER.error("Failed to load OTA version", e);
            CURRENT_VERSION = new OTAVersion("1.0.0", 1, "unknown", "unknown", "error", "Failed to load version info");
            return CURRENT_VERSION;
        }
    }
    
    /**
     * Get the full version string for network transmission (without MC version/loader).
     * Format: v1.0.3-b04-release
     */
    public String getFullVersion() {
        return String.format("%s-b%02d-%s", version, buildNumber, releaseType);
    }
    
    /**
     * Get the display version string including MC version and mod loader.
     * Format: v1.0.3-b04-mc1.21.1-forge-release
     */
    public String getDisplayVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s-b%02d", version, buildNumber));
        if (minecraftVersion != null && !"unknown".equals(minecraftVersion)) {
            sb.append("-mc").append(minecraftVersion);
        }
        if (modLoader != null && !"unknown".equals(modLoader)) {
            sb.append("-").append(modLoader);
        }
        sb.append("-").append(releaseType);
        return sb.toString();
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
     * Get the mod loader (e.g., "forge", "neoforge")
     */
    public String getModLoader() {
        return modLoader;
    }
    
    /**
     * Check if this version targets the same Minecraft version and mod loader as another
     */
    public boolean isCompatibleWith(OTAVersion other) {
        if (other == null) return false;
        if (!"unknown".equals(this.minecraftVersion) && !"unknown".equals(other.minecraftVersion)) {
            if (!this.minecraftVersion.equals(other.minecraftVersion)) {
                return false;
            }
        }
        if (!"unknown".equals(this.modLoader) && !"unknown".equals(other.modLoader)) {
            if (!this.modLoader.equals(other.modLoader)) {
                return false;
            }
        }
        return true;
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
     *   New: "v1.0.3-b04-release" or "v1.0.3-b04-mc1.21.1-release"
     *   Old: "v1.0.3-release.3" or "v1.0.3-release.3:1.21.1"
     */
    public static OTAVersion parseFromString(String versionString) {
        try {
            // New format: contains "-b" followed by digits
            if (versionString.contains("-b") && versionString.matches(".*-b\\d+.*")) {
                String[] parts = versionString.split("-");
                String version = parts[0];
                int build = 0;
                String mcVersion = null;
                String loader = null;
                String releaseType = "unknown";
                
                java.util.Set<String> knownLoaders = java.util.Set.of("forge", "neoforge", "fabric", "quilt");
                
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].matches("b\\d+")) {
                        build = Integer.parseInt(parts[i].substring(1));
                    } else if (parts[i].startsWith("mc")) {
                        mcVersion = parts[i].substring(2);
                    } else if (knownLoaders.contains(parts[i].toLowerCase())) {
                        loader = parts[i].toLowerCase();
                    } else {
                        releaseType = parts[i];
                    }
                }
                
                return new OTAVersion(version, build, mcVersion, loader, releaseType, "");
            }
            
            // Old format with optional MC version: "v1.0.3-release.3:1.21.1"
            String mcVersion = null;
            String vPart = versionString;
            if (versionString.contains(":")) {
                String[] colonParts = versionString.split(":", 2);
                vPart = colonParts[0];
                mcVersion = colonParts[1];
            }
            
            String[] parts = vPart.split("\\.");
            if (parts.length >= 4) {
                int build = Integer.parseInt(parts[parts.length - 1]);
                StringBuilder versionBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) versionBuilder.append(".");
                    versionBuilder.append(parts[i]);
                }
                return new OTAVersion(versionBuilder.toString(), build, mcVersion, null, "remote", "");
            }
            
            // Fallback for unrecognized format
            return new OTAVersion(vPart, 0, mcVersion, null, "unknown", "");
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.warn("Failed to parse version string: {}", versionString);
            return new OTAVersion(versionString, 0, null, null, "error", "");
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
        return (version + "." + buildNumber + ":" + minecraftVersion + ":" + modLoader).hashCode();
    }
}
