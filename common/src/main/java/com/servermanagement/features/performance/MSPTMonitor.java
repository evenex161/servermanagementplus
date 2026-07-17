package com.servermanagement.features.performance;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.profiling.ProfilerFiller;

public class MSPTMonitor {
    
    private static long lastTickTime = 0;
    private static float currentMSPT = 0.0f;
    private static final float MSPT_WARNING_THRESHOLD = 45.0f;
    private static final float MSPT_RECOVERY_THRESHOLD = 40.0f;

    private static long lastTickStart = 0;

    public static void update(MinecraftServer server) {
        long now = System.nanoTime();
        if (lastTickStart == 0) {
            lastTickStart = now;
            return;
        }
        long tickTime = now - lastTickStart;
        lastTickStart = now;
        currentMSPT = tickTime / 1000000.0f; // Convert to ms
        
        // Safety Valve logic
        if (currentMSPT < MSPT_RECOVERY_THRESHOLD) {
            // Safe to process chunks
            ChunkPreGenerator.tick();
            DynamicWorkloadScaler.tryIncreaseWorkload(server);
        } else if (currentMSPT > MSPT_WARNING_THRESHOLD) {
            // High load, throttle workloads
            DynamicWorkloadScaler.throttleWorkload(server);
            // ChunkPreGenerator.tick() is intentionally skipped
        }
    }

    public static float getCurrentMSPT() {
        return currentMSPT;
    }
}
