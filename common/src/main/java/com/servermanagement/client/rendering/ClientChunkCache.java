package com.servermanagement.client.rendering;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChunkCache {

    // Simple RAM cache for now. A full disk cache implementation 
    // requires serializing chunk sections and paletted containers.
    private static final Map<Long, LevelChunk> cachedChunks = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    public static void cacheChunk(LevelChunk chunk) {
        if (cachedChunks.size() >= MAX_CACHE_SIZE) {
            // Very rudimentary eviction: clear everything if we hit the limit
            // Real implementation would use an LRU cache or disk-spill.
            cachedChunks.clear();
        }
        cachedChunks.put(chunk.getPos().toLong(), chunk);
    }

    @Nullable
    public static LevelChunk getChunk(ChunkPos pos) {
        return cachedChunks.get(pos.toLong());
    }

    public static void clearCache() {
        cachedChunks.clear();
    }
}
