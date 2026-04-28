package com.servermanagement.ota;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.servermanagement.ServerManagementMod;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Checks CurseForge for the latest mod version
 * CurseForge Project: https://www.curseforge.com/minecraft/mc-mods/servermanagement
 */
public class CurseForgeUpdateChecker {
    
    // CurseForge project ID
    private static final int PROJECT_ID = 1381899;
    private static final String CURSEFORGE_API = "https://api.curseforge.com/v1/mods/%d/files";
    
    // API key loaded from secure config file
    private static String API_KEY = null;
    private static boolean ENABLED = false;
    private static long CACHE_DURATION = 3600000; // 1 hour default
    
    // Cache to prevent excessive API calls
    private static UpdateInfo cachedResult = null;
    private static long lastCheckTime = 0;
    
    static {
        loadConfiguration();
    }
    
    /**
     * Load API key and settings from secure configuration file
     */
    private static void loadConfiguration() {
        try {
            // Try multiple locations for the config file
            Path[] configPaths = {
                Paths.get("curseforge.properties"),
                Paths.get("config", "curseforge.properties"),
                Paths.get("..", "curseforge.properties")
            };
            
            Properties props = new Properties();
            boolean configFound = false;
            
            for (Path configPath : configPaths) {
                if (Files.exists(configPath)) {
                    try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                        props.load(fis);
                        configFound = true;
                        ServerManagementMod.LOGGER.info("Loaded CurseForge configuration from: {}", 
                            configPath.toAbsolutePath());
                        break;
                    }
                }
            }
            
            if (!configFound) {
                createTemplateConfigFile();
                ServerManagementMod.LOGGER.warn("CurseForge configuration file not found. Update checking disabled.");
                ServerManagementMod.LOGGER.info("A template 'curseforge.properties' has been created in your server root.");
                ServerManagementMod.LOGGER.info("Edit it and paste your CurseForge API key to enable update checking.");
                return;
            }
            
            // Load settings
            API_KEY = props.getProperty("curseforge.api.key");
            ENABLED = Boolean.parseBoolean(props.getProperty("curseforge.enabled", "true"));
            CACHE_DURATION = Long.parseLong(props.getProperty("curseforge.cache.duration", "3600000"));
            
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                ServerManagementMod.LOGGER.warn("CurseForge API key not configured. Update checking disabled.");
                ENABLED = false;
            } else {
                // Mask API key in logs for security
                String maskedKey = API_KEY.substring(0, Math.min(10, API_KEY.length())) + "***";
                ServerManagementMod.LOGGER.info("CurseForge API configured (Key: {}...)", maskedKey);
                ServerManagementMod.LOGGER.info("CurseForge update checking: {}", ENABLED ? "ENABLED" : "DISABLED");
            }
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to load CurseForge configuration", e);
            ENABLED = false;
        }
    }
    
    /**
     * Creates a template curseforge.properties file in the server root so the
     * operator knows exactly where to put their API key without having to read docs.
     */
    private static void createTemplateConfigFile() {
        Path targetPath = Paths.get("curseforge.properties");
        if (Files.exists(targetPath)) {
            return; // Another thread beat us, nothing to do
        }
        
        String template =
            "# CurseForge API Configuration for ServerManagement+\n" +
            "# KEEP THIS FILE SECURE - DO NOT COMMIT TO VERSION CONTROL!\n" +
            "# Add this file to .gitignore\n" +
            "#\n" +
            "# How to obtain your API key:\n" +
            "#   1. Log in at https://www.curseforge.com\n" +
            "#   2. Go to Account Settings > API Keys\n" +
            "#   3. Generate a new key and paste it below.\n" +
            "#   4. Set curseforge.enabled=true to activate update checking.\n" +
            "\n" +
            "# Paste your CurseForge API key here\n" +
            "curseforge.api.key=\n" +
            "\n" +
            "# Set to true once your API key is filled in\n" +
            "curseforge.enabled=false\n" +
            "\n" +
            "# Cache duration in milliseconds (default: 1 hour = 3600000)\n" +
            "curseforge.cache.duration=3600000\n";
        
        try {
            Files.writeString(targetPath, template, StandardCharsets.UTF_8);
            ServerManagementMod.LOGGER.info("Created template CurseForge config at: {}",
                targetPath.toAbsolutePath());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Could not create template curseforge.properties: {}", e.getMessage());
        }
    }
    
    /**
     * Check CurseForge for available updates (async)
     */
    public static CompletableFuture<UpdateInfo> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if enabled
                if (!ENABLED) {
                    ServerManagementMod.LOGGER.debug("CurseForge update checking is disabled");
                    return new UpdateInfo(false, null, null, null);
                }
                
                // Check cache
                long now = System.currentTimeMillis();
                if (cachedResult != null && (now - lastCheckTime) < CACHE_DURATION) {
                    ServerManagementMod.LOGGER.debug("Using cached CurseForge result ({}ms old)", 
                        now - lastCheckTime);
                    return cachedResult;
                }
                
                OTAVersion currentVersion = OTAVersion.loadFromResources();
                
                ServerManagementMod.LOGGER.info("Checking CurseForge for updates...");
                ServerManagementMod.LOGGER.info("Current version: {} (MC {})", currentVersion.getDisplayVersion(), currentVersion.getMinecraftVersion());
                
                // Get latest file from CurseForge
                CurseForgeFile latestFile = getLatestFile();
                
                if (latestFile == null) {
                    ServerManagementMod.LOGGER.warn("No files found on CurseForge");
                    UpdateInfo result = new UpdateInfo(false, null, null, null);
                    cachedResult = result;
                    lastCheckTime = now;
                    return result;
                }
                
                // Parse version from filename or display name
                OTAVersion remoteVersion = parseVersionFromFile(latestFile);
                
                boolean updateAvailable = remoteVersion.isNewerThan(currentVersion);
                
                if (updateAvailable) {
                    ServerManagementMod.LOGGER.info("Update available on CurseForge!");
                    ServerManagementMod.LOGGER.info("Latest version: {}", remoteVersion.getDisplayVersion());
                    ServerManagementMod.LOGGER.info("Download URL: {}", latestFile.downloadUrl);
                    
                    UpdateInfo result = new UpdateInfo(true, remoteVersion, latestFile.downloadUrl, latestFile.fileName);
                    cachedResult = result;
                    lastCheckTime = now;
                    return result;
                } else {
                    ServerManagementMod.LOGGER.info("No updates available (current version is latest)");
                    UpdateInfo result = new UpdateInfo(false, remoteVersion, null, null);
                    cachedResult = result;
                    lastCheckTime = now;
                    return result;
                }
                
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to check CurseForge for updates", e);
                return new UpdateInfo(false, null, null, null);
            }
        });
    }
    
    /**
     * Get the latest file for our Minecraft version from CurseForge
     */
    private static CurseForgeFile getLatestFile() {
        if (!ENABLED || API_KEY == null) {
            return null;
        }
        
        // Get MC version from OTA properties
        String minecraftVersion = OTAVersion.loadFromResources().getMinecraftVersion();
        
        try {
            String apiUrl = String.format(CURSEFORGE_API, PROJECT_ID);
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000); // 10 second timeout
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonArray files = json.getAsJsonArray("data");
                
                ServerManagementMod.LOGGER.debug("Found {} files on CurseForge", files.size());
                
                // Find latest file for our Minecraft version
                for (int i = 0; i < files.size(); i++) {
                    JsonObject file = files.get(i).getAsJsonObject();
                    JsonArray gameVersions = file.getAsJsonArray("gameVersions");
                    
                    boolean matchesVersion = false;
                    for (int j = 0; j < gameVersions.size(); j++) {
                        if (gameVersions.get(j).getAsString().equals(minecraftVersion)) {
                            matchesVersion = true;
                            break;
                        }
                    }
                    
                    if (matchesVersion) {
                        ServerManagementMod.LOGGER.debug("Latest file: {}", file.get("fileName").getAsString());
                        return new CurseForgeFile(
                            file.get("id").getAsInt(),
                            file.get("fileName").getAsString(),
                            file.get("displayName").getAsString(),
                            file.get("downloadUrl").getAsString(),
                            file.get("fileLength").getAsLong()
                        );
                    }
                }
                
                ServerManagementMod.LOGGER.warn("No files found for Minecraft {}", minecraftVersion);
            } else if (responseCode == 403) {
                ServerManagementMod.LOGGER.error("CurseForge API authentication failed - Invalid API key");
            } else if (responseCode == 404) {
                ServerManagementMod.LOGGER.error("CurseForge project not found - Check PROJECT_ID");
            } else {
                ServerManagementMod.LOGGER.error("CurseForge API error: HTTP {}", responseCode);
                
                // Try to read error response
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    ServerManagementMod.LOGGER.error("Error details: {}", errorResponse.toString());
                } catch (Exception e) {
                    // Ignore error reading error stream
                }
            }
            
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Network error connecting to CurseForge API", e);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Error fetching from CurseForge API", e);
        }
        
        return null;
    }
    
    /**
     * Parse version from CurseForge file info
     */
    private static OTAVersion parseVersionFromFile(CurseForgeFile file) {
        // Try to extract version from filename
        // e.g., "servermanagement-1.0.0.2.jar" -> "1.0.0.2"
        String filename = file.fileName.toLowerCase();
        
        // Remove extension
        filename = filename.replace(".jar", "");
        
        // Try to find version pattern
        String[] parts = filename.split("-");
        if (parts.length >= 2) {
            String versionPart = parts[parts.length - 1];
            return OTAVersion.parseFromString(versionPart);
        }
        
        // Fallback: use display name
        return OTAVersion.parseFromString(file.displayName);
    }
    
    /**
     * Update information from CurseForge
     */
    public static class UpdateInfo {
        public final boolean updateAvailable;
        public final OTAVersion version;
        public final String downloadUrl;
        public final String fileName;
        
        public UpdateInfo(boolean updateAvailable, OTAVersion version, String downloadUrl, String fileName) {
            this.updateAvailable = updateAvailable;
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
        }
    }
    
    /**
     * CurseForge file information
     */
    private static class CurseForgeFile {
        final int id;
        final String fileName;
        final String displayName;
        final String downloadUrl;
        final long fileLength;
        
        CurseForgeFile(int id, String fileName, String displayName, String downloadUrl, long fileLength) {
            this.id = id;
            this.fileName = fileName;
            this.displayName = displayName;
            this.downloadUrl = downloadUrl;
            this.fileLength = fileLength;
        }
    }
}
