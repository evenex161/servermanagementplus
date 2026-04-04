package com.servermanagement.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple LRU cache with time-based expiration.
 * Thread-safe and optimized for concurrent access.
 * Uses LinkedHashMap for O(1) LRU eviction instead of linear scan.
 */
public class ExpiringCache<K, V> {
    private final Map<K, CacheEntry<V>> cache;
    private final long expirationMs;
    private final int maxSize;
    
    public ExpiringCache(int maxSize, long expirationMs) {
        // LinkedHashMap with access-order for LRU; synchronized for thread safety
        this.cache = Collections.synchronizedMap(new LinkedHashMap<K, CacheEntry<V>>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > maxSize;
            }
        });
        this.expirationMs = expirationMs;
        this.maxSize = maxSize;
    }
    
    /**
     * Get value from cache
     */
    public V get(K key) {
        synchronized (cache) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                if (entry.isExpired()) {
                    cache.remove(key);
                    PerformanceMetrics.getInstance().recordCacheMiss();
                    return null;
                }
                entry.updateAccess();
                PerformanceMetrics.getInstance().recordCacheHit();
                return entry.value;
            }
        }
        PerformanceMetrics.getInstance().recordCacheMiss();
        return null;
    }
    
    /**
     * Put value in cache (LRU eviction is automatic via LinkedHashMap)
     */
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, expirationMs));
    }
    
    /**
     * Invalidate specific key
     */
    public void invalidate(K key) {
        cache.remove(key);
    }
    
    /**
     * Clear all entries
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Remove expired entries
     */
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    private static class CacheEntry<V> {
        final V value;
        final long expirationTime;
        long lastAccess;
        
        CacheEntry(V value, long ttlMs) {
            this.value = value;
            this.lastAccess = System.currentTimeMillis();
            this.expirationTime = this.lastAccess + ttlMs;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
        
        void updateAccess() {
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
