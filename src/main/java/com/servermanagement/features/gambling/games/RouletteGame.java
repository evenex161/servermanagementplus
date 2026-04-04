package com.servermanagement.features.gambling.games;

import com.servermanagement.features.gambling.GamblingGame;
import com.servermanagement.features.gambling.GamblingManager;

import java.security.SecureRandom;

/**
 * Roulette game - Classic casino roulette with European layout (0-36)
 * Various betting options
 */
public class RouletteGame implements GamblingGame {
    private static final SecureRandom random = new SecureRandom();
    private final BetType betType;
    private final int specificNumber; // For straight bets
    
    public enum BetType {
        RED(2.0),          // Red numbers: 2x payout
        BLACK(2.0),        // Black numbers: 2x payout
        EVEN(2.0),         // Even numbers: 2x payout
        ODD(2.0),          // Odd numbers: 2x payout
        LOW(2.0),          // 1-18: 2x payout
        HIGH(2.0),         // 19-36: 2x payout
        DOZEN(3.0),        // 1-12, 13-24, 25-36: 3x payout
        COLUMN(3.0),       // Column bets: 3x payout
        STRAIGHT(36.0);    // Single number: 36x payout
        
        final double multiplier;
        
        BetType(double multiplier) {
            this.multiplier = multiplier;
        }
    }
    
    // Red numbers in European roulette
    private static final int[] RED_NUMBERS = {
        1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36
    };
    
    public RouletteGame(BetType betType) {
        this(betType, 0);
    }
    
    public RouletteGame(BetType betType, int specificNumber) {
        this.betType = betType;
        this.specificNumber = specificNumber;
    }
    
    @Override
    public double play(double betAmount) {
        // Spin the wheel (0-36)
        int winningNumber = random.nextInt(37);
        
        boolean won = false;
        
        switch (betType) {
            case RED:
                won = isRed(winningNumber);
                break;
            case BLACK:
                won = !isRed(winningNumber) && winningNumber != 0;
                break;
            case EVEN:
                won = winningNumber != 0 && winningNumber % 2 == 0;
                break;
            case ODD:
                won = winningNumber % 2 == 1;
                break;
            case LOW:
                won = winningNumber >= 1 && winningNumber <= 18;
                break;
            case HIGH:
                won = winningNumber >= 19 && winningNumber <= 36;
                break;
            case STRAIGHT:
                won = winningNumber == specificNumber;
                break;
            case DOZEN:
                // For simplicity, bet on first dozen (1-12)
                won = winningNumber >= 1 && winningNumber <= 12;
                break;
            case COLUMN:
                // For simplicity, bet on first column
                won = winningNumber % 3 == 1 && winningNumber != 0;
                break;
        }
        
        if (won) {
            // Apply house edge
            double payout = betAmount * betType.multiplier * (1.0 - GamblingManager.ROULETTE_HOUSE_EDGE);
            return payout;
        }
        
        return 0.0;
    }
    
    /**
     * Check if a number is red in roulette
     */
    private boolean isRed(int number) {
        for (int red : RED_NUMBERS) {
            if (red == number) return true;
        }
        return false;
    }
    
    @Override
    public String getName() {
        return "Roulette - " + betType.name();
    }
    
    @Override
    public double getHouseEdge() {
        return GamblingManager.ROULETTE_HOUSE_EDGE;
    }
    
    public BetType getBetType() {
        return betType;
    }
    
    public int getSpecificNumber() {
        return specificNumber;
    }
}
