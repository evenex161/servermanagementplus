package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.*;

/**
 * Tracks which achievements have been rewarded to each player to prevent duplicate rewards.
 */
public class AchievementRewardTracker {
    private int dataVersion = DataVersion.CURRENT_VERSION;
    private Map<UUID, Set<String>> playerAchievements = new HashMap<>();

    public AchievementRewardTracker() {
        this.playerAchievements = new HashMap<>();
    }

    /**
     * Load tracker data from disk with automatic decryption
     */
    public static AchievementRewardTracker load(MinecraftServer server) {
        File file = getDataFile(server);
        AchievementRewardTracker tracker = SecureDataStorage.load(file, AchievementRewardTracker.class, 
            new AchievementRewardTracker());
        
        if (tracker.playerAchievements == null) {
            tracker.playerAchievements = new HashMap<>();
        }
        
        // Check data version and migrate if needed
        if (tracker.dataVersion == 0) {
            ServerManagementMod.LOGGER.info("Migrating legacy AchievementRewardTracker data to version {}", 
                DataVersion.CURRENT_VERSION);
            tracker.migrateData(0, DataVersion.CURRENT_VERSION);
        } else if (tracker.dataVersion < DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.info("Migrating AchievementRewardTracker data from version {} to {}", 
                tracker.dataVersion, DataVersion.CURRENT_VERSION);
            tracker.migrateData(tracker.dataVersion, DataVersion.CURRENT_VERSION);
        } else if (tracker.dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.error("AchievementRewardTracker data version {} is newer than supported version {}!", 
                tracker.dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        tracker.dataVersion = DataVersion.CURRENT_VERSION;
        
        ServerManagementMod.LOGGER.info("Loaded achievement reward tracker for {} players (v{})", 
            tracker.playerAchievements.size(), tracker.dataVersion);
        return tracker;
    }
    
    /**
     * Migrate data between versions
     */
    private void migrateData(int fromVersion, int toVersion) {
        // Future migrations will be added here
        ServerManagementMod.LOGGER.debug("Migration from v{} to v{} completed for AchievementRewardTracker", 
            fromVersion, toVersion);
    }

    /**
     * Save tracker data to disk with encryption
     */
    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        SecureDataStorage.save(this, file, AchievementRewardTracker.class);
        ServerManagementMod.LOGGER.debug("Saved encrypted achievement reward tracker v{}", dataVersion);
    }
    
    public int getDataVersion() {
        return dataVersion;
    }

    private static File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/achievement_rewards.json");
    }

    /**
     * Check if a player has already been rewarded for an achievement
     */
    public boolean hasBeenRewarded(UUID playerUUID, String achievementId) {
        Set<String> achievements = playerAchievements.get(playerUUID);
        return achievements != null && achievements.contains(achievementId);
    }

    /**
     * Mark an achievement as rewarded for a player
     */
    public void markAsRewarded(UUID playerUUID, String achievementId) {
        playerAchievements.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(achievementId);
    }

    /**
     * Get all rewarded achievements for a player
     */
    public Set<String> getRewardedAchievements(UUID playerUUID) {
        return new HashSet<>(playerAchievements.getOrDefault(playerUUID, new HashSet<>()));
    }

    /**
     * Get total number of rewarded achievements for a player
     */
    public int getRewardedCount(UUID playerUUID) {
        Set<String> achievements = playerAchievements.get(playerUUID);
        return achievements != null ? achievements.size() : 0;
    }
}
