package com.servermanagement.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    public static void load() {
        if (!PREFS_FILE.exists()) return;
        try (FileReader reader = new FileReader(PREFS_FILE)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            if (array != null) {
                skippedVersions.clear();
                for (JsonElement el : array) {
                    skippedVersions.add(el.getAsString());
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
                JsonArray array = new JsonArray();
                for (String v : skippedVersions) {
                    array.add(v);
                }
                GSON.toJson(array, writer);
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
}
