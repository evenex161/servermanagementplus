package com.servermanagement.features.performance;

import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkUnloadDelayManager {

    private static final Map<ChunkPos, Long> delayedUnloads = new ConcurrentHashMap<>();
    private static final long DELAY_MS = 10000; // 10 seconds

    public static void scheduleDelayedUnload(ChunkPos pos) {
        delayedUnloads.put(pos, System.currentTimeMillis() + DELAY_MS);
    }

    public static boolean isDelayed(ChunkPos pos) {
        return delayedUnloads.containsKey(pos);
    }

    public static void tick() {
        if (delayedUnloads.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ChunkPos, Long>> iterator = delayedUnloads.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, Long> entry = iterator.next();
            if (now >= entry.getValue()) {
                // Time to actually unload/remove ticket
                // This requires a callback to DistanceManager which we will handle via Mixin hooks
                iterator.remove();
            }
        }
    }
}
