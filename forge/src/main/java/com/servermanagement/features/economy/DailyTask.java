package com.servermanagement.features.economy;

import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a single daily task with progress tracking
 */
public class DailyTask {
    private final TaskType type;
    private final int goal;
    private int progress;
    private boolean claimed;
    private final int reward;
    private ItemStack rewardItem; // Legacy field for Gson backward compatibility
    private List<ItemStack> rewardItems = new ArrayList<>(); // Optional item rewards
    private final String customDescription; // Optional custom description

    public DailyTask(TaskType type, int goal) {
        this.type = type;
        this.goal = goal;
        this.progress = 0;
        this.claimed = false;
        this.reward = type.getReward(goal);
        this.customDescription = null;
        this.rewardItems = new ArrayList<>();
    }

    public DailyTask(TaskType type, int goal, double reward, String description) {
        this.type = type;
        this.goal = goal;
        this.progress = 0;
        this.claimed = false;
        this.reward = (int) reward;
        this.customDescription = description;
        this.rewardItems = new ArrayList<>();
    }

    
    public void migrateLegacyData() {
        if (rewardItems == null) {
            rewardItems = new ArrayList<>();
        }
        if (rewardItem != null && !rewardItem.isEmpty()) {
            rewardItems.add(rewardItem.copy());
            rewardItem = null; // Clear it so it doesn't get saved again if we exclude nulls, or just leave it empty
        }
    }

    public TaskType getType() {
        return type;
    }

    public int getGoal() {
        return goal;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, goal);
    }

    public void addProgress(int amount) {
        this.progress = Math.min(this.progress + amount, goal);
    }

    public boolean isCompleted() {
        return progress >= goal;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public int getReward() {
        return reward;
    }
    
    public List<ItemStack> getRewardItems() {
        return rewardItems != null ? rewardItems : new ArrayList<>();
    }
    
    public void setRewardItems(List<ItemStack> rewardItems) {
        this.rewardItems = rewardItems != null ? new ArrayList<>(rewardItems) : new ArrayList<>();
    }

    /**
     * Get progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (goal == 0) return 100;
        return Math.min(100, (progress * 100) / goal);
    }

    /**
     * Get formatted progress string (e.g., "15/20")
     */
    public String getProgressString() {
        return progress + "/" + goal;
    }

    /**
     * Get description for this task
     */
    public String getDescription() {
        if (customDescription != null && !customDescription.isEmpty()) {
            return customDescription;
        }
        return type.getDescription(goal);
    }
}
