package com.servermanagement.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side storage for achievements data
 */
public class ClientAchievementsData {
    private static Set<String> earnedAchievements = new HashSet<>();
    private static int totalRewardsEarned = 0;

    public static Set<String> getEarnedAchievements() {
        return earnedAchievements;
    }

    public static void setEarnedAchievements(Set<String> achievements) {
        earnedAchievements = new HashSet<>(achievements);
    }

    public static int getTotalRewardsEarned() {
        return totalRewardsEarned;
    }

    public static void setTotalRewardsEarned(int total) {
        totalRewardsEarned = total;
    }

    public static void clear() {
        earnedAchievements.clear();
        totalRewardsEarned = 0;
    }
}
