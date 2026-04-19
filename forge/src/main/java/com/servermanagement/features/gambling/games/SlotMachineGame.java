package com.servermanagement.features.gambling.games;

import com.servermanagement.features.gambling.GamblingGame;
import com.servermanagement.features.gambling.GamblingManager;

import java.security.SecureRandom;

/**
 * Slot machine game - Three reels with various symbols
 * Different symbol combinations yield different payouts
 */
public class SlotMachineGame implements GamblingGame {
    private static final SecureRandom random = new SecureRandom();
    
    // Symbols and their weights (lower = rarer)
    private enum Symbol {
        CHERRY(40, 2),      // Common: 2x
        LEMON(30, 3),       // Uncommon: 3x
        ORANGE(20, 5),      // Rare: 5x
        BELL(8, 10),        // Very Rare: 10x
        DIAMOND(2, 50),     // Ultra Rare: 50x
        JACKPOT(1, 100);    // Legendary: 100x
        
        final int weight;
        final double multiplier;
        
        Symbol(int weight, double multiplier) {
            this.weight = weight;
            this.multiplier = multiplier;
        }
    }
    
    @Override
    public double play(double betAmount) {
        // Spin three reels
        Symbol reel1 = spinReel();
        Symbol reel2 = spinReel();
        Symbol reel3 = spinReel();
        
        // Check for wins
        double multiplier = 0.0;
        
        // Three of a kind - full multiplier
        if (reel1 == reel2 && reel2 == reel3) {
            multiplier = reel1.multiplier;
        }
        // Two of a kind - partial multiplier
        else if (reel1 == reel2 || reel2 == reel3 || reel1 == reel3) {
            Symbol matchedSymbol = reel1 == reel2 ? reel1 : (reel2 == reel3 ? reel2 : reel1);
            multiplier = matchedSymbol.multiplier * 0.5; // Half payout for two matches
        }
        
        if (multiplier > 0) {
            // Apply house edge
            double payout = betAmount * multiplier * (1.0 - GamblingManager.SLOT_MACHINE_HOUSE_EDGE);
            return payout;
        }
        
        return 0.0;
    }
    
    /**
     * Spin a single reel using weighted random selection
     */
    private Symbol spinReel() {
        int totalWeight = 0;
        for (Symbol symbol : Symbol.values()) {
            totalWeight += symbol.weight;
        }
        
        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (Symbol symbol : Symbol.values()) {
            currentWeight += symbol.weight;
            if (roll < currentWeight) {
                return symbol;
            }
        }
        
        return Symbol.CHERRY; // Fallback
    }
    
    @Override
    public String getName() {
        return "Slot Machine";
    }
    
    @Override
    public double getHouseEdge() {
        return GamblingManager.SLOT_MACHINE_HOUSE_EDGE;
    }
}
