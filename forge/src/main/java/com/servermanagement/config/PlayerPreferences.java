package com.servermanagement.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<UUID, PlayerPref> preferences = new HashMap<>();
    private static File configFile;
    private static int dataVersion = DataVersion.CURRENT_VERSION;

    public static void initialize(MinecraftServer server) {
        File configDir = new java.io.File(server.getServerDirectory(), "config/servermanagement");
        configDir.mkdirs();
        configFile = new File(configDir, "player_preferences.json");
        load();
    }

    public static boolean getAutoShow(UUID playerId) {
        return preferences.getOrDefault(playerId, new PlayerPref()).autoShowConfig;
    }

    public static void setAutoShow(UUID playerId, boolean autoShow) {
        PlayerPref pref = preferences.getOrDefault(playerId, new PlayerPref());
        pref.autoShowConfig = autoShow;
        preferences.put(playerId, pref);
        save();
    }

    private static void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                PlayerPrefData data = GSON.fromJson(reader, PlayerPrefData.class);
                if (data != null && data.preferences != null) {
                    preferences = data.preferences;
                    
                    // Check data version
                    if (data.dataVersion == 0) {
                        ServerManagementMod.LOGGER.debug("Migrating legacy PlayerPreferences data to version {}", 
                            DataVersion.CURRENT_VERSION);
                    } else if (data.dataVersion < DataVersion.CURRENT_VERSION) {
                        ServerManagementMod.LOGGER.debug("Migrating PlayerPreferences data from version {} to {}", 
                            data.dataVersion, DataVersion.CURRENT_VERSION);
                    } else if (data.dataVersion > DataVersion.CURRENT_VERSION) {
                        ServerManagementMod.LOGGER.error("PlayerPreferences data version {} is newer than supported version {}!", 
                            data.dataVersion, DataVersion.CURRENT_VERSION);
                    }
                    
                    dataVersion = DataVersion.CURRENT_VERSION;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            PlayerPrefData data = new PlayerPrefData();
            data.dataVersion = dataVersion;
            data.preferences = preferences;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PlayerPrefData {
        int dataVersion = DataVersion.CURRENT_VERSION;
        Map<UUID, PlayerPref> preferences = new HashMap<>();
    }

    private static class PlayerPref {
        boolean autoShowConfig = false;
    }
}
