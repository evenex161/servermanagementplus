package com.servermanagement.util;

import com.servermanagement.ServerManagementMod;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks performance metrics for the mod (TPS impact, save frequency, cache hit rates)
 * Lightweight monitoring with minimal overhead using atomic operations.
 */
public class PerformanceMetrics {
    private static PerformanceMetrics instance;
    
    // Save metrics
    private final LongAdder totalSaves = new LongAdder();
    private final LongAdder debouncedSaves = new LongAdder(); // Saves that were skipped due to debouncing
    private final AtomicLong lastSaveTime = new AtomicLong(0);
    
    // Cache metrics
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    
    // Encryption metrics
    private final LongAdder encryptOperations = new LongAdder();
    private final LongAdder decryptOperations = new LongAdder();
    
    // Task completion metrics
    private final LongAdder tasksCompleted = new LongAdder();
    
    // Timer metrics
    private final LongAdder timerTicks = new LongAdder();
    
    // Tick timing (simple moving average)
    private final ConcurrentHashMap<String, TickStats> tickTimings = new ConcurrentHashMap<>();
    
    private PerformanceMetrics() {}
    
    public static PerformanceMetrics getInstance() {
        if (instance == null) {
            instance = new PerformanceMetrics();
        }
        return instance;
    }
    
    // Save metrics
    public void recordSave() {
        totalSaves.increment();
        lastSaveTime.set(System.currentTimeMillis());
    }
    
    public void recordDebouncedSave() {
        debouncedSaves.increment();
    }
    
    public long getTotalSaves() {
        return totalSaves.sum();
    }
    
    public long getDebouncedSaves() {
        return debouncedSaves.sum();
    }
    
    public long getTimeSinceLastSave() {
        long last = lastSaveTime.get();
        return last > 0 ? System.currentTimeMillis() - last : -1;
    }
    
    // Cache metrics
    public void recordCacheHit() {
        cacheHits.increment();
    }
    
    public void recordCacheMiss() {
        cacheMisses.increment();
    }
    
    public long getCacheHits() {
        return cacheHits.sum();
    }
    
    public long getCacheMisses() {
        return cacheMisses.sum();
    }
    
    public double getCacheHitRate() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100.0 : 0.0;
    }
    
    // Encryption metrics
    public void recordEncrypt() {
        encryptOperations.increment();
    }
    
    public void recordDecrypt() {
        decryptOperations.increment();
    }
    
    public long getEncryptOperations() {
        return encryptOperations.sum();
    }
    
    public long getDecryptOperations() {
        return decryptOperations.sum();
    }
    
    // Task metrics
    public void recordTaskCompletion() {
        tasksCompleted.increment();
    }
    
    public long getTasksCompleted() {
        return tasksCompleted.sum();
    }
    
    // Timer metrics
    public void recordTimerTick() {
        timerTicks.increment();
    }
    
    public long getTimerTicks() {
        return timerTicks.sum();
    }
    
    // Tick timing (for measuring system overhead)
    public void recordTickTime(String system, long nanos) {
        tickTimings.computeIfAbsent(system, k -> new TickStats()).addSample(nanos);
    }
    
    public TickStats getTickStats(String system) {
        return tickTimings.get(system);
    }
    
    /**
     * Get comprehensive metrics report
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ServerManagement Performance Metrics ===\n");
        
        // Save metrics
        long totalSaves = getTotalSaves();
        long debouncedSaves = getDebouncedSaves();
        double saveEfficiency = totalSaves > 0 ? (double) debouncedSaves / (totalSaves + debouncedSaves) * 100.0 : 0.0;
        sb.append(String.format("Saves: %d total, %d debounced (%.1f%% reduction)\n", 
            totalSaves, debouncedSaves, saveEfficiency));
        
        long timeSinceSave = getTimeSinceLastSave();
        if (timeSinceSave >= 0) {
            sb.append(String.format("Last save: %.1f seconds ago\n", timeSinceSave / 1000.0));
        }
        
        // Cache metrics
        long hits = getCacheHits();
        long misses = getCacheMisses();
        double hitRate = getCacheHitRate();
        sb.append(String.format("Cache: %d hits, %d misses (%.1f%% hit rate)\n", hits, misses, hitRate));
        
        // Encryption metrics
        sb.append(String.format("Encryption: %d encrypt, %d decrypt operations\n", 
            getEncryptOperations(), getDecryptOperations()));
        
        // Task metrics
        sb.append(String.format("Tasks completed: %d\n", getTasksCompleted()));
        
        // Timer metrics
        sb.append(String.format("Timer ticks: %d\n", getTimerTicks()));
        
        // Tick timings
        if (!tickTimings.isEmpty()) {
            sb.append("\nSystem Tick Timings (avg µs):\n");
            tickTimings.forEach((system, stats) -> {
                sb.append(String.format("  %s: %.2f µs (%.2f ms/sec @ 20 TPS)\n", 
                    system, stats.getAverageMicros(), stats.getAverageMicros() * 20 / 1000.0));
            });
        }
        
        return sb.toString();
    }
    
    /**
     * Reset all metrics (useful for benchmarking)
     */
    public void reset() {
        totalSaves.reset();
        debouncedSaves.reset();
        lastSaveTime.set(0);
        cacheHits.reset();
        cacheMisses.reset();
        encryptOperations.reset();
        decryptOperations.reset();
        tasksCompleted.reset();
        timerTicks.reset();
        tickTimings.clear();
    }
    
    /**
     * Log metrics to console (call periodically or on command)
     */
    public void logReport() {
        ServerManagementMod.LOGGER.info("\n" + getReport());
    }
    
    /**
     * Thread-safe tick statistics tracker
     */
    public static class TickStats {
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder sampleCount = new LongAdder();
        private final AtomicLong maxNanos = new AtomicLong(0);
        
        public void addSample(long nanos) {
            totalNanos.add(nanos);
            sampleCount.increment();
            
            // Update max (thread-safe compare-and-swap)
            long currentMax = maxNanos.get();
            while (nanos > currentMax && !maxNanos.compareAndSet(currentMax, nanos)) {
                currentMax = maxNanos.get();
            }
        }
        
        public double getAverageMicros() {
            long samples = sampleCount.sum();
            return samples > 0 ? totalNanos.sum() / (double) samples / 1000.0 : 0.0;
        }
        
        public double getMaxMicros() {
            return maxNanos.get() / 1000.0;
        }
        
        public long getSampleCount() {
            return sampleCount.sum();
        }
    }
}
