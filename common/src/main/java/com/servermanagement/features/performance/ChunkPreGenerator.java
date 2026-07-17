package com.servermanagement.features.performance;

import com.servermanagement.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Comparator;

public class ChunkPreGenerator {

    private static final TicketType<ChunkPos> PREGEN_TICKET = TicketType.create("sm_pregen", Comparator.comparingLong(ChunkPos::toLong));
    private static final Queue<ChunkPos> pregenQueue = new LinkedList<>();
    private static boolean isRunning = false;
    private static ServerLevel currentLevel;
    private static int totalChunks = 0;
    private static int generatedChunks = 0;

    public static void startPregen(ServerLevel level, BlockPos center, int chunkRadius) {
        if (isRunning) return;
        currentLevel = level;
        isRunning = true;
        pregenQueue.clear();

        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;

        // Spiral matrix algorithm
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int t = Math.max(chunkRadius * 2, chunkRadius * 2);
        int maxI = t * t;

        for (int i = 0; i < maxI; i++) {
            if ((-chunkRadius <= x) && (x <= chunkRadius) && (-chunkRadius <= z) && (z <= chunkRadius)) {
                pregenQueue.add(new ChunkPos(cx + x, cz + z));
            }
            if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                t = dx;
                dx = -dz;
                dz = t;
            }
            x += dx;
            z += dz;
        }

        totalChunks = pregenQueue.size();
        generatedChunks = 0;
        Constants.LOG.info("Started chunk pre-generation for {} chunks", totalChunks);
    }

    public static void tick() {
        if (!isRunning || pregenQueue.isEmpty()) {
            if (isRunning) {
                Constants.LOG.info("Chunk pre-generation complete!");
                isRunning = false;
            }
            return;
        }

        // Poll 5 chunks per tick (throttled by MSPTMonitor later)
        for (int i = 0; i < 5 && !pregenQueue.isEmpty(); i++) {
            ChunkPos pos = pregenQueue.poll();
            currentLevel.getChunkSource().addRegionTicket(PREGEN_TICKET, pos, 2, pos);
            generatedChunks++;
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static int getProgress() {
        if (totalChunks == 0) return 100;
        return (int) (((float) generatedChunks / totalChunks) * 100);
    }

    public static void stopPregen() {
        isRunning = false;
        pregenQueue.clear();
        Constants.LOG.info("Chunk pre-generation stopped.");
    }
}
