package com.servermanagement.client;

import com.servermanagement.features.economy.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side storage for bank account data received from server
 */
public class ClientBankData {
    private static double balance = 0.0;
    private static List<Transaction> transactions = new ArrayList<>();

    public static double getBalance() {
        return balance;
    }

    public static void setBalance(double newBalance) {
        balance = newBalance;
    }

    public static List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public static void setTransactions(List<Transaction> newTransactions) {
        transactions = new ArrayList<>(newTransactions);
    }

    public static void clear() {
        balance = 0.0;
        transactions.clear();
    }
}
