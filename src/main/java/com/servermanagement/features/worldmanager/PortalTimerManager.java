package com.servermanagement.features.worldmanager;

import net.minecraft.server.MinecraftServer;

public class PortalTimerManager {
    private final MinecraftServer server;

    public PortalTimerManager(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Sets a portal timer for the given dimension.
     * Only one timer can be active at a time across all dimensions.
     *
     * @param dimensionId the dimension to set the timer for
     * @param seconds how long the timer runs (0 to cancel)
     * @param portalType "nether", "end", or "both"
     * @return true if the timer was set, false if blocked (e.g., another timer is active)
     */
    public static boolean setTimer(String dimensionId, int seconds, String portalType) {
        WorldManager worldManager = WorldManager.getInstance();
        if (worldManager == null) return false;

        WorldManagerData data = worldManager.getData();

        // Handle cancel
        if (seconds == 0) {
            data.clearTimer(dimensionId);
            TimerTickHandler.clearAnnouncementTracking(dimensionId);
            worldManager.save();
            return true;
        }

        // Enforce single timer: block if any other dimension already has a timer
        if (data.hasAnyActiveTimer()) {
            String existingDim = data.getActiveTimerDimension();
            if (existingDim != null && !existingDim.equals(dimensionId)) {
                return false; // Another timer is running
            }
        }

        // Determine target state based on portal type and current state
        boolean currentNetherState = data.areNetherPortalsEnabled(dimensionId);
        boolean currentEndState = data.areEndPortalsEnabled(dimensionId);
        boolean targetEnables;

        switch (portalType) {
            case "nether":
                targetEnables = !currentNetherState;
                break;
            case "end":
                targetEnables = !currentEndState;
                break;
            case "both":
            default:
                // If either is disabled, target is enable; if both enabled, target is disable
                targetEnables = !currentNetherState || !currentEndState;
                break;
        }

        // Save timer data to persistence
        data.setTimerSeconds(dimensionId, seconds);
        data.setTimerEnablesPortal(dimensionId, targetEnables);
        data.setTimerPortalType(dimensionId, portalType);
        TimerTickHandler.clearAnnouncementTracking(dimensionId);
        worldManager.save();

        return true;
    }

    /**
     * Timer processing is handled solely by TimerTickHandler.
     * This method is intentionally empty to prevent dual timer processing.
     */
    public void tick() {
        // No-op: TimerTickHandler is the sole authoritative timer processor
    }
}
