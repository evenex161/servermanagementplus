package com.servermanagement.features.economy;

/**
 * Achievement reward tiers based on rarity
 */
public enum AchievementRewardTier {
    COMMON(50, 100, "Common"),
    UNCOMMON(150, 300, "Uncommon"),
    RARE(400, 700, "Rare"),
    EPIC(1000, 2000, "Epic"),
    LEGENDARY(3000, 5000, "Legendary");

    private final int minReward;
    private final int maxReward;
    private final String displayName;

    AchievementRewardTier(int minReward, int maxReward, String displayName) {
        this.minReward = minReward;
        this.maxReward = maxReward;
        this.displayName = displayName;
    }

    public int getMinReward() {
        return minReward;
    }

    public int getMaxReward() {
        return maxReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get average reward for this tier
     */
    public int getAverageReward() {
        return (minReward + maxReward) / 2;
    }

    /**
     * Determine tier from advancement frame type
     */
    public static AchievementRewardTier fromFrameType(net.minecraft.advancements.AdvancementType frameType) {
        switch (frameType) {
            case TASK:
                return COMMON;
            case GOAL:
                return UNCOMMON;
            case CHALLENGE:
                return RARE;
            default:
                return COMMON;
        }
    }

    /**
     * Determine tier from advancement criteria count (more complex = higher tier)
     */
    public static AchievementRewardTier fromComplexity(int criteriaCount, boolean hasParent) {
        if (criteriaCount >= 10) {
            return LEGENDARY;
        } else if (criteriaCount >= 5) {
            return EPIC;
        } else if (criteriaCount >= 3) {
            return RARE;
        } else if (hasParent) {
            return UNCOMMON;
        }
        return COMMON;
    }
}
