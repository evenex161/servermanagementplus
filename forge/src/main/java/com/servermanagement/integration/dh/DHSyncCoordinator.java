package com.servermanagement.integration.dh;

import com.servermanagement.Constants;
import net.minecraft.world.level.ChunkPos;

/**
 * Coordinates between SM+'s server-side pre-generation and Distant Horizons'
 * distant generation system. When SM+ pre-generates a chunk, this coordinator
 * attempts to notify DH to refresh its LOD for that position.
 * 
 * Completely inert when DH is not installed.
 */
public class DHSyncCoordinator {

    private static java.lang.reflect.Method invalidateLodMethod = null;
    private static boolean hookAttempted = false;

    /**
     * Notify DH that a chunk has been fully generated/populated by SM+'s pre-generator.
     * DH should refresh its LOD for this position to pick up real terrain data.
     */
    public static void notifyChunkGenerated(ChunkPos pos) {
        if (!DistantHorizonsHook.isAvailable()) return;

        // Lazy-init the reflection hook
        if (!hookAttempted) {
            hookAttempted = true;
            try {
                // DH 2.x: Try to find the LOD invalidation API
                Class<?> dhApiClass = Class.forName("com.seibel.distanthorizons.api.DhApi");
                // The exact method signature depends on DH version — try known patterns
                try {
                    invalidateLodMethod = dhApiClass.getDeclaredMethod("refreshLodAt", int.class, int.class);
                } catch (NoSuchMethodException e) {
                    try {
                        invalidateLodMethod = dhApiClass.getDeclaredMethod("notifyChunkUpdated", int.class, int.class);
                    } catch (NoSuchMethodException e2) {
                        Constants.LOG.debug("DHSyncCoordinator: No LOD refresh API found in DH. " +
                            "Chunk sync will rely on DH's own world-watcher.");
                    }
                }
            } catch (ClassNotFoundException e) {
                // DH API class not found — already logged by DistantHorizonsHook
            }
        }

        if (invalidateLodMethod != null) {
            try {
                invalidateLodMethod.invoke(null, pos.x, pos.z);
            } catch (Exception e) {
                Constants.LOG.debug("DHSyncCoordinator: Failed to notify DH of chunk generation at [{}, {}]",
                    pos.x, pos.z);
            }
        }
    }
}
