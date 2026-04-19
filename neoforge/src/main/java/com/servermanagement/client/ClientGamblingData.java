package com.servermanagement.client;

/**
 * Client-side storage for gambling statistics received from server
 */
public class ClientGamblingData {
    private static long totalBets = 0;
    private static long totalWins = 0;
    private static long totalLosses = 0;
    private static double totalWagered = 0.0;
    private static double totalWon = 0.0;
    private static double totalLost = 0.0;
    private static double biggestWin = 0.0;
    private static double biggestLoss = 0.0;

    public static long getTotalBets() {
        return totalBets;
    }

    public static void setTotalBets(long totalBets) {
        ClientGamblingData.totalBets = totalBets;
    }

    public static long getTotalWins() {
        return totalWins;
    }

    public static void setTotalWins(long totalWins) {
        ClientGamblingData.totalWins = totalWins;
    }

    public static long getTotalLosses() {
        return totalLosses;
    }

    public static void setTotalLosses(long totalLosses) {
        ClientGamblingData.totalLosses = totalLosses;
    }

    public static double getTotalWagered() {
        return totalWagered;
    }

    public static void setTotalWagered(double totalWagered) {
        ClientGamblingData.totalWagered = totalWagered;
    }

    public static double getTotalWon() {
        return totalWon;
    }

    public static void setTotalWon(double totalWon) {
        ClientGamblingData.totalWon = totalWon;
    }

    public static double getTotalLost() {
        return totalLost;
    }

    public static void setTotalLost(double totalLost) {
        ClientGamblingData.totalLost = totalLost;
    }

    public static double getBiggestWin() {
        return biggestWin;
    }

    public static void setBiggestWin(double biggestWin) {
        ClientGamblingData.biggestWin = biggestWin;
    }

    public static double getBiggestLoss() {
        return biggestLoss;
    }

    public static void setBiggestLoss(double biggestLoss) {
        ClientGamblingData.biggestLoss = biggestLoss;
    }

    public static double getNetProfit() {
        return totalWon - totalLost;
    }

    public static double getWinRate() {
        if (totalBets == 0) return 0.0;
        return (double) totalWins / totalBets;
    }

    public static void clear() {
        totalBets = 0;
        totalWins = 0;
        totalLosses = 0;
        totalWagered = 0.0;
        totalWon = 0.0;
        totalLost = 0.0;
        biggestWin = 0.0;
        biggestLoss = 0.0;
    }

    /**
     * Update all stats at once (called by sync packet)
     */
    public static void updateStats(long totalBets, long totalWins, long totalLosses,
                                   double totalWagered, double totalWon, double totalLost,
                                   double biggestWin, double biggestLoss) {
        ClientGamblingData.totalBets = totalBets;
        ClientGamblingData.totalWins = totalWins;
        ClientGamblingData.totalLosses = totalLosses;
        ClientGamblingData.totalWagered = totalWagered;
        ClientGamblingData.totalWon = totalWon;
        ClientGamblingData.totalLost = totalLost;
        ClientGamblingData.biggestWin = biggestWin;
        ClientGamblingData.biggestLoss = biggestLoss;
    }
}
