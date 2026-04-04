package com.servermanagement.features.economy;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's daily tasks and free reward with cooldown tracking
 */
public class PlayerDailyTasks {
    private List<DailyTask> tasks;
    private long lastTaskRefreshTime;
    private long lastFreeRewardClaimTime;
    private boolean freeRewardClaimed;
    
    private static final long COOLDOWN_DURATION = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static final int FREE_REWARD_AMOUNT = 50; // $50 free daily reward (default)

    public PlayerDailyTasks() {
        this.tasks = new ArrayList<>();
        this.lastTaskRefreshTime = 0;
        this.lastFreeRewardClaimTime = 0;
        this.freeRewardClaimed = false;
    }

    public List<DailyTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<DailyTask> tasks) {
        this.tasks = tasks;
    }

    public long getLastTaskRefreshTime() {
        return lastTaskRefreshTime;
    }

    public void setLastTaskRefreshTime(long lastTaskRefreshTime) {
        this.lastTaskRefreshTime = lastTaskRefreshTime;
    }

    public long getLastFreeRewardClaimTime() {
        return lastFreeRewardClaimTime;
    }

    public void setLastFreeRewardClaimTime(long lastFreeRewardClaimTime) {
        this.lastFreeRewardClaimTime = lastFreeRewardClaimTime;
    }

    public boolean isFreeRewardClaimed() {
        return freeRewardClaimed;
    }

    public void setFreeRewardClaimed(boolean freeRewardClaimed) {
        this.freeRewardClaimed = freeRewardClaimed;
    }

    public int getFreeRewardAmount() {
        return FREE_REWARD_AMOUNT;
    }

    /**
     * Check if tasks need to be refreshed (24 hours since last refresh)
     */
    public boolean needsTaskRefresh() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastTaskRefreshTime >= COOLDOWN_DURATION;
    }

    /**
     * Check if free reward is available (24 hours since last claim)
     */
    public boolean isFreeRewardAvailable() {
        if (!freeRewardClaimed) return true; // Never claimed before
        long currentTime = System.currentTimeMillis();
        return currentTime - lastFreeRewardClaimTime >= COOLDOWN_DURATION;
    }

    /**
     * Get remaining time until task refresh (in milliseconds)
     */
    public long getTimeUntilTaskRefresh() {
        long currentTime = System.currentTimeMillis();
        long timeSinceRefresh = currentTime - lastTaskRefreshTime;
        return Math.max(0, COOLDOWN_DURATION - timeSinceRefresh);
    }

    /**
     * Get remaining time until free reward available (in milliseconds)
     */
    public long getTimeUntilFreeReward() {
        if (!freeRewardClaimed) return 0;
        long currentTime = System.currentTimeMillis();
        long timeSinceClaim = currentTime - lastFreeRewardClaimTime;
        return Math.max(0, COOLDOWN_DURATION - timeSinceClaim);
    }

    /**
     * Get formatted time remaining (e.g., "12h 34m")
     */
    public static String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) return "Available";
        
        long hours = milliseconds / (60 * 60 * 1000);
        long minutes = (milliseconds % (60 * 60 * 1000)) / (60 * 1000);
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    /**
     * Get a task by index
     */
    public DailyTask getTask(int index) {
        if (index >= 0 && index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }

    /**
     * Check if all tasks are completed
     */
    public boolean areAllTasksCompleted() {
        return tasks.stream().allMatch(DailyTask::isCompleted);
    }

    /**
     * Check if all completed tasks are claimed
     */
    public boolean areAllTasksClaimed() {
        return tasks.stream().allMatch(task -> !task.isCompleted() || task.isClaimed());
    }
}
