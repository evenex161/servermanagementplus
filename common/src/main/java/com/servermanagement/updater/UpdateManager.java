package com.servermanagement.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.servermanagement.Constants;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UpdateManager {
    public static boolean justUpdated = false;
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ServerManagementUpdater");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static CompletableFuture<UpdateCheckResult> checkAllUpdates(String currentVersion, String loader, String gameVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Checking for updates across all sources for {} {}...", loader, gameVersion);
                
                String channel = UpdatePreferences.getUpdateChannel(currentVersion);
                
                UpdateInfo modrinthData = queryModrinth(loader, gameVersion, channel);
                UpdateInfo curseforgeData = queryCurseForge(loader, gameVersion, channel);
                
                return new UpdateCheckResult(modrinthData, curseforgeData, currentVersion);
            } catch (Exception e) {
                LOGGER.error("Failed to check for updates", e);
                return new UpdateCheckResult(null, null, currentVersion);
            }
        });
    }

    // Deprecated, maintained for compatibility but routes to new system
    public static CompletableFuture<Optional<UpdateInfo>> checkForUpdates(String currentVersion, String loader, String gameVersion) {
        return checkAllUpdates(currentVersion, loader, gameVersion).thenApply(result -> {
            UpdatePreferences.load();
            UpdateInfo best = result.resolve(UpdatePreferences.getMainSource(), UpdatePreferences.isCheckFallback());
            if (best == null) {
                LOGGER.info("No updates found (Current: {}).", currentVersion);
                return Optional.empty();
            }
            LOGGER.info("Update available: {}", best.version());
            return Optional.of(best);
        });
    }

    public static void checkUpdateSuccessState(String currentVersion) {
        try {
            Path stateFile = java.nio.file.Paths.get("config/servermanagement_update_state.json");
            if (Files.exists(stateFile)) {
                JsonObject json = JsonParser.parseString(Files.readString(stateFile)).getAsJsonObject();
                String lastKnown = json.has("lastKnownVersion") ? json.get("lastKnownVersion").getAsString() : "0.0.0";
                // Simple string comparison for now, in reality you'd want semantic versioning comparison
                if (!lastKnown.equals(currentVersion)) {
                    justUpdated = true;
                }
            } else {
                justUpdated = false; // First run, don't say just updated
            }
            JsonObject newState = new JsonObject();
            newState.addProperty("lastKnownVersion", currentVersion);
            Files.writeString(stateFile, newState.toString());
            
            // Clean up flags from previous updates to prevent smart start script from triggering falsely
            try {
                Path serverRoot = java.nio.file.Paths.get("");
                Files.deleteIfExists(serverRoot.resolve("update_in_progress.flag"));
                Files.deleteIfExists(serverRoot.resolve("update_finished.flag"));
                
                // Clean up old OTA configuration file
                Files.deleteIfExists(serverRoot.resolve("curseforge.properties"));
            } catch (Exception e) {
                LOGGER.warn("Failed to clean up update flags", e);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check update success state", e);
        }
    }

    public static void downloadAndHandoff(String downloadUrl, boolean isClient, 
            Path currentJar,
            java.util.function.BiConsumer<Float, String> progressCallback, 
            Runnable onSuccess, 
            java.util.function.Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Downloading update from {}...", downloadUrl);
                if (progressCallback != null) progressCallback.accept(0.0f, "Connecting...");
                
                java.net.URL url = new java.net.URI(downloadUrl).toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "ServerManagement/Updater");
                long totalBytes = conn.getContentLengthLong();
                
                Path tempFile = Files.createTempFile("ServerManagement-Update-", ".jar");
                try (java.io.InputStream in = conn.getInputStream();
                     java.io.OutputStream out = Files.newOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    long downloadedBytes = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloadedBytes += read;
                        if (progressCallback != null && totalBytes > 0) {
                            progressCallback.accept((float) downloadedBytes / totalBytes, "Downloading update...");
                        }
                    }
                }
                
                LOGGER.info("Update downloaded to {}", tempFile);
                if (progressCallback != null) progressCallback.accept(1.0f, "Download complete!");
                
                boolean success = executeUpdaterHandoff(tempFile, currentJar, isClient);
                if (success) {
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.accept("Dev environment: updater.jar missing.");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to download update", e);
                if (onError != null) onError.accept(e.getMessage());
            }
        });
    }

    private static UpdateInfo queryModrinth(String loader, String gameVersion, String channel) {
        if (Constants.MODRINTH_PROJECT_ID.equals("YOUR_MODRINTH_ID")) return null;
        try {
            String url = String.format("https://api.modrinth.com/v2/project/%s/version?loaders=%s&game_versions=%s",
                    Constants.MODRINTH_PROJECT_ID, 
                    URLEncoder.encode("[\"" + loader.toLowerCase() + "\"]", StandardCharsets.UTF_8),
                    URLEncoder.encode("[\"" + gameVersion + "\"]", StandardCharsets.UTF_8));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "ServerManagement/Updater")
                    .GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
                for (int i = 0; i < versions.size(); i++) {
                    JsonObject latest = versions.get(i).getAsJsonObject();
                    String type = latest.has("version_type") ? latest.get("version_type").getAsString() : "release";
                    if (channel.equals("release") && !type.equals("release")) continue;
                    if (channel.equals("beta") && type.equals("alpha")) continue;
                    
                    String versionNumber = latest.get("version_number").getAsString();
                    String dlUrl = latest.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();
                    String changelog = latest.has("changelog") && !latest.get("changelog").isJsonNull() ? latest.get("changelog").getAsString() : "No changelog provided.";
                    String date = latest.has("date_published") ? latest.get("date_published").getAsString() : "Unknown date";
                    return new UpdateInfo(versionNumber, dlUrl, changelog, date, "Modrinth");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Modrinth query failed", e);
        }
        return null;
    }

    private static UpdateInfo queryCurseForge(String loader, String gameVersion, String channel) {
        if (Constants.CURSEFORGE_PROJECT_ID.equals("YOUR_CURSEFORGE_ID")) return null;
        try {
            String url = String.format("https://api.curseforge.com/v1/mods/%s/files?gameVersion=%s",
                    Constants.CURSEFORGE_PROJECT_ID, gameVersion);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-api-key", Constants.CURSEFORGE_API_KEY != null ? Constants.CURSEFORGE_API_KEY : "")
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray files = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
                for (int i = 0; i < files.size(); i++) {
                    JsonObject file = files.get(i).getAsJsonObject();
                    
                    int rt = file.has("releaseType") ? file.get("releaseType").getAsInt() : 1;
                    if (channel.equals("release") && rt != 1) continue;
                    if (channel.equals("beta") && rt == 3) continue;

                    JsonArray gameVersions = file.getAsJsonArray("gameVersions");
                    boolean matchesLoader = false;
                    for (int j = 0; j < gameVersions.size(); j++) {
                        String gv = gameVersions.get(j).getAsString().toLowerCase();
                        if (gv.contains(loader.toLowerCase())) matchesLoader = true;
                    }
                    if (matchesLoader && file.has("downloadUrl") && !file.get("downloadUrl").isJsonNull()) {
                        String dlUrl = file.get("downloadUrl").getAsString();
                        String displayName = file.get("displayName").getAsString();
                        String date = file.has("fileDate") ? file.get("fileDate").getAsString() : "Unknown date";
                        return new UpdateInfo(displayName, dlUrl, "Changelog available on CurseForge.", date, "CurseForge");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CurseForge query failed", e);
        }
        return null;
    }

    public static boolean isNewerVersion(String remote, String current) {
        if (remote == null || current == null || remote.equals(current)) return false;
        
        // Normalize "2.1.0b1" to "2.1.0-b1" for consistent splitting
        String rNorm = remote.replaceAll("([0-9])b([0-9])", "$1-b$2");
        String cNorm = current.replaceAll("([0-9])b([0-9])", "$1-b$2");
        
        String[] rParts = rNorm.split("-");
        String[] cParts = cNorm.split("-");
        
        String[] rNums = rParts[0].split("\\.");
        String[] cNums = cParts[0].split("\\.");
        
        int len = Math.max(rNums.length, cNums.length);
        for (int i = 0; i < len; i++) {
            int r = i < rNums.length ? tryParseInt(rNums[i]) : 0;
            int c = i < cNums.length ? tryParseInt(cNums[i]) : 0;
            if (r != c) return r > c;
        }
        
        // Base versions match. Full releases (no dash) are newer than pre-releases
        if (rParts.length == 1 && cParts.length > 1) return true;
        if (rParts.length > 1 && cParts.length == 1) return false;
        
        if (rParts.length > 1 && cParts.length > 1) {
            int rPre = tryParseInt(rParts[1]);
            int cPre = tryParseInt(cParts[1]);
            if (rPre != cPre) return rPre > cPre;
            return rParts[1].compareTo(cParts[1]) > 0;
        }
        
        return false;
    }

    private static int tryParseInt(String val) {
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean executeUpdaterHandoff(Path newJar, Path currentJar, boolean isClient) {
        try {
            Path tempUpdater = Files.createTempFile("sm-updater", ".jar");
            
            // Extract embedded updater.jar
            try (var in = UpdateManager.class.getResourceAsStream("/assets/servermanagement/updater.jar")) {
                if (in == null) {
                    LOGGER.warn("Embedded updater.jar not found! This is normal in development environments. Skipping updater handoff.");
                    return false;
                }
                Files.copy(in, tempUpdater, StandardCopyOption.REPLACE_EXISTING);
            }

            long pid = ProcessHandle.current().pid();
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                javaBin += ".exe";
            }

            ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-jar", tempUpdater.toAbsolutePath().toString(),
                String.valueOf(pid),
                currentJar.toAbsolutePath().toString(),
                newJar.toAbsolutePath().toString(),
                String.valueOf(isClient)
            );
            
            if (isClient) {
                // Append current JVM args and launch command to reboot client
                String cmdLine = System.getProperty("sun.java.command");
                if (cmdLine != null && !cmdLine.isEmpty()) pb.command().add(cmdLine);
            } else {
                // If it's a server, try to append the start command if available, otherwise it relies on wrapper loop
                String cmdLine = System.getProperty("sun.java.command");
                if (cmdLine != null && !cmdLine.isEmpty()) pb.command().add(cmdLine);
            }

            // Redirect JVM output to help diagnose launch failures
            if (currentJar.getParent() != null) {
                pb.redirectError(currentJar.getParent().resolve("updater_jvm_error.log").toFile());
                pb.redirectOutput(currentJar.getParent().resolve("updater_jvm_output.log").toFile());
            }

            // Create flag for smart restart scripts
            Path serverRoot = currentJar.getParent().getParent();
            if (serverRoot != null) {
                Files.writeString(serverRoot.resolve("update_in_progress.flag"), "update in progress");
            }

            pb.start();
            LOGGER.info("Handoff complete, signaling server shutdown.");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to execute updater handoff", e);
            return false;
        }
    }
}
