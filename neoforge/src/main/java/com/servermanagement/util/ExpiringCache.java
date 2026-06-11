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
        // Plain LinkedHashMap with access-order for LRU
        this.cache = new LinkedHashMap<K, CacheEntry<V>>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > maxSize;
            }
        };
        this.expirationMs = expirationMs;
        this.maxSize = maxSize;
    }
    
    /**
     * Get value from cache
     */
    public V get(K key) {
        CacheEntry<V> entry;
        synchronized (cache) {
            entry = cache.get(key);
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
                entry = null;
            }
            if (entry != null) {
                entry.updateAccess();
            }
        }
        if (entry != null) {
            PerformanceMetrics.getInstance().recordCacheHit();
            return entry.value;
        }
        PerformanceMetrics.getInstance().recordCacheMiss();
        return null;
    }
    
    /**
     * Put value in cache
     */
    public void put(K key, V value) {
        synchronized (cache) {
            cache.put(key, new CacheEntry<>(value, expirationMs));
        }
    }
    
    /**
     * Invalidate specific key
     */
    public void invalidate(K key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }
    
    /**
     * Clear all entries
     */
    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }
    
    /**
     * Get cache size
     */
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
    
    /**
     * Remove expired entries
     */
    public void cleanupExpired() {
        synchronized (cache) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }
    
    private static class CacheEntry<V> {
        final V value;
        final long ttlMs;
        long lastAccess;
        
        CacheEntry(V value, long ttlMs) {
            this.value = value;
            this.ttlMs = ttlMs;
            this.lastAccess = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastAccess > ttlMs;
        }
        
        void updateAccess() {
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
