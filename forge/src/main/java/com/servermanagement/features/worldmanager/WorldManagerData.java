package com.servermanagement.features.worldmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class WorldManagerData {
    // Data version for migration support
    private int dataVersion = DataVersion.CURRENT_VERSION;
    
    // Legacy field kept for migration from old format
    private Map<String, Boolean> portalsEnabled = new HashMap<>();
    
    // Granular per-dimension portal controls
    private Map<String, Boolean> netherPortalsEnabled = new HashMap<>();
    private Map<String, Boolean> endPortalsEnabled = new HashMap<>();
    
    private Map<String, ChatConnection> chatConnections = new HashMap<>();
    private Map<String, Integer> timerSeconds = new HashMap<>();
    private Map<String, Long> timerStartTime = new HashMap<>();
    private Map<String, Boolean> timerEnablesPortal = new HashMap<>(); // true = enable when done, false = disable when done
    private Map<String, String> timerPortalType = new HashMap<>(); // "nether", "end", or "both"
    private boolean chatIsolationEnabled = false;
    private boolean tabIsolationEnabled = false;
    private LobbySpawn lobbySpawn = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static WorldManagerData load(MinecraftServer server) {
        File file = getDataFile(server);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                // Validate file is not empty
                if (file.length() == 0) {
                    com.servermanagement.ServerManagementMod.LOGGER.warn("WorldManagerData file is empty, creating backup and using defaults");
                    backupCorruptedFile(file);
                    return new WorldManagerData();
                }
                
                WorldManagerData data = GSON.fromJson(reader, WorldManagerData.class);
                
                // Validate loaded data
                if (data == null) {
                    com.servermanagement.ServerManagementMod.LOGGER.warn("Failed to parse WorldManagerData, creating backup and using defaults");
                    backupCorruptedFile(file);
                    return new WorldManagerData();
                }
                
                // Check version and migrate if needed
                if (data.dataVersion == 0) {
                    com.servermanagement.ServerManagementMod.LOGGER.info("Migrating legacy WorldManager data to version {}", DataVersion.CURRENT_VERSION);
                    data.dataVersion = DataVersion.CURRENT_VERSION;
                } else if (data.dataVersion < DataVersion.CURRENT_VERSION) {
                    com.servermanagement.ServerManagementMod.LOGGER.info("Migrating WorldManager data from version {} to {}", 
                        data.dataVersion, DataVersion.CURRENT_VERSION);
                    data.dataVersion = DataVersion.CURRENT_VERSION;
                } else if (data.dataVersion > DataVersion.CURRENT_VERSION) {
                    com.servermanagement.ServerManagementMod.LOGGER.error("WorldManager data version {} is newer than supported version {}!",
                        data.dataVersion, DataVersion.CURRENT_VERSION);
                }
                
                // Ensure maps are initialized (in case JSON was partial)
                if (data.portalsEnabled == null) {
                    data.portalsEnabled = new HashMap<>();
                }
                if (data.netherPortalsEnabled == null) {
                    data.netherPortalsEnabled = new HashMap<>();
                }
                if (data.endPortalsEnabled == null) {
                    data.endPortalsEnabled = new HashMap<>();
                }
                if (data.chatConnections == null) {
                    com.servermanagement.ServerManagementMod.LOGGER.warn("Chat connections data was null, initializing empty map");
                    data.chatConnections = new HashMap<>();
                }
                if (data.timerSeconds == null) {
                    data.timerSeconds = new HashMap<>();
                }
                if (data.timerStartTime == null) {
                    data.timerStartTime = new HashMap<>();
                }
                if (data.timerEnablesPortal == null) {
                    data.timerEnablesPortal = new HashMap<>();
                }
                if (data.timerPortalType == null) {
                    data.timerPortalType = new HashMap<>();
                }
                
                // Migration: convert legacy portalsEnabled to granular nether/end maps
                if (!data.portalsEnabled.isEmpty() && data.netherPortalsEnabled.isEmpty() && data.endPortalsEnabled.isEmpty()) {
                    com.servermanagement.ServerManagementMod.LOGGER.info("Migrating legacy portalsEnabled to granular nether/end portal maps");
                    data.netherPortalsEnabled.putAll(data.portalsEnabled);
                    data.endPortalsEnabled.putAll(data.portalsEnabled);
                    data.portalsEnabled.clear();
                }
                
                com.servermanagement.ServerManagementMod.LOGGER.info("Successfully loaded WorldManagerData from: {}", file.getAbsolutePath());
                return data;
                
            } catch (com.google.gson.JsonSyntaxException e) {
                com.servermanagement.ServerManagementMod.LOGGER.error("WorldManagerData file has invalid JSON syntax, creating backup and using defaults", e);
                backupCorruptedFile(file);
                return new WorldManagerData();
            } catch (Exception e) {
                com.servermanagement.ServerManagementMod.LOGGER.error("Failed to load WorldManagerData, creating backup and using defaults", e);
                backupCorruptedFile(file);
                return new WorldManagerData();
            }
        }
        com.servermanagement.ServerManagementMod.LOGGER.info("WorldManagerData file does not exist, creating with defaults");
        return new WorldManagerData();
    }
    
    /**
     * Creates a backup of a corrupted data file before resetting it.
     */
    private static void backupCorruptedFile(File file) {
        try {
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backupFile = new File(file.getParentFile(), file.getName() + ".corrupted_" + timestamp);
            
            java.nio.file.Files.copy(file.toPath(), backupFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            com.servermanagement.ServerManagementMod.LOGGER.info("Created backup of corrupted file: {}", backupFile.getName());
            
            // Delete the corrupted file so a fresh one will be created
            if (file.delete()) {
                com.servermanagement.ServerManagementMod.LOGGER.info("Deleted corrupted data file");
            }
        } catch (Exception e) {
            com.servermanagement.ServerManagementMod.LOGGER.error("Failed to backup corrupted file", e);
        }
    }

    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
            com.servermanagement.ServerManagementMod.LOGGER.debug("Saved WorldManagerData");
        } catch (Exception e) {
            com.servermanagement.ServerManagementMod.LOGGER.error("Failed to save WorldManagerData!", e);
            e.printStackTrace();
        }
    }

    private static File getDataFile(MinecraftServer server) {
        return new File(server.getServerDirectory(), "config/servermanagement/world_manager.json");
    }

    public boolean areNetherPortalsEnabled(String dimensionId) {
        return netherPortalsEnabled.getOrDefault(dimensionId, true);
    }

    public void setNetherPortalsEnabled(String dimensionId, boolean enabled) {
        netherPortalsEnabled.put(dimensionId, enabled);
    }

    public boolean areEndPortalsEnabled(String dimensionId) {
        return endPortalsEnabled.getOrDefault(dimensionId, true);
    }

    public void setEndPortalsEnabled(String dimensionId, boolean enabled) {
        endPortalsEnabled.put(dimensionId, enabled);
    }

    public boolean isChatIsolationEnabled() {
        return chatIsolationEnabled;
    }

    public void setChatIsolationEnabled(boolean enabled) {
        chatIsolationEnabled = enabled;
    }

    public boolean isTabIsolationEnabled() {
        return tabIsolationEnabled;
    }

    public void setTabIsolationEnabled(boolean enabled) {
        tabIsolationEnabled = enabled;
    }

    public LobbySpawn getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(LobbySpawn spawn) {
        lobbySpawn = spawn;
    }

    public void clearLobbySpawn() {
        lobbySpawn = null;
    }

    public Map<String, ChatConnection> getChatConnections() {
        return chatConnections;
    }
    
    public java.util.List<String> getChatConnections(String dimensionId) {
        java.util.List<String> connections = new java.util.ArrayList<>();
        for (Map.Entry<String, ChatConnection> entry : chatConnections.entrySet()) {
            if (entry.getKey().equals(dimensionId) && entry.getValue().connected) {
                connections.add(entry.getKey());
            }
        }
        return connections;
    }
    
    // Timer methods
    public void setTimerSeconds(String dimensionId, int seconds) {
        timerSeconds.put(dimensionId, seconds);
        if (seconds > 0 && !timerStartTime.containsKey(dimensionId)) {
            timerStartTime.put(dimensionId, System.currentTimeMillis() / 1000);
        }
    }
    
    public int getTimerSeconds(String dimensionId) {
        return timerSeconds.getOrDefault(dimensionId, 0);
    }
    
    public boolean hasActiveTimer(String dimensionId) {
        return timerSeconds.containsKey(dimensionId) && timerSeconds.get(dimensionId) > 0;
    }
    
    public long getRemainingTime(String dimensionId) {
        return timerSeconds.getOrDefault(dimensionId, 0);
    }
    
    public void clearTimer(String dimensionId) {
        timerSeconds.remove(dimensionId);
        timerStartTime.remove(dimensionId);
        timerEnablesPortal.remove(dimensionId);
        timerPortalType.remove(dimensionId);
    }
    
    public void setTimerEnablesPortal(String dimensionId, boolean enables) {
        timerEnablesPortal.put(dimensionId, enables);
    }
    
    public boolean getTimerEnablesPortal(String dimensionId) {
        return timerEnablesPortal.getOrDefault(dimensionId, true); // Default to enabling
    }

    public void setTimerPortalType(String dimensionId, String portalType) {
        timerPortalType.put(dimensionId, portalType);
    }

    public String getTimerPortalType(String dimensionId) {
        return timerPortalType.getOrDefault(dimensionId, "both");
    }

    /**
     * Checks if ANY dimension has an active timer.
     */
    public boolean hasAnyActiveTimer() {
        for (Map.Entry<String, Integer> entry : timerSeconds.entrySet()) {
            if (entry.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the dimension ID of the currently active timer, or null if none.
     */
    public String getActiveTimerDimension() {
        for (Map.Entry<String, Integer> entry : timerSeconds.entrySet()) {
            if (entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static class LobbySpawn {
        public double x, y, z;
        public String dimension;
        public float yaw, pitch;

        public LobbySpawn(double x, double y, double z, String dimension, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static class ChatConnection {
        public boolean connected;

        public ChatConnection(boolean connected) {
            this.connected = connected;
        }
    }
}
