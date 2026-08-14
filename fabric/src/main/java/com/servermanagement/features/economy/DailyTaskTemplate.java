package com.servermanagement.features.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Template for daily tasks that admins can create and manage.
 * The daily system selects from these templates when generating player tasks.
 */
public class DailyTaskTemplate {
    private String id; // Unique identifier
    private TaskType type;
    private int targetAmount;
    private double rewardAmount;
    private ItemStack rewardItem; // Legacy field for Gson backward compatibility
    private List<ItemStack> rewardItems = new ArrayList<>(); // Optional item rewards
    private String customDescription; // Optional custom description
    private boolean enabled; // Can be disabled without deleting
    
    public DailyTaskTemplate() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.rewardItems = new ArrayList<>();
    }
    
    public DailyTaskTemplate(TaskType type, int targetAmount, double rewardAmount, String customDescription) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.customDescription = customDescription;
        this.enabled = true;
        this.rewardItems = new ArrayList<>();
    }
    
    public DailyTaskTemplate(TaskType type, int targetAmount, double rewardAmount, List<ItemStack> rewardItems, String customDescription) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.targetAmount = targetAmount;
        this.rewardAmount = rewardAmount;
        this.rewardItems = rewardItems != null ? new ArrayList<>(rewardItems) : new ArrayList<>();
        this.customDescription = customDescription;
        this.enabled = true;
    }

    // Getters
    
    public void migrateLegacyData() {
        if (rewardItems == null) {
            rewardItems = new ArrayList<>();
        }
        if (rewardItem != null && !rewardItem.isEmpty()) {
            rewardItems.add(rewardItem.copy());
            rewardItem = null; // Clear it so it doesn't get saved again if we exclude nulls, or just leave it empty
        }
    }

    public String getId() {
        return id;
    }

    public TaskType getType() {
        return type;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public double getRewardAmount() {
        return rewardAmount;
    }
    
    public List<ItemStack> getRewardItems() {
        return rewardItems != null ? rewardItems : new ArrayList<>();
    }

    public String getCustomDescription() {
        return customDescription;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Setters
    public void setType(TaskType type) {
        this.type = type;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setRewardAmount(double rewardAmount) {
        this.rewardAmount = rewardAmount;
    }
    
    public void setRewardItems(List<ItemStack> rewardItems) {
        this.rewardItems = rewardItems != null ? new ArrayList<>(rewardItems) : new ArrayList<>();
    }

    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get display description (custom or auto-generated)
     */
    public String getDescription() {
        if (customDescription != null && !customDescription.isEmpty()) {
            return customDescription;
        }
        return generateDescription();
    }

    /**
     * Generate automatic description based on task type
     */
    private String generateDescription() {
        switch (type) {
            case BREAK_BLOCKS:
                return "Break " + targetAmount + " blocks";
            case KILL_MOBS:
                return "Kill " + targetAmount + " mobs";
            case TRAVEL_DISTANCE:
                return "Travel " + targetAmount + " blocks";
            case CRAFT_ITEMS:
                return "Craft " + targetAmount + " items";
            case MINE_ORES:
                return "Mine " + targetAmount + " ores";
            case TRADE_VILLAGERS:
                return "Trade with villagers " + targetAmount + " times";
            default:
                return "Complete task";
        }
    }

    /**
     * Create a DailyTask instance from this template
     */
    public DailyTask createTask() {
        DailyTask task = new DailyTask(type, targetAmount, rewardAmount, getDescription());
        if (rewardItems != null && !rewardItems.isEmpty()) {
            task.setRewardItems(new ArrayList<>(rewardItems));
        }
        return task;
    }

    /**
     * Validate template has required fields
     */
    public boolean isValid() {
        return type != null && targetAmount > 0 && rewardAmount > 0;
    }

    @Override
    public String toString() {
        return "DailyTaskTemplate{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", target=" + targetAmount +
                ", reward=$" + rewardAmount +
                ", enabled=" + enabled +
                '}';
    }
}
