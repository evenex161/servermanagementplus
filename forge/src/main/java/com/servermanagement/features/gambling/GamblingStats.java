package com.servermanagement.features.gambling;

import java.io.Serializable;
import java.util.UUID;

/**
 * Tracks gambling statistics for a player
 */
public class GamblingStats implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UUID playerId;
    private long totalBets;
    private long totalWins;
    private long totalLosses;
    private double totalWagered;
    private double totalWon;
    private double totalLost;
    private double biggestWin;
    private double biggestLoss;
    private long lastPlayedTime;
    
    public GamblingStats(UUID playerId) {
        this.playerId = playerId;
        this.totalBets = 0;
        this.totalWins = 0;
        this.totalLosses = 0;
        this.totalWagered = 0.0;
        this.totalWon = 0.0;
        this.totalLost = 0.0;
        this.biggestWin = 0.0;
        this.biggestLoss = 0.0;
        this.lastPlayedTime = System.currentTimeMillis();
    }
    
    public void recordBet(double amount, double profit, boolean won) {
        totalBets++;
        totalWagered += amount;
        lastPlayedTime = System.currentTimeMillis();
        
        if (won) {
            totalWins++;
            totalWon += profit;
            if (profit > biggestWin) {
                biggestWin = profit;
            }
        } else {
            totalLosses++;
            double loss = Math.abs(profit);
            totalLost += loss;
            if (loss > biggestLoss) {
                biggestLoss = loss;
            }
        }
    }
    
    public double getNetProfit() {
        return totalWon - totalLost;
    }
    
    public double getWinRate() {
        if (totalBets == 0) return 0.0;
        return (double) totalWins / totalBets;
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public long getTotalBets() { return totalBets; }
    public long getTotalWins() { return totalWins; }
    public long getTotalLosses() { return totalLosses; }
    public double getTotalWagered() { return totalWagered; }
    public double getTotalWon() { return totalWon; }
    public double getTotalLost() { return totalLost; }
    public double getBiggestWin() { return biggestWin; }
    public double getBiggestLoss() { return biggestLoss; }
    public long getLastPlayedTime() { return lastPlayedTime; }
}
