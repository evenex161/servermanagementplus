package com.servermanagement.features.performance;

import com.servermanagement.Constants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

public class DynamicWorkloadScaler {

    private static int originalSimulationDistance = -1;
    private static int minSimulationDistance = 4; // Configurable
    private static long lastScaleTime = 0;
    private static final long COOLDOWN_MS = 5000; // 5 seconds between scaling actions

    public static void setMinSimulationDistance(int min) {
        minSimulationDistance = min;
    }

    public static void throttleWorkload(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastScaleTime < COOLDOWN_MS) return;

        PlayerList playerList = server.getPlayerList();
        int currentDistance = playerList.getSimulationDistance();

        if (originalSimulationDistance == -1) {
            originalSimulationDistance = currentDistance;
        }

        if (currentDistance > minSimulationDistance) {
            int newDistance = currentDistance - 1;
            playerList.setSimulationDistance(newDistance);
            Constants.LOG.warn("High server load! Throttled simulation distance from {} to {}", currentDistance, newDistance);
            lastScaleTime = now;
        }
    }

    public static void tryIncreaseWorkload(MinecraftServer server) {
        if (originalSimulationDistance == -1) return;

        long now = System.currentTimeMillis();
        if (now - lastScaleTime < COOLDOWN_MS) return;

        PlayerList playerList = server.getPlayerList();
        int currentDistance = playerList.getSimulationDistance();

        if (currentDistance < originalSimulationDistance) {
            int newDistance = currentDistance + 1;
            playerList.setSimulationDistance(newDistance);
            Constants.LOG.info("Server load stabilized. Restoring simulation distance from {} to {}", currentDistance, newDistance);
            lastScaleTime = now;
            
            if (newDistance == originalSimulationDistance) {
                originalSimulationDistance = -1; // Reset tracker when fully restored
            }
        }
    }
}
