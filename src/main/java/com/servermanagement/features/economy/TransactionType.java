package com.servermanagement.features.economy;

/**
 * Types of transactions in the economy system
 */
public enum TransactionType {
    ACHIEVEMENT("Achievement Reward", true),
    PLAYER_TRANSFER_SENT("Sent to Player", false),
    PLAYER_TRANSFER_RECEIVED("Received from Player", true),
    ADMIN_GIVE("Admin Gift", true),
    ADMIN_TAKE("Admin Deduction", false);

    private final String displayName;
    private final boolean income; // true = adds money, false = removes money

    TransactionType(String displayName, boolean income) {
        this.displayName = displayName;
        this.income = income;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isIncome() {
        return income;
    }
}
