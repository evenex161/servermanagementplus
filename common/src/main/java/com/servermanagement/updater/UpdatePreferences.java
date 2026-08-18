package com.servermanagement.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.servermanagement.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class UpdatePreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PREFS_FILE = new File("config/servermanagement_update_prefs.json");
    private static final Set<String> skippedVersions = new HashSet<>();
    private static String mainSource = "ask";
    private static boolean checkFallback = true;
    private static String updateChannel = "";

    public static void load() {
        if (!PREFS_FILE.exists()) return;
        try (FileReader reader = new FileReader(PREFS_FILE)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null) {
                skippedVersions.clear();
                if (root.isJsonArray()) {
                    // Legacy migration
                    for (JsonElement el : root.getAsJsonArray()) {
                        skippedVersions.add(el.getAsString());
                    }
                    save(); // Upgrade format immediately
                } else if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("skippedVersions")) {
                        for (JsonElement el : obj.getAsJsonArray("skippedVersions")) {
                            skippedVersions.add(el.getAsString());
                        }
                    }
                    if (obj.has("mainSource")) mainSource = obj.get("mainSource").getAsString();
                    if (obj.has("checkFallback")) checkFallback = obj.get("checkFallback").getAsBoolean();
                    if (obj.has("updateChannel")) updateChannel = obj.get("updateChannel").getAsString();
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load update preferences", e);
        }
    }

    public static void save() {
        try {
            PREFS_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(PREFS_FILE)) {
                JsonObject obj = new JsonObject();
                JsonArray array = new JsonArray();
                for (String v : skippedVersions) {
                    array.add(v);
                }
                obj.add("skippedVersions", array);
                obj.addProperty("mainSource", mainSource);
                obj.addProperty("checkFallback", checkFallback);
                obj.addProperty("updateChannel", updateChannel);
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save update preferences", e);
        }
    }

    public static boolean isSkipped(String version) {
        return skippedVersions.contains(version);
    }

    public static void skipVersion(String version) {
        skippedVersions.add(version);
        save();
    }

    public static String getMainSource() {
        return mainSource;
    }

    public static void setMainSource(String source) {
        mainSource = source;
    }

    public static boolean isCheckFallback() {
        return checkFallback;
    }

    public static void setCheckFallback(boolean check) {
        checkFallback = check;
    }

    public static String getUpdateChannel(String currentVersion) {
        if (updateChannel.isEmpty()) {
            if (currentVersion != null && (currentVersion.contains("-b") || currentVersion.contains("beta"))) {
                updateChannel = "beta";
            } else {
                updateChannel = "release";
            }
            save();
        }
        return updateChannel;
    }

    public static void setUpdateChannel(String channel) {
        updateChannel = channel;
    }
}
