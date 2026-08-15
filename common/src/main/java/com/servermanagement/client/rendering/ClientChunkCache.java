package com.servermanagement.client.rendering;

import com.servermanagement.integration.dh.TieredRenderingPipeline;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache for client-side chunks. Automatically evicts least-recently-accessed
 * entries when the cache exceeds its capacity. Thread-safe via synchronized access.
 * 
 * When Distant Horizons is active, the cache is automatically constrained to only
 * store chunks within the server's simulation distance (see {@link TieredRenderingPipeline}).
 */
public class ClientChunkCache {

    private static int maxCacheSize = 10000;

    private static final Map<Long, LevelChunk> cachedChunks = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, LevelChunk> eldest) {
            return size() > maxCacheSize;
        }
    };

    /**
     * Cache a chunk. Automatically evicts the least-recently-accessed entry
     * if the cache exceeds its maximum size.
     */
    public static synchronized void cacheChunk(LevelChunk chunk) {
        cachedChunks.put(chunk.getPos().toLong(), chunk);
    }

    /**
     * Retrieve a cached chunk, or null if not cached.
     * Accessing a chunk promotes it in the LRU ordering.
     */
    @Nullable
    public static synchronized LevelChunk getChunk(ChunkPos pos) {
        return cachedChunks.get(pos.toLong());
    }

    /**
     * Check if a chunk is cached without promoting it in LRU order.
     */
    public static synchronized boolean hasChunk(ChunkPos pos) {
        return cachedChunks.containsKey(pos.toLong());
    }

    /**
     * Remove a specific chunk from the cache.
     */
    public static synchronized void evictChunk(ChunkPos pos) {
        cachedChunks.remove(pos.toLong());
    }

    public static synchronized void clearCache() {
        cachedChunks.clear();
    }

    public static synchronized int getCacheSize() {
        return cachedChunks.size();
    }

    public static int getMaxCacheSize() {
        return maxCacheSize;
    }

    public static void setMaxCacheSize(int size) {
        maxCacheSize = Math.max(100, Math.min(50000, size));
    }
}
