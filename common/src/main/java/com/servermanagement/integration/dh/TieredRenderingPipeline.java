package com.servermanagement.integration.dh;

import com.servermanagement.Constants;

import java.lang.reflect.Method;

public class TieredRenderingPipeline {

    private static Method setMinRenderDistanceMethod = null;

    static {
        if (DistantHorizonsHook.isAvailable()) {
            try {
                Class<?> dhConfigClass = Class.forName("com.seibel.distanthorizons.api.DistantHorizonsConfig");
                setMinRenderDistanceMethod = dhConfigClass.getDeclaredMethod("setMinRenderDistance", int.class);
            } catch (Exception e) {
                Constants.LOG.debug("Could not hook into DistantHorizonsConfig for tiered rendering.");
            }
        }
    }

    public static void adjustMinRenderDistance(int serverSimulationDistance) {
        if (setMinRenderDistanceMethod == null) return;

        try {
            // Push the DH minimum render distance out to where our simulation/cache ends
            // to prevent Z-fighting and duplicate rendering.
            setMinRenderDistanceMethod.invoke(null, serverSimulationDistance);
        } catch (Exception e) {
            Constants.LOG.error("Failed to dynamically adjust DH render distance", e);
        }
    }
}
