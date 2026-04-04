package com.servermanagement.features.economy;

import java.util.UUID;

/**
 * Represents a single transaction in the economy system
 */
public class Transaction {
    private final TransactionType type;
    private final double amount;
    private final long timestamp;
    private final String description;
    private final UUID otherParty; // For transfers/requests, null otherwise

    public Transaction(TransactionType type, double amount, String description, UUID otherParty) {
        this.type = type;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.description = description;
        this.otherParty = otherParty;
    }

    public Transaction(TransactionType type, double amount, String description) {
        this(type, amount, description, null);
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOtherParty() {
        return otherParty;
    }

    /**
     * Format transaction for display
     */
    public String getFormattedAmount() {
        String sign = type.isIncome() ? "+" : "-";
        return sign + "$" + String.format("%.2f", Math.abs(amount));
    }
}
