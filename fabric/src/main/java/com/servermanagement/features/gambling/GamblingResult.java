package com.servermanagement.features.gambling;

/**
 * Result of a gambling game
 */
public class GamblingResult {
    private final boolean won;
    private final double payout;
    private final String message;
    
    public GamblingResult(boolean won, double payout, String message) {
        this.won = won;
        this.payout = payout;
        this.message = message;
    }
    
    public boolean isWon() {
        return won;
    }
    
    public double getPayout() {
        return payout;
    }
    
    public String getMessage() {
        return message;
    }
}
