package com.servermanagement.client;

import com.servermanagement.features.economy.DailyTask;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side storage for daily tasks data
 */
public class ClientDailyTasksData {
    private static List<DailyTask> tasks = new ArrayList<>();
    private static long resetTime = 0;
    private static boolean freeRewardAvailable = false;
    private static int freeRewardAmount = 100;
    private static List<ItemStack> freeRewardItems = new ArrayList<>();
    private static long timeUntilFreeReward = 0;

    public static List<DailyTask> getTasks() {
        return tasks;
    }

    public static void setTasks(List<DailyTask> newTasks) {
        tasks = new ArrayList<>(newTasks);
    }

    public static long getResetTime() {
        return resetTime;
    }

    public static void setResetTime(long time) {
        resetTime = time;
    }
    
    public static boolean isFreeRewardAvailable() {
        return freeRewardAvailable;
    }
    
    public static void setFreeRewardAvailable(boolean available) {
        freeRewardAvailable = available;
    }
    
    public static int getFreeRewardAmount() {
        return freeRewardAmount;
    }
    
    public static void setFreeRewardAmount(int amount) {
        freeRewardAmount = amount;
    }
    
    public static long getTimeUntilFreeReward() {
        return timeUntilFreeReward;
    }
    
    public static void setTimeUntilFreeReward(long time) {
        timeUntilFreeReward = time;
    }

        public static List<ItemStack> getFreeRewardItems() {
        return freeRewardItems;
    }

    public static void setFreeRewardItems(List<ItemStack> items) {
        freeRewardItems = new ArrayList<>(items);
    }

    public static void clear() {
        tasks.clear();
        resetTime = 0;
        freeRewardAvailable = false;
        freeRewardAmount = 100;
        freeRewardItems.clear();
        timeUntilFreeReward = 0;
    }
}
