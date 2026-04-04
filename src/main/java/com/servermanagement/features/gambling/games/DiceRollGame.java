package com.servermanagement.features.gambling.games;

import com.servermanagement.features.gambling.GamblingGame;
import com.servermanagement.features.gambling.GamblingManager;

import java.security.SecureRandom;

/**
 * Dice roll game - Roll two dice, bet on the outcome
 * Various betting options with different payouts
 */
public class DiceRollGame implements GamblingGame {
    private static final SecureRandom random = new SecureRandom();
    private final BetType betType;
    private final int targetNumber; // For specific number bets
    
    public enum BetType {
        HIGH(8, 12, 2.0),      // Roll 8-12: 2x payout
        LOW(2, 6, 2.0),        // Roll 2-6: 2x payout
        SEVEN(7, 7, 5.0),      // Roll exactly 7: 5x payout
        DOUBLES(-1, -1, 6.0),  // Roll doubles: 6x payout
        SPECIFIC(-1, -1, 12.0); // Specific number: 12x payout
        
        final int minRoll;
        final int maxRoll;
        final double multiplier;
        
        BetType(int minRoll, int maxRoll, double multiplier) {
            this.minRoll = minRoll;
            this.maxRoll = maxRoll;
            this.multiplier = multiplier;
        }
    }
    
    public DiceRollGame(BetType betType) {
        this(betType, 0);
    }
    
    public DiceRollGame(BetType betType, int targetNumber) {
        this.betType = betType;
        this.targetNumber = targetNumber;
    }
    
    @Override
    public double play(double betAmount) {
        // Roll two dice
        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        int total = die1 + die2;
        
        boolean won = false;
        double multiplier = betType.multiplier;
        
        switch (betType) {
            case HIGH:
            case LOW:
                won = total >= betType.minRoll && total <= betType.maxRoll;
                break;
            case SEVEN:
                won = total == 7;
                break;
            case DOUBLES:
                won = die1 == die2;
                break;
            case SPECIFIC:
                won = total == targetNumber;
                break;
        }
        
        if (won) {
            // Apply house edge
            double payout = betAmount * multiplier * (1.0 - GamblingManager.DICE_ROLL_HOUSE_EDGE);
            return payout;
        }
        
        return 0.0;
    }
    
    @Override
    public String getName() {
        return "Dice Roll - " + betType.name();
    }
    
    @Override
    public double getHouseEdge() {
        return GamblingManager.DICE_ROLL_HOUSE_EDGE;
    }
    
    public BetType getBetType() {
        return betType;
    }
    
    public int getTargetNumber() {
        return targetNumber;
    }
}
