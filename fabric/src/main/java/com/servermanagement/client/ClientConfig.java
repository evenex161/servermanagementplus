package com.servermanagement.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.servermanagement.ServerManagementMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    private static final Map<String, Boolean> skipBlacklistDeleteWarnings = new HashMap<>();
    private static int statsBarX = 10;
    private static int statsBarY = 10;
    private static boolean showStatsBar = true;

    public static void init(File gameDir) {
        configFile = new File(gameDir, "config/servermanagement_client.json");
        load();
    }

    private static void load() {
        if (configFile != null && configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("statsBarX")) statsBarX = json.get("statsBarX").getAsInt();
                    if (json.has("statsBarY")) statsBarY = json.get("statsBarY").getAsInt();
                    if (json.has("showStatsBar")) showStatsBar = json.get("showStatsBar").getAsBoolean();
                }
                if (json != null && json.has("skipBlacklistDeleteWarnings")) {
                    JsonObject warnings = json.getAsJsonObject("skipBlacklistDeleteWarnings");
                    for (Map.Entry<String, JsonElement> entry : warnings.entrySet()) {
                        skipBlacklistDeleteWarnings.put(entry.getKey(), entry.getValue().getAsBoolean());
                    }
                }
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Failed to load client config", e);
            }
        }
    }

    public static void save() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject json = new JsonObject();
            JsonObject warnings = new JsonObject();
            for (Map.Entry<String, Boolean> entry : skipBlacklistDeleteWarnings.entrySet()) {
                warnings.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("skipBlacklistDeleteWarnings", warnings);
            json.addProperty("statsBarX", statsBarX);
            json.addProperty("statsBarY", statsBarY);
            json.addProperty("showStatsBar", showStatsBar);
            GSON.toJson(json, writer);
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save client config", e);
        }
    }
    
    public static String getCurrentServerId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            return "singleplayer";
        }
        ServerData data = mc.getCurrentServer();
        if (data != null) {
            return data.ip;
        }
        return "unknown";
    }
    
    public static boolean shouldSkipBlacklistWarning() {
        return skipBlacklistDeleteWarnings.getOrDefault(getCurrentServerId(), false);
    }
    
    public static int getStatsBarX() { return statsBarX; }
    public static void setStatsBarX(int x) { statsBarX = x; save(); }
    public static int getStatsBarY() { return statsBarY; }
    public static void setStatsBarY(int y) { statsBarY = y; save(); }
    public static boolean isShowStatsBar() { return showStatsBar; }
    public static void setShowStatsBar(boolean show) { showStatsBar = show; save(); }
    
    public static void setSkipBlacklistWarning(boolean skip) {
        skipBlacklistDeleteWarnings.put(getCurrentServerId(), skip);
        save();
    }
}
