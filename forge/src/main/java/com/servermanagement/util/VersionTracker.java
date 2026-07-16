package com.servermanagement.util;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.time.Instant;

/**
 * Tracks mod version installations and data migrations.
 * Uses SecureDataStorage for encrypted persistence.
 */
public class VersionTracker {
    
    public static class VersionInfo {
        public String modVersion;
        public int dataVersion;
        public String firstInstalled;
        public String lastUpdated;
        public String minecraftVersion;
        
        public VersionInfo() {
            this.modVersion = ServerManagementMod.getModVersion();
            this.dataVersion = DataVersion.CURRENT_VERSION;
            this.firstInstalled = Instant.now().toString();
            this.lastUpdated = Instant.now().toString();
            this.minecraftVersion = "1.20.1";
        }
    }
    
    /**
     * Load or create version tracking file
     */
    public static VersionInfo loadOrCreate(MinecraftServer server) {
        File file = getVersionFile(server);
        
        VersionInfo info = SecureDataStorage.load(file, VersionInfo.class, null);
        
        if (info != null) {
            boolean changed = false;
            
            // Check if mod was updated
            if (!ServerManagementMod.getModVersion().equals(info.modVersion)) {
                ServerManagementMod.LOGGER.info("Mod updated from {} to {}", 
                    info.modVersion, ServerManagementMod.getModVersion());
                info.modVersion = ServerManagementMod.getModVersion();
                info.lastUpdated = Instant.now().toString();
                changed = true;
            }
            
            // Check if data version changed
            if (info.dataVersion < DataVersion.CURRENT_VERSION) {
                ServerManagementMod.LOGGER.info("Data version updated from {} to {}", 
                    info.dataVersion, DataVersion.CURRENT_VERSION);
                info.dataVersion = DataVersion.CURRENT_VERSION;
                info.lastUpdated = Instant.now().toString();
                changed = true;
            }
            
            if (changed) {
                save(info, server);
            }
            return info;
        }
        
        // Create new version info
        ServerManagementMod.LOGGER.info("First time installation detected - ServerManagement v{}", 
            ServerManagementMod.getModVersion());
        info = new VersionInfo();
        save(info, server);
        return info;
    }
    
    /**
     * Save version info to disk (encrypted)
     */
    private static void save(VersionInfo info, MinecraftServer server) {
        File file = getVersionFile(server);
        try {
            SecureDataStorage.save(info, file, VersionInfo.class);
            ServerManagementMod.LOGGER.debug("Saved version info: v{}, data v{}", 
                info.modVersion, info.dataVersion);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save version info", e);
        }
    }
    
    private static File getVersionFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/version.json");
    }
}
