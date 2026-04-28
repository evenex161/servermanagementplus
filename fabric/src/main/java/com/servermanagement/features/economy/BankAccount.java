package com.servermanagement.features.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player's bank account with balance and transaction history.
 */
public class BankAccount {
    private final UUID playerUUID;
    private double balance;
    private final List<Transaction> transactions;
    private static final int MAX_TRANSACTION_HISTORY = 100;

    public BankAccount(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
    }

    public BankAccount(UUID playerUUID, double startingBalance) {
        this.playerUUID = playerUUID;
        this.balance = Math.max(0, startingBalance);
        this.transactions = new ArrayList<>();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized void setBalance(double balance) {
        this.balance = Math.max(0, balance); // Never allow negative balance
    }

    /**
     * Add money to the account.
     * Synchronized to prevent concurrent balance modifications.
     * @return true if successful
     */
    public synchronized boolean deposit(double amount) {
        if (amount <= 0) return false;
        this.balance += amount;
        return true;
    }

    /**
     * Remove money from the account.
     * Synchronized to prevent concurrent balance modifications.
     * @return true if successful, false if insufficient funds
     */
    public synchronized boolean withdraw(double amount) {
        if (amount <= 0 || this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    /**
     * Atomically check balance and withdraw in one operation.
     * Prevents TOCTOU race conditions where balance is checked then modified.
     * @return true if the account had sufficient funds and withdrawal succeeded
     */
    public synchronized boolean tryWithdraw(double amount) {
        if (amount <= 0 || this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    /**
     * Add a transaction to history, maintaining max size
     */
    public synchronized void addTransaction(Transaction transaction) {
        transactions.add(0, transaction); // Add to front (newest first)
        
        // Keep only last MAX_TRANSACTION_HISTORY transactions
        if (transactions.size() > MAX_TRANSACTION_HISTORY) {
            transactions.remove(transactions.size() - 1);
        }
    }

    /**
     * Get transaction history (newest first)
     */
    public synchronized List<Transaction> getTransactions() {
        return new ArrayList<>(transactions); // Return copy
    }

    /**
     * Get recent transactions (limited count)
     */
    public synchronized List<Transaction> getRecentTransactions(int count) {
        int limit = Math.min(count, transactions.size());
        return new ArrayList<>(transactions.subList(0, limit));
    }
}
