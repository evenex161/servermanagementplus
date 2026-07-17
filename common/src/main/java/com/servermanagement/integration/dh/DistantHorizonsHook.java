package com.servermanagement.integration.dh;

import com.servermanagement.Constants;
import net.minecraft.world.level.ChunkPos;

import java.lang.reflect.Method;

public class DistantHorizonsHook {

    private static boolean isDHAvailable = false;
    private static Method dhBroadcastMethod = null;

    static {
        try {
            // Attempt to resolve DH API via reflection to prevent hard crashes
            Class<?> dhApiClass = Class.forName("com.seibel.distanthorizons.api.DistantHorizonsAPI");
            dhBroadcastMethod = dhApiClass.getDeclaredMethod("notifyChunkFinalized", int.class, int.class);
            isDHAvailable = true;
            Constants.LOG.info("Distant Horizons API hooked successfully via reflection.");
        } catch (ClassNotFoundException e) {
            Constants.LOG.debug("Distant Horizons not found. LOD integration disabled.");
        } catch (NoSuchMethodException e) {
            Constants.LOG.warn("Distant Horizons API found but method notifyChunkFinalized is missing. Version mismatch?");
        }
    }

    public static void broadcastChunkFinalized(ChunkPos pos) {
        if (!isDHAvailable || dhBroadcastMethod == null) return;
        
        try {
            dhBroadcastMethod.invoke(null, pos.x, pos.z);
        } catch (Exception e) {
            Constants.LOG.error("Failed to broadcast chunk finalized to Distant Horizons", e);
        }
    }

    public static boolean isAvailable() {
        return isDHAvailable;
    }
}
