package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.ModFileRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side manager for OTA (Over-The-Air) mod updates.
 * Handles version checking, file downloading, verification, and installation.
 */
public class OTAUpdateManager {
    
    private static boolean updateInProgress = false;
    private static String targetVersion = null;
    private static String expectedHash = null;
    private static long expectedSize = 0;
    private static String targetMinecraftVersion = null;
    private static List<byte[]> receivedChunks = new ArrayList<>();
    private static int totalChunks = 0;
    private static boolean userAcceptedUpdate = false;
    private static OTAUpdateScreen updateScreen = null;
    
    /**
     * Handle version mismatch detected by server
     */
    public static void handleVersionMismatch(String clientVersion, String serverVersion,
                                            int serverDataVersion, String serverModJarName,
                                            String serverModJarHash, long serverModJarSize,
                                            String serverMinecraftVersion, String serverModLoader) {
        ServerManagementMod.LOGGER.debug("handleVersionMismatch called, updateInProgress={}, loader={}", updateInProgress, serverModLoader);
        
        if (updateInProgress) {
            ServerManagementMod.LOGGER.warn("Update already in progress, ignoring new version mismatch");
            return; // Already handling an update
        }
        
        ServerManagementMod.LOGGER.info("Version mismatch: client {} -> server {} (file: {}, size: {} bytes)",
            clientVersion, serverVersion, serverModJarName, serverModJarSize);
        
        // Show update screen
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            updateScreen = new OTAUpdateScreen(clientVersion, serverVersion, serverModJarSize);
            minecraft.setScreen(updateScreen);
            ServerManagementMod.LOGGER.debug("OTA Update screen displayed");
        });
        
        // Show chat message to player (if already in game)
        LocalPlayer player = minecraft.player;
        
        if (player != null) {
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            player.displayClientMessage(Component.literal("§6§l⚠ MOD VERSION MISMATCH"), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("§7Your version: §c" + clientVersion), false);
            player.displayClientMessage(Component.literal("§7Server version: §a" + serverVersion), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("§eAn automatic update is available!"), false);
            player.displayClientMessage(Component.literal("§7File: §f" + serverModJarName), false);
            player.displayClientMessage(Component.literal("§7Size: §f" + formatFileSize(serverModJarSize)), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("§aThe update will download automatically."), false);
            player.displayClientMessage(Component.literal("§7Please wait while the update downloads..."), false);
            player.displayClientMessage(Component.literal("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            player.displayClientMessage(Component.literal(""), false);
        } else {
            ServerManagementMod.LOGGER.warn("Player is null, cannot send chat messages");
        }
        
        // Auto-start update (user has implicitly accepted by joining)
        ServerManagementMod.LOGGER.info("Starting auto-update to version {}", serverVersion);
        startUpdate(serverVersion, serverModJarHash, serverModJarSize, serverMinecraftVersion);
    }
    
    /**
     * Start the update process
     */
    private static void startUpdate(String version, String hash, long size, String minecraftVersion) {
        updateInProgress = true;
        targetVersion = version;
        expectedHash = hash;
        expectedSize = size;
        targetMinecraftVersion = minecraftVersion;
        receivedChunks.clear();
        totalChunks = 0;
        
        ServerManagementMod.LOGGER.info("Starting OTA update to version {}", version);
        
        // Update screen progress
        if (updateScreen != null) {
            Minecraft.getInstance().execute(() -> 
                updateScreen.updateProgress(0.0f, "Requesting mod file from server...")
            );
        }
        
        // Request mod file from server
        ModNetworking.sendToServer(new ModFileRequestPacket(
            targetVersion,
            ServerManagementMod.getModVersion(),
            targetMinecraftVersion != null ? targetMinecraftVersion : 
                com.servermanagement.ota.OTAVersion.loadFromResources().getMinecraftVersion()
        ));
    }
    
    /**
     * Handle received file chunk
     */
    public static void handleModFileChunk(int chunkIndex, int totalChunks, byte[] chunkData, String fileHash) {
        if (!updateInProgress) {
            ServerManagementMod.LOGGER.warn("Received chunk but no update in progress, ignoring");
            return;
        }
        
        if (!fileHash.equals(expectedHash)) {
            ServerManagementMod.LOGGER.error("Received chunk with wrong hash, aborting");
            cancelUpdate("Hash mismatch");
            return;
        }
        
        // Initialize chunks list if needed
        if (OTAUpdateManager.totalChunks == 0) {
            OTAUpdateManager.totalChunks = totalChunks;
            receivedChunks = new ArrayList<>(totalChunks);
            for (int i = 0; i < totalChunks; i++) {
                receivedChunks.add(null);
            }
            
            ServerManagementMod.LOGGER.info("Starting download: {} chunks", totalChunks);
            
            // Update screen
            if (updateScreen != null) {
                Minecraft.getInstance().execute(() -> 
                    updateScreen.updateProgress(0.0f, "Downloading mod file...")
                );
            }
            
            // Notify player
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null) {
                player.displayClientMessage(Component.literal("§7[OTA] §aDownload started..."), false);
            }
        }
        
        // Store chunk
        if (chunkIndex < receivedChunks.size()) {
            receivedChunks.set(chunkIndex, chunkData);
        }
        
        // Calculate and update progress
        float progress = ((chunkIndex + 1) * 1.0f) / totalChunks;
        String status = String.format("Downloading... (%d/%d chunks)", chunkIndex + 1, totalChunks);
        
        if (updateScreen != null) {
            Minecraft.getInstance().execute(() -> 
                updateScreen.updateProgress(progress * 0.9f, status) // Reserve 10% for verification
            );
        }
        
        // Show progress every 10 chunks or on last chunk
        if (chunkIndex % 10 == 0 || chunkIndex == totalChunks - 1) {
            double progressPercent = progress * 100.0;
            
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null) {
                player.displayClientMessage(Component.literal(
                    String.format("§7[OTA] §eDownloading: %.1f%% (%d/%d)", 
                        progressPercent, chunkIndex + 1, totalChunks)
                ), false);
            }
        }
    }
    
    /**
     * Handle transfer completion
     */
    public static void handleTransferComplete(String fileHash, long fileSize, String version) {
        if (!updateInProgress) {
            return;
        }
        
        ServerManagementMod.LOGGER.info("Transfer complete, verifying file...");
        
        // Update screen
        if (updateScreen != null) {
            Minecraft.getInstance().execute(() -> 
                updateScreen.updateProgress(0.95f, "Verifying file integrity...")
            );
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.displayClientMessage(Component.literal("§7[OTA] §aDownload complete!"), false);
            player.displayClientMessage(Component.literal("§7[OTA] §eVerifying file integrity..."), false);
        }
        
        try {
            // Assemble file from chunks
            byte[] fileData = assembleChunks();
            
            // Verify size
            if (fileData.length != expectedSize) {
                throw new IOException(String.format("Size mismatch: expected %d, got %d", 
                    expectedSize, fileData.length));
            }
            
            // Verify hash
            String actualHash = calculateHash(fileData);
            if (!actualHash.equals(expectedHash)) {
                throw new IOException("Hash mismatch: file corrupted during transfer");
            }
            
            ServerManagementMod.LOGGER.info("File verification successful");
            
            if (player != null) {
                player.displayClientMessage(Component.literal("§7[OTA] §aVerification successful!"), false);
                player.displayClientMessage(Component.literal("§7[OTA] §eInstalling update..."), false);
            }
            
            // Install the update
            installUpdate(fileData, version);
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to verify/install update", e);
            
            if (player != null) {
                player.displayClientMessage(Component.literal("§7[OTA] §cUpdate failed: " + e.getMessage()), false);
            }
            
            cancelUpdate(e.getMessage());
        }
    }
    
    /**
     * Handle transfer failure
     */
    public static void handleTransferFailed(String message) {
        ServerManagementMod.LOGGER.error("Transfer failed: {}", message);
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.displayClientMessage(Component.literal("§7[OTA] §cUpdate failed: " + message), false);
        }
        
        cancelUpdate(message);
    }
    
    /**
     * Cancel update and update screen
     */
    private static void cancelUpdate(String errorMessage) {
        updateInProgress = false;
        
        if (updateScreen != null) {
            Minecraft.getInstance().execute(() -> 
                updateScreen.setFailed(errorMessage)
            );
        }
        
        // Clean up
        receivedChunks.clear();
        totalChunks = 0;
        targetVersion = null;
        targetMinecraftVersion = null;
        expectedHash = null;
        expectedSize = 0;
    }
    
    /**
     * Assemble file from received chunks
     */
    private static byte[] assembleChunks() throws IOException {
        int totalSize = 0;
        for (byte[] chunk : receivedChunks) {
            if (chunk == null) {
                throw new IOException("Missing chunk data");
            }
            totalSize += chunk.length;
        }
        
        byte[] fileData = new byte[totalSize];
        int offset = 0;
        for (byte[] chunk : receivedChunks) {
            System.arraycopy(chunk, 0, fileData, offset, chunk.length);
            offset += chunk.length;
        }
        
        return fileData;
    }
    
    /**
     * Calculate SHA-256 hash
     */
    private static String calculateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Install the update
     */
    private static void installUpdate(byte[] fileData, String version) {
        try {
            // Update screen
            if (updateScreen != null) {
                Minecraft.getInstance().execute(() -> 
                    updateScreen.updateProgress(1.0f, "Installing update...")
                );
            }
            
            // Find mods directory
            File modsDir = new File("mods");
            if (!modsDir.exists()) {
                modsDir.mkdirs();
            }
            
            // Backup old mod JAR
            File[] oldMods = modsDir.listFiles((dir, name) -> 
                (name.startsWith("servermanagement") || name.startsWith("servermanagementplus")) && name.endsWith(".jar"));
            
            if (oldMods != null && oldMods.length > 0) {
                for (File oldMod : oldMods) {
                    File backup = new File(modsDir, oldMod.getName() + ".backup");
                    if (oldMod.renameTo(backup)) {
                        ServerManagementMod.LOGGER.info("Backed up old mod to {}", backup.getName());
                    }
                }
            }
            
            // Write new mod JAR - version already contains the full version string from Gradle
            File newModFile = new File(modsDir, "servermanagementplus-" + version + ".jar");
            try (FileOutputStream fos = new FileOutputStream(newModFile)) {
                fos.write(fileData);
            }
            
            ServerManagementMod.LOGGER.info("Successfully installed update to {}", newModFile.getName());
            
            // Update screen to complete
            if (updateScreen != null) {
                Minecraft.getInstance().execute(() -> 
                    updateScreen.setComplete()
                );
            }
            
            // Notify player
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null) {
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                player.displayClientMessage(Component.literal("§a§l✓ UPDATE INSTALLED SUCCESSFULLY"), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("§7Version: §a" + version), false);
                player.displayClientMessage(Component.literal("§7File: §f" + newModFile.getName()), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("§e⚠ RESTART REQUIRED"), false);
                player.displayClientMessage(Component.literal("§7Please restart your game to apply the update."), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("§8Old version backed up as .backup"), false);
                player.displayClientMessage(Component.literal("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                player.displayClientMessage(Component.literal(""), false);
            }
            
            updateInProgress = false;
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to install update", e);
            
            ServerManagementMod.LOGGER.warn("Update cancelled: {}", e.getMessage());
            
            // Update screen if exists
            if (updateScreen != null) {
                Minecraft.getInstance().execute(() -> 
                    updateScreen.setFailed(e.getMessage())
                );
            }
            
            updateInProgress = false;
            targetVersion = null;
            targetMinecraftVersion = null;
            expectedHash = null;
            expectedSize = 0;
            receivedChunks.clear();
            totalChunks = 0;
        }
    }
    
    /**
     * Format file size for display
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
