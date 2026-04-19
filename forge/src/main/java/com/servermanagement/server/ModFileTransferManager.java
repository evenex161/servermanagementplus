package com.servermanagement.server;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.ModFileChunkPacket;
import com.servermanagement.network.packet.ModFileCompletePacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for transferring mod JAR files to clients via OTA.
 * Handles file reading, chunking, and sending to clients.
 */
public class ModFileTransferManager {
    
    private static final Map<String, TransferSession> activeSessions = new ConcurrentHashMap<>();
    private static final java.util.Set<String> completedTransfers = ConcurrentHashMap.newKeySet();
    private static File modJarFile = null;
    private static String modJarHash = null;
    
    /**
     * Initialize the mod file transfer system.
     * Finds the current mod JAR file.
     */
    public static void initialize() {
        try {
            // Find our mod JAR in the mods folder
            File modsDir = new File("mods");
            if (modsDir.exists() && modsDir.isDirectory()) {
                File[] files = modsDir.listFiles((dir, name) -> 
                    (name.startsWith("servermanagement") || name.startsWith("servermanagementplus")) && name.endsWith(".jar"));
                
                if (files != null && files.length > 0) {
                    // Use the first matching file (or most recent if multiple)
                    modJarFile = files[0];
                    for (File file : files) {
                        if (file.lastModified() > modJarFile.lastModified()) {
                            modJarFile = file;
                        }
                    }
                    
                    // Calculate hash
                    modJarHash = calculateFileHash(modJarFile);
                    
                    ServerManagementMod.LOGGER.debug("OTA file transfer ready: {} ({} bytes)", modJarFile.getName(), modJarFile.length());
                } else {
                    ServerManagementMod.LOGGER.warn("Could not find mod JAR file for OTA updates");
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to initialize OTA update system", e);
        }
    }
    
    /**
     * Get the mod JAR file name
     */
    public static String getModJarName() {
        return modJarFile != null ? modJarFile.getName() : "unknown";
    }
    
    /**
     * Get the mod JAR file hash
     */
    public static String getModJarHash() {
        return modJarHash != null ? modJarHash : "";
    }
    
    /**
     * Get the mod JAR file size
     */
    public static long getModJarSize() {
        return modJarFile != null ? modJarFile.length() : 0;
    }
    
    /**
     * Check if OTA system is ready
     */
    public static boolean isReady() {
        return modJarFile != null && modJarHash != null;
    }
    
    /**
     * Check if a player already has an active or completed transfer this session.
     * Prevents DoS via repeated file transfer requests.
     */
    public static boolean hasActiveOrCompletedTransfer(ServerPlayer player) {
        String playerId = player.getStringUUID();
        return activeSessions.containsKey(playerId) || completedTransfers.contains(playerId);
    }
    
    /**
     * Start transferring the mod file to a client
     */
    public static void startTransfer(ServerPlayer player, String requestedVersion) {
        if (!isReady()) {
            ServerManagementMod.LOGGER.error("OTA system not initialized, cannot transfer file");
            ModNetworking.sendToPlayer(new ModFileCompletePacket(
                "", 0, requestedVersion, false, 
                "Server OTA system not initialized"
            ), player);
            return;
        }
        
        String playerId = player.getStringUUID();
        
        // Check if transfer already in progress
        if (activeSessions.containsKey(playerId)) {
            ServerManagementMod.LOGGER.warn("Transfer already in progress for {}", player.getName().getString());
            return;
        }
        
        ServerManagementMod.LOGGER.info("Starting mod file transfer to {}", player.getName().getString());
        
        // Create transfer session
        TransferSession session = new TransferSession(player, modJarFile, modJarHash);
        activeSessions.put(playerId, session);
        
        // Start async transfer
        CompletableFuture.runAsync(() -> {
            try {
                transferFile(session);
                
                // Send completion packet
                ModNetworking.sendToPlayer(new ModFileCompletePacket(
                    modJarHash,
                    modJarFile.length(),
                    ServerManagementMod.getModVersion(),
                    true,
                    "Transfer completed successfully"
                ), player);
                
                ServerManagementMod.LOGGER.info("File transfer completed for {}", 
                    player.getName().getString());
                
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("File transfer failed for {}", 
                    player.getName().getString(), e);
                
                ModNetworking.sendToPlayer(new ModFileCompletePacket(
                    "",
                    0,
                    ServerManagementMod.getModVersion(),
                    false,
                    "Transfer failed: " + e.getMessage()
                ), player);
            } finally {
                activeSessions.remove(playerId);
                completedTransfers.add(playerId);
            }
        });
    }
    
    /**
     * Transfer file in chunks
     */
    private static void transferFile(TransferSession session) throws IOException {
        File file = session.file;
        long fileSize = file.length();
        int totalChunks = (int) Math.ceil((double) fileSize / ModFileChunkPacket.CHUNK_SIZE);
        
        ServerManagementMod.LOGGER.info("Transferring {} bytes in {} chunks", fileSize, totalChunks);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[ModFileChunkPacket.CHUNK_SIZE];
            int chunkIndex = 0;
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Create chunk data (may be smaller than buffer for last chunk)
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                
                // Send chunk packet
                ModFileChunkPacket packet = new ModFileChunkPacket(
                    chunkIndex,
                    totalChunks,
                    chunkData,
                    session.fileHash
                );
                
                // Abort early if player disconnected (avoid wasting I/O and bandwidth)
                if (session.player.hasDisconnected()) {
                    ServerManagementMod.LOGGER.info("Aborting transfer — player {} disconnected",
                        session.player.getName().getString());
                    return;
                }
                
                ModNetworking.sendToPlayer(packet, session.player);
                
                chunkIndex++;
                
                // Small delay to avoid overwhelming the network
                try {
                    Thread.sleep(10); // 10ms delay between chunks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Transfer interrupted", e);
                }
            }
            
            ServerManagementMod.LOGGER.info("Sent {} chunks successfully", chunkIndex);
        }
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    private static String calculateFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            ServerManagementMod.LOGGER.error("Failed to calculate file hash", e);
            return "";
        }
    }
    
    /**
     * Transfer session data
     */
    private static class TransferSession {
        final ServerPlayer player;
        final File file;
        final String fileHash;
        
        TransferSession(ServerPlayer player, File file, String fileHash) {
            this.player = player;
            this.file = file;
            this.fileHash = fileHash;
        }
    }
}
