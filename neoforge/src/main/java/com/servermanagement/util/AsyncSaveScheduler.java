package com.servermanagement.util;

import com.servermanagement.ServerManagementMod;

import java.util.concurrent.*;

/**
 * Manages asynchronous save operations with batching and debouncing.
 * Prevents excessive disk I/O by batching multiple save requests.
 * Uses virtual threads (Java 21) for save execution to reduce platform thread overhead,
 * with a small scheduled executor for debounce timing.
 */
public class AsyncSaveScheduler {
    // Small scheduler for debounce delays only — not for actual I/O work
    private final ScheduledExecutorService delayScheduler;
    // Virtual-thread executor for actual save I/O
    private final ExecutorService saveExecutor;
    private final ConcurrentHashMap<String, SaveTask> pendingSaves;
    private final long debounceMs;
    private final Thread shutdownHook;
    private volatile boolean isShutdown = false;
    private static final java.util.concurrent.atomic.AtomicInteger poolCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    
    public AsyncSaveScheduler(int threadPoolSize, long debounceMs) {
        this.delayScheduler = Executors.newScheduledThreadPool(
            1,
            r -> {
                Thread thread = new Thread(r, "ServerManagement-SaveScheduler");
                thread.setDaemon(true);
                return thread;
            }
        );
        this.saveExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.pendingSaves = new ConcurrentHashMap<>();
        this.debounceMs = debounceMs;
        
        int id = poolCounter.incrementAndGet();
        this.shutdownHook = new Thread(() -> {
            try {
                this.flushAll();
            } catch (Exception e) {
                // Ignore during shutdown
            }
        }, "ServerManagement-SaveShutdownHook-" + id);
        try {
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        } catch (IllegalStateException e) {
            // Already shutting down
        }
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
        
        // Schedule debounce delay, then dispatch to virtual thread for I/O
        ScheduledFuture<?> future = delayScheduler.schedule(() -> {
            saveExecutor.execute(() -> {
                try {
                    saveOperation.run();
                    PerformanceMetrics.getInstance().recordSave();
                    pendingSaves.remove(key);
                } catch (Exception e) {
                    ServerManagementMod.LOGGER.error("Error during async save for key: {}", key, e);
                }
            });
        }, debounceMs, TimeUnit.MILLISECONDS);
        
        pendingSaves.put(key, new SaveTask(future, saveOperation));
    }
    
    /**
     * Immediately execute all pending saves and wait for completion.
     * Used during server shutdown.
     */
    public void flushAll() {
        if (pendingSaves.isEmpty()) {
            return;
        }
        
        ServerManagementMod.LOGGER.info("Flushing {} pending save operations", pendingSaves.size());
        
        // Cancel all scheduled futures
        for (SaveTask task : pendingSaves.values()) {
            task.future.cancel(false);
        }
        
        // Execute all save operations immediately on virtual threads
        CompletableFuture<?>[] futures = pendingSaves.values().stream()
            .map(task -> CompletableFuture.runAsync(task.operation, saveExecutor))
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
        if (isShutdown) return;
        isShutdown = true;
        
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // Already shutting down
        }
        
        flushAll();
        delayScheduler.shutdown();
        saveExecutor.close();
        try {
            if (!delayScheduler.awaitTermination(15, TimeUnit.SECONDS)) {
                delayScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            delayScheduler.shutdownNow();
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
