package com.servermanagement.integration.dh;

import com.servermanagement.Constants;

/**
 * Coordinates rendering between SM+'s client chunk cache and Distant Horizons' LOD system.
 * 
 * When DH is active, SM+ yields chunk rendering beyond the server's simulation distance
 * to prevent Z-fighting and double memory usage. SM+'s ClientChunkCache still operates
 * for chunks within simulation distance (where DH doesn't render LODs).
 * 
 * All methods are safe to call regardless of whether DH is installed.
 */
public class TieredRenderingPipeline {

    private static int serverSimulationDistance = 5;

    /**
     * Updates the known server simulation distance. Called when the player joins
     * or when the server changes its simulation distance.
     */
    public static void setServerSimulationDistance(int distance) {
        serverSimulationDistance = distance;
        if (DistantHorizonsHook.isAvailable()) {
            Constants.LOG.debug("Tiered rendering: SM+ handles chunks within {} chunks, DH handles beyond.",
                distance);
        }
    }

    /**
     * Returns true if SM+ should handle rendering/caching for a chunk at the given
     * distance from the player. When DH is active, SM+ yields chunks beyond
     * simulation distance to DH's LOD renderer.
     *
     * @param chunkDistanceSq squared chunk distance from the player
     * @return true if SM+ should handle this chunk, false if DH should
     */
    public static boolean shouldSMHandleChunk(int chunkDistanceSq) {
        if (!DistantHorizonsHook.isAvailable()) {
            // No DH — SM+ handles everything
            return true;
        }
        // When DH is present, SM+ only handles chunks within simulation distance.
        // DH handles everything beyond that with its LOD system.
        int threshold = serverSimulationDistance * serverSimulationDistance;
        return chunkDistanceSq <= threshold;
    }

    /**
     * Returns the current server simulation distance.
     */
    public static int getServerSimulationDistance() {
        return serverSimulationDistance;
    }
}
