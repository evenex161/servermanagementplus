package com.servermanagement.features.economy;

/**
 * Types of daily tasks players can complete
 */
public enum TaskType {
    BREAK_BLOCKS("Break Blocks", "Break %d blocks", "*"),
    KILL_MOBS("Kill Mobs", "Kill %d mobs", "!"),
    TRAVEL_DISTANCE("Travel Distance", "Travel %d blocks", ">"),
    CRAFT_ITEMS("Craft Items", "Craft %d items", "+"),
    MINE_ORES("Mine Ores", "Mine %d ores", "#"),
    TRADE_VILLAGERS("Trade with Villagers", "Trade %d times", "$");

    private final String displayName;
    private final String descriptionFormat;
    private final String icon;

    TaskType(String displayName, String descriptionFormat, String icon) {
        this.displayName = displayName;
        this.descriptionFormat = descriptionFormat;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Get formatted description with goal amount
     */
    public String getDescription(int goal) {
        return String.format(descriptionFormat, goal);
    }

    /**
     * Get reward amount for this task type based on goal
     */
    public int getReward(int goal) {
        // Base rewards per unit of progress
        int baseReward = switch (this) {
            case BREAK_BLOCKS -> 1;  // $1 per 10 blocks
            case KILL_MOBS -> 5;     // $5 per mob
            case TRAVEL_DISTANCE -> 1; // $1 per 100 blocks
            case CRAFT_ITEMS -> 3;   // $3 per item
            case MINE_ORES -> 10;    // $10 per ore
            case TRADE_VILLAGERS -> 20; // $20 per trade
        };
        
        return Math.max(50, goal * baseReward); // Minimum $50 reward
    }

    /**
     * Get a random reasonable goal for this task type
     */
    public int getRandomGoal(java.util.Random random) {
        return switch (this) {
            case BREAK_BLOCKS -> 50 + random.nextInt(150); // 50-200 blocks
            case KILL_MOBS -> 10 + random.nextInt(20);      // 10-30 mobs
            case TRAVEL_DISTANCE -> 500 + random.nextInt(1500); // 500-2000 blocks
            case CRAFT_ITEMS -> 5 + random.nextInt(15);     // 5-20 items
            case MINE_ORES -> 10 + random.nextInt(30);      // 10-40 ores
            case TRADE_VILLAGERS -> 3 + random.nextInt(7);  // 3-10 trades
        };
    }
}
