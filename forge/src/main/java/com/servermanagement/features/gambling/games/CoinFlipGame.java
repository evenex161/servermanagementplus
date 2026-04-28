package com.servermanagement.features.gambling.games;

import com.servermanagement.features.gambling.GamblingGame;
import com.servermanagement.features.gambling.GamblingManager;

import java.security.SecureRandom;

/**
 * Coin flip game - Double or nothing
 * 50/50 chance with 2% house edge
 */
public class CoinFlipGame implements GamblingGame {
    private static final SecureRandom random = new SecureRandom();
    private final String choice; // "heads" or "tails"
    
    public CoinFlipGame(String choice) {
        this.choice = choice.toLowerCase();
    }
    
    @Override
    public double play(double betAmount) {
        // Flip the coin
        boolean heads = random.nextBoolean();
        String result = heads ? "heads" : "tails";
        
        // Check if player won
        if (result.equals(choice)) {
            // Win: 2x payout minus house edge
            double payout = betAmount * 2.0 * (1.0 - GamblingManager.COIN_FLIP_HOUSE_EDGE);
            return payout;
        } else {
            // Loss
            return 0.0;
        }
    }
    
    @Override
    public String getName() {
        return "Coin Flip";
    }
    
    @Override
    public double getHouseEdge() {
        return GamblingManager.COIN_FLIP_HOUSE_EDGE;
    }
    
    public String getChoice() {
        return choice;
    }
}
