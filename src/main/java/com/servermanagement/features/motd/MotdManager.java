package com.servermanagement.features.motd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.servermanagement.ServerManagementMod;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MotdManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "motd.json";

    private static MotdManager instance;
    private MinecraftServer server;
    private String motdText = "";
    private boolean initialized = false;

    private MotdManager() {}

    public static MotdManager getInstance() {
        if (instance == null) {
            instance = new MotdManager();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
        load();
        applyMotd();
        this.initialized = true;
    }

    public String getMotdText() {
        return motdText;
    }

    public void setMotdText(String text) {
        this.motdText = text;
        applyMotd();
        save();
    }

    private void applyMotd() {
        if (server == null) {
            return;
        }
        try {
            String rendered = renderMotdForServer(motdText);
            server.setMotd(rendered);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to apply MOTD: {}", e.getMessage(), e);
        }
    }

    /**
     * Converts the stored MOTD text (with &amp;-based formatting codes) into a
     * string with section sign codes for the server MOTD.
     * <p>
     * Vanilla Minecraft resets ALL formatting when a color code is encountered.
     * To allow multiple formatting codes to persist across color changes (e.g.,
     * &amp;l&amp;cHello = bold red), this method re-emits active formatting
     * codes after each color code.
     */
    private String renderMotdForServer(String text) {
        if (text == null || text.isEmpty()) {
            return "A Minecraft Server";
        }
        StringBuilder result = new StringBuilder();
        java.util.Set<Character> activeFormats = new java.util.LinkedHashSet<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    // Color code: emit it, then re-emit active formatting
                    result.append('\u00A7').append(code);
                    for (char f : activeFormats) {
                        result.append('\u00A7').append(f);
                    }
                    i++;
                    continue;
                }
                switch (code) {
                    case 'l': case 'o': case 'n': case 'm': case 'k':
                        activeFormats.add(code);
                        result.append('\u00A7').append(code);
                        i++;
                        continue;
                    case 'r':
                        activeFormats.clear();
                        result.append('\u00A7').append('r');
                        i++;
                        continue;
                }
            }
            result.append(ch);
        }
        return result.toString();
    }

    /**
     * Converts a raw MOTD (potentially with section signs from server.properties)
     * back to ampersand notation for editing.
     */
    public static String toEditableFormat(String rawMotd) {
        if (rawMotd == null) {
            return "";
        }
        return rawMotd.replace('\u00A7', '&');
    }

    private void load() {
        Path dataDir = getDataDirectory();
        Path file = dataDir.resolve(DATA_FILE);
        if (!Files.exists(file)) {
            // Read default MOTD from server if available
            if (server != null) {
                this.motdText = toEditableFormat(server.getMotd());
            }
            return;
        }
        try {
            String json = Files.readString(file);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.has("motd")) {
                this.motdText = obj.get("motd").getAsString();
            }
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load MOTD data: {}", e.getMessage(), e);
        }
    }

    private void save() {
        Path dataDir = getDataDirectory();
        try {
            Files.createDirectories(dataDir);
            Path file = dataDir.resolve(DATA_FILE);
            JsonObject obj = new JsonObject();
            obj.addProperty("dataVersion", 1);
            obj.addProperty("motd", motdText);
            Files.writeString(file, GSON.toJson(obj));
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save MOTD data: {}", e.getMessage(), e);
        }
    }

    private Path getDataDirectory() {
        if (server != null) {
            return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("servermanagement");
        }
        return Path.of("servermanagement");
    }

    public void saveAndShutdown() {
        if (initialized) {
            save();
        }
    }
}
