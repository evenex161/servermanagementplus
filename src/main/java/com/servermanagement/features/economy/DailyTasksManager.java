package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.*;

/**
 * Manages daily tasks for all players
 */
public class DailyTasksManager {
    private int dataVersion = DataVersion.CURRENT_VERSION;
    private Map<UUID, PlayerDailyTasks> playerTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private transient Random random; // Not serialized - causes Java module issues with Gson
    private transient DailyTaskTemplateManager templateManager; // Not serialized

    public DailyTasksManager() {
        this.playerTasks = new java.util.concurrent.ConcurrentHashMap<>();
        this.random = new Random(); // Initialize random
    }

    /**
     * Set template manager (called after loading)
     */
    public void setTemplateManager(DailyTaskTemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    
    /**
     * Get template manager
     */
    public DailyTaskTemplateManager getTemplateManager() {
        return templateManager;
    }

    /**
     * Load daily tasks data from disk with automatic decryption
     */
    public static DailyTasksManager load(MinecraftServer server) {
        File file = getDataFile(server);
        DailyTasksManager manager = SecureDataStorage.load(file, DailyTasksManager.class, 
            new DailyTasksManager());
        
        if (manager.playerTasks == null) {
            manager.playerTasks = new java.util.concurrent.ConcurrentHashMap<>();
        } else if (!(manager.playerTasks instanceof java.util.concurrent.ConcurrentHashMap)) {
            manager.playerTasks = new java.util.concurrent.ConcurrentHashMap<>(manager.playerTasks);
        }
        
        // Check data version and migrate if needed
        if (manager.dataVersion == 0) {
            ServerManagementMod.LOGGER.info("Migrating legacy DailyTasksManager data to version {}", 
                DataVersion.CURRENT_VERSION);
            manager.migrateData(0, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion < DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.info("Migrating DailyTasksManager data from version {} to {}", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
            manager.migrateData(manager.dataVersion, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.error("DailyTasksManager data version {} is newer than supported version {}!", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        manager.dataVersion = DataVersion.CURRENT_VERSION;
        
        // Re-initialize transient fields after deserialization
        if (manager.random == null) {
            manager.random = new Random();
        }
        
        ServerManagementMod.LOGGER.info("Loaded daily tasks for {} players (v{})", 
            manager.playerTasks.size(), manager.dataVersion);
        return manager;
    }
    
    /**
     * Migrate data between versions
     */
    private void migrateData(int fromVersion, int toVersion) {
        // Future migrations will be added here
        ServerManagementMod.LOGGER.debug("Migration from v{} to v{} completed for DailyTasksManager", 
            fromVersion, toVersion);
    }

    /**
     * Save daily tasks data to disk with encryption
     */
    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        SecureDataStorage.save(this, file, DailyTasksManager.class);
        ServerManagementMod.LOGGER.debug("Saved encrypted daily tasks data v{}", dataVersion);
    }
    
    public int getDataVersion() {
        return dataVersion;
    }

    private static File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/daily_tasks.json");
    }

    /**
     * Get or create daily tasks for a player
     */
    public PlayerDailyTasks getOrCreatePlayerTasks(UUID playerUUID) {
        PlayerDailyTasks tasks = playerTasks.get(playerUUID);
        if (tasks == null) {
            tasks = new PlayerDailyTasks();
            generateNewTasks(tasks);
            playerTasks.put(playerUUID, tasks);
        }
        return tasks;
    }
    
    /**
     * Get player tasks without creating (returns null if not exist)
     */
    public PlayerDailyTasks getPlayerTasks(UUID playerUUID) {
        return playerTasks.get(playerUUID);
    }

    /**
     * Generate 3 new unique daily tasks
     */
    public void generateNewTasks(PlayerDailyTasks playerTasks) {
        List<DailyTask> newTasks = new ArrayList<>();
        
        // Use templates if available
        if (templateManager != null) {
            List<DailyTaskTemplate> selectedTemplates = templateManager.selectRandomTemplates(3);
            for (DailyTaskTemplate template : selectedTemplates) {
                newTasks.add(template.createTask());
            }
            ServerManagementMod.LOGGER.debug("Generated {} tasks from templates", newTasks.size());
        }
        
        // Fallback to random generation if no templates available
        if (newTasks.isEmpty()) {
            List<TaskType> availableTypes = new ArrayList<>(Arrays.asList(TaskType.values()));
            Collections.shuffle(availableTypes, random);
            
            for (int i = 0; i < Math.min(3, availableTypes.size()); i++) {
                TaskType type = availableTypes.get(i);
                int goal = type.getRandomGoal(random);
                newTasks.add(new DailyTask(type, goal));
            }
            ServerManagementMod.LOGGER.debug("Generated {} tasks with random generation", newTasks.size());
        }
        
        playerTasks.setTasks(newTasks);
        playerTasks.setLastTaskRefreshTime(System.currentTimeMillis());
    }

    /**
     * Check and refresh tasks if cooldown expired
     */
    public boolean checkAndRefreshTasks(UUID playerUUID) {
        PlayerDailyTasks tasks = getOrCreatePlayerTasks(playerUUID);
        if (tasks.needsTaskRefresh()) {
            generateNewTasks(tasks);
            // Reset free reward if it was claimed
            if (tasks.isFreeRewardClaimed()) {
                tasks.setFreeRewardClaimed(false);
            }
            return true;
        }
        return false;
    }

    /**
     * Add progress to a specific task type for a player
     * Returns the task description if it was just completed, null otherwise
     */
    public String addProgress(UUID playerUUID, TaskType taskType, int amount) {
        PlayerDailyTasks playerTasksData = getOrCreatePlayerTasks(playerUUID);
        
        for (DailyTask task : playerTasksData.getTasks()) {
            if (task.getType() == taskType && !task.isClaimed()) {
                boolean wasCompleted = task.isCompleted();
                task.addProgress(amount);
                
                // Check if task was just completed
                if (!wasCompleted && task.isCompleted()) {
                    return task.getDescription();
                }
            }
        }
        return null;
    }

    /**
     * Claim reward for a completed task
     */
    public int claimTaskReward(UUID playerUUID, int taskIndex) {
        PlayerDailyTasks playerTasksData = getOrCreatePlayerTasks(playerUUID);
        DailyTask task = playerTasksData.getTask(taskIndex);
        
        if (task != null && task.isCompleted() && !task.isClaimed()) {
            task.setClaimed(true);
            return task.getReward();
        }
        
        return 0;
    }

    /**
     * Claim the free daily reward
     */
    public int claimFreeReward(UUID playerUUID) {
        PlayerDailyTasks playerTasksData = getOrCreatePlayerTasks(playerUUID);
        
        if (playerTasksData.isFreeRewardAvailable()) {
            playerTasksData.setFreeRewardClaimed(true);
            playerTasksData.setLastFreeRewardClaimTime(System.currentTimeMillis());
            // Use the admin-configurable value from template manager
            if (templateManager != null) {
                return (int) templateManager.getFreeRewardAmount();
            }
            return playerTasksData.getFreeRewardAmount();
        }
        
        return 0;
    }

    /**
     * Get all player tasks (for admin purposes)
     */
    public Map<UUID, PlayerDailyTasks> getAllPlayerTasks() {
        return new HashMap<>(playerTasks);
    }

    /**
     * Force reset all player daily tasks (admin command)
     */
    public int forceResetAllDailies() {
        int count = 0;
        for (PlayerDailyTasks tasks : playerTasks.values()) {
            generateNewTasks(tasks);
            // Reset free reward
            tasks.setFreeRewardClaimed(false);
            count++;
        }
        ServerManagementMod.LOGGER.info("Force reset dailies for {} players", count);
        return count;
    }

    /**
     * Force reset specific player's daily tasks
     */
    public boolean forceResetPlayerDailies(UUID playerUUID) {
        PlayerDailyTasks tasks = playerTasks.get(playerUUID);
        if (tasks != null) {
            generateNewTasks(tasks);
            tasks.setFreeRewardClaimed(false);
            ServerManagementMod.LOGGER.info("Force reset dailies for player {}", playerUUID);
            return true;
        }
        return false;
    }
}
