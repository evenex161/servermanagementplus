package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.security.SecureDataStorage;
import com.servermanagement.util.DataVersion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Manages daily task templates that admins can configure.
 * These templates are used to generate player daily tasks.
 */
public class DailyTaskTemplateManager {
    private int dataVersion = DataVersion.CURRENT_VERSION;
    private List<DailyTaskTemplate> templates = new ArrayList<>();
    private double freeRewardAmount = 50.0; // Default free reward
    private ItemStack freeRewardItem = ItemStack.EMPTY; // Optional item reward
    private List<ItemStack> freeRewardItems = new ArrayList<>(); // Multi-item reward
    private int freeRewardCooldownHours = 24; // Default cooldown

    public DailyTaskTemplateManager() {
        this.templates = new ArrayList<>();
        initializeDefaultTemplates();
    }

    /**
     * Initialize with default templates if none exist
     */
    private void initializeDefaultTemplates() {
        if (templates.isEmpty()) {
            // Default task templates
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.BREAK_BLOCKS, 100, "")), 75.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.BREAK_BLOCKS, 250, "")), 150.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.BREAK_BLOCKS, 500, "")), 250.0, new java.util.ArrayList<>()));
            
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.KILL_MOBS, 20, "")), 100.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.KILL_MOBS, 50, "")), 200.0, new java.util.ArrayList<>()));
            
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.TRAVEL_DISTANCE, 1000, "")), 80.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.TRAVEL_DISTANCE, 5000, "")), 200.0, new java.util.ArrayList<>()));
            
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.CRAFT_ITEMS, 50, "")), 90.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.CRAFT_ITEMS, 100, "")), 175.0, new java.util.ArrayList<>()));
            
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.MINE_ORES, 30, "")), 120.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.MINE_ORES, 75, "")), 250.0, new java.util.ArrayList<>()));
            
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.TRADE_VILLAGERS, 10, "")), 100.0, new java.util.ArrayList<>()));
            templates.add(new DailyTaskTemplate(java.util.Collections.singletonList(new TaskComponent(TaskType.TRADE_VILLAGERS, 25, "")), 225.0, new java.util.ArrayList<>()));
        }
    }

    /**
     * Load templates from disk
     */
    public static DailyTaskTemplateManager load(MinecraftServer server) {
        File file = getDataFile(server);
        DailyTaskTemplateManager manager = SecureDataStorage.load(file, DailyTaskTemplateManager.class, 
            new DailyTaskTemplateManager());
        
        if (manager.templates == null) {
            manager.templates = new ArrayList<>();
        }
        
        // Check data version and migrate if needed
        if (manager.dataVersion == 0) {
            ServerManagementMod.LOGGER.debug("Migrating legacy DailyTaskTemplateManager data to version {}", 
                DataVersion.CURRENT_VERSION);
            manager.migrateData(0, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion < DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.debug("Migrating DailyTaskTemplateManager data from version {} to {}", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
            manager.migrateData(manager.dataVersion, DataVersion.CURRENT_VERSION);
        } else if (manager.dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.error("DailyTaskTemplateManager data version {} is newer than supported version {}!", 
                manager.dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        manager.dataVersion = DataVersion.CURRENT_VERSION;
        
        // Ensure we have default templates
        manager.initializeDefaultTemplates();
        
        // Migrate legacy template data
        if (manager.templates != null) {
            for (DailyTaskTemplate template : manager.templates) {
                template.migrateLegacyData();
            }
        }
        
        // Migrate legacy single item to list
        if (manager.freeRewardItems == null) {
            manager.freeRewardItems = new ArrayList<>();
        }
        if (manager.freeRewardItem != null && !manager.freeRewardItem.isEmpty()) {
            manager.freeRewardItems.add(manager.freeRewardItem.copy());
            manager.freeRewardItem = ItemStack.EMPTY;
        }
        
        ServerManagementMod.LOGGER.debug("Loaded {} daily task templates (v{})", 
            manager.templates.size(), manager.dataVersion);
        return manager;
    }
    
    /**
     * Migrate data between versions
     */
    private void migrateData(int fromVersion, int toVersion) {
        // Future migrations will be added here
        // Example:
        // if (fromVersion < DataVersion.VERSION_1_1_0 && toVersion >= DataVersion.VERSION_1_1_0) {
        //     // Perform 1.0 -> 1.1 migration
        // }
        ServerManagementMod.LOGGER.debug("Migration from v{} to v{} completed for DailyTaskTemplateManager", 
            fromVersion, toVersion);
    }

    /**
     * Save templates to disk
     */
    public void save(MinecraftServer server) {
        File file = getDataFile(server);
        SecureDataStorage.save(this, file, DailyTaskTemplateManager.class);
        ServerManagementMod.LOGGER.debug("Saved encrypted daily task templates v{}", dataVersion);
    }
    
    public int getDataVersion() {
        return dataVersion;
    }

    private static File getDataFile(MinecraftServer server) {
        File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, "data/servermanagement/daily_task_templates.json");
    }

    /**
     * Get all templates
     */
    public List<DailyTaskTemplate> getAllTemplates() {
        return new ArrayList<>(templates);
    }

    /**
     * Get enabled templates only
     */
    public List<DailyTaskTemplate> getEnabledTemplates() {
        List<DailyTaskTemplate> result = new ArrayList<>();
        for (DailyTaskTemplate t : templates) {
            if (t.isEnabled() ) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Get template by ID
     */
    public DailyTaskTemplate getTemplate(String id) {
        for (DailyTaskTemplate t : templates) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Add new template
     */
    public DailyTaskTemplate addTemplate(DailyTaskTemplate template) {
        templates.add(template);
        return template;
    }

    /**
     * Update existing template
     */
    public boolean updateTemplate(String id, DailyTaskTemplate updated) {
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).getId().equals(id)) {
                templates.set(i, updated);
                return true;
            }
        }
        return false;
    }

    /**
     * Delete template
     */
    public boolean deleteTemplate(String id) {
        return templates.removeIf(t -> t.getId().equals(id));
    }

    /**
     * Select 3 random templates for daily tasks
     */
    public List<DailyTaskTemplate> selectRandomTemplates(int count) {
        List<DailyTaskTemplate> enabled = getEnabledTemplates();
        if (enabled.isEmpty()) {
            initializeDefaultTemplates();
            enabled = getEnabledTemplates();
        }

        Collections.shuffle(enabled);
        int limit = Math.min(count, enabled.size());
        return enabled.subList(0, limit);
    }

    /**
     * Get free reward amount
     */
    public double getFreeRewardAmount() {
        return freeRewardAmount;
    }

    /**
     * Set free reward amount
     */
    public void setFreeRewardAmount(double amount) {
        this.freeRewardAmount = Math.max(0, amount);
    }
    
    /**
     * Get free reward items
     */
    public List<ItemStack> getFreeRewardItems() {
        if (freeRewardItems == null) {
            freeRewardItems = new ArrayList<>();
        }
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack item : freeRewardItems) {
            copies.add(item.copy());
        }
        return copies;
    }
    
    /**
     * Set free reward items
     */
    public void setFreeRewardItems(List<ItemStack> items) {
        this.freeRewardItems = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.isEmpty()) {
                    this.freeRewardItems.add(item.copy());
                }
            }
        }
    }

    /**
     * Get free reward item
     */
    public ItemStack getFreeRewardItem() {
        return freeRewardItem != null ? freeRewardItem : ItemStack.EMPTY;
    }
    
    /**
     * Set free reward item
     */
    public void setFreeRewardItem(ItemStack item) {
        this.freeRewardItem = item != null ? item.copy() : ItemStack.EMPTY;
    }

    /**
     * Get free reward cooldown in hours
     */
    public int getFreeRewardCooldownHours() {
        return freeRewardCooldownHours;
    }

    /**
     * Set free reward cooldown in hours
     */
    public void setFreeRewardCooldownHours(int hours) {
        this.freeRewardCooldownHours = Math.max(1, hours);
    }

    /**
     * Get templates by type
     */
    public List<DailyTaskTemplate> getTemplatesByType(TaskType type) {
        List<DailyTaskTemplate> result = new ArrayList<>();
        for (DailyTaskTemplate t : templates) {
            if (t.getType() == type) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Get statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", templates.size());
        int enabledCount = 0;
        EnumMap<TaskType, Integer> typeCounts = new EnumMap<>(TaskType.class);
        for (DailyTaskTemplate t : templates) {
            if (t.isEnabled()) enabledCount++;
            typeCounts.merge(t.getType(), 1, Integer::sum);
        }
        stats.put("enabled", enabledCount);
        stats.put("disabled", templates.size() - enabledCount);
        for (TaskType type : TaskType.values()) {
            stats.put(type.name(), typeCounts.getOrDefault(type, 0));
        }
        return stats;
    }
}
