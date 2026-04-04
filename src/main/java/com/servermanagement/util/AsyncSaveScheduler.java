package com.servermanagement.util;

import com.servermanagement.ServerManagementMod;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages asynchronous save operations with batching and debouncing.
 * Prevents excessive disk I/O by batching multiple save requests.
 */
public class AsyncSaveScheduler {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, SaveTask> pendingSaves;
    private final long debounceMs;
    
    public AsyncSaveScheduler(int threadPoolSize, long debounceMs) {
        this.scheduler = Executors.newScheduledThreadPool(
            threadPoolSize,
            r -> {
                Thread thread = new Thread(r, "ServerManagement-SaveWorker");
                thread.setDaemon(true);
                return thread;
            }
        );
        this.pendingSaves = new ConcurrentHashMap<>();
        this.debounceMs = debounceMs;
    }
    
    /**
     * Schedule a save operation with debouncing.
     * Multiple calls to the same key within debounce period will only execute once.
     */
    public void scheduleSave(String key, Runnable saveOperation) {
        SaveTask existingTask = pendingSaves.get(key);
        
        if (existingTask != null) {
            // Cancel existing scheduled save
            existingTask.future.cancel(false);
            PerformanceMetrics.getInstance().recordDebouncedSave();
        }
        
        // Schedule new save
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                saveOperation.run();
                PerformanceMetrics.getInstance().recordSave();
                pendingSaves.remove(key);
            } catch (Exception e) {
                ServerManagementMod.LOGGER.error("Error during async save for key: {}", key, e);
            }
        }, debounceMs, TimeUnit.MILLISECONDS);
        
        pendingSaves.put(key, new SaveTask(future, saveOperation));
    }
    
    /**
     * Immediately execute all pending saves and wait for completion.
     * Used during server shutdown.
     */
    public void flushAll() {
        ServerManagementMod.LOGGER.info("Flushing {} pending save operations", pendingSaves.size());
        
        // Cancel all scheduled futures
        for (SaveTask task : pendingSaves.values()) {
            task.future.cancel(false);
        }
        
        // Execute all save operations immediately
        CompletableFuture<?>[] futures = pendingSaves.values().stream()
            .map(task -> CompletableFuture.runAsync(task.operation, scheduler))
            .toArray(CompletableFuture[]::new);
        
        // Wait for all to complete
        try {
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            ServerManagementMod.LOGGER.error("Error flushing saves", e);
        }
        
        pendingSaves.clear();
    }
    
    /**
     * Shutdown the scheduler gracefully
     */
    public void shutdown() {
        flushAll();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get number of pending saves
     */
    public int getPendingSaveCount() {
        return pendingSaves.size();
    }
    
    private static class SaveTask {
        final ScheduledFuture<?> future;
        final Runnable operation;
        
        SaveTask(ScheduledFuture<?> future, Runnable operation) {
            this.future = future;
            this.operation = operation;
        }
    }
}
