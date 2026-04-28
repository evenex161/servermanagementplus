package com.servermanagement.features.gambling;

/**
 * Interface for gambling games
 */
public interface GamblingGame {
    /**
     * Play the game with the given bet amount
     * @param betAmount Amount wagered
     * @return Payout amount (0 if lost, betAmount + winnings if won)
     */
    double play(double betAmount);
    
    /**
     * Get the name of the game
     */
    String getName();
    
    /**
     * Get the house edge as a percentage
     */
    double getHouseEdge();
}
