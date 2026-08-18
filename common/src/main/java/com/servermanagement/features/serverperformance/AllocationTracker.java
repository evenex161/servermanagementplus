package com.servermanagement.features.serverperformance;

import com.servermanagement.Constants;

import java.lang.management.ManagementFactory;

/**
 * Lightweight JMX-based allocation rate tracker.
 * Measures heap allocation rate in MB/s using ThreadMXBean.
 * Only active when explicitly sampled — zero overhead when not polled.
 */
public class AllocationTracker {

    private static long lastSampleTime = 0;
    private static long lastAllocatedBytes = 0;
    private static double allocationRateMBps = 0.0;
    private static boolean supported = false;

    static {
        try {
            // Check if the com.sun.management extension is available
            var threadMXBean = ManagementFactory.getThreadMXBean();
            if (threadMXBean instanceof com.sun.management.ThreadMXBean sunBean) {
                // Test if thread allocation measurement is supported
                sunBean.getThreadAllocatedBytes(Thread.currentThread().getId());
                supported = true;
            }
        } catch (Exception e) {
            Constants.LOG.debug("AllocationTracker: ThreadMXBean allocation tracking not supported on this JVM.");
        }
    }

    /**
     * Sample current allocation state. Call once per second (or per N ticks) on the server thread.
     */
    public static void sample() {
        if (!supported) return;

        try {
            var sunBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            long[] threadIds = sunBean.getAllThreadIds();

            long totalAllocated = 0;
            long[] allocations = sunBean.getThreadAllocatedBytes(threadIds);
            for (long alloc : allocations) {
                if (alloc > 0) totalAllocated += alloc;
            }

            long now = System.nanoTime();
            if (lastSampleTime > 0) {
                double elapsedSeconds = (now - lastSampleTime) / 1_000_000_000.0;
                if (elapsedSeconds > 0.01) {
                    long deltaBytes = totalAllocated - lastAllocatedBytes;
                    allocationRateMBps = (deltaBytes / (1024.0 * 1024.0)) / elapsedSeconds;
                }
            }

            lastSampleTime = now;
            lastAllocatedBytes = totalAllocated;
        } catch (Exception e) {
            // Silently ignore — JMX quirks on some JVMs
        }
    }

    public static double getAllocationRateMBps() {
        return allocationRateMBps;
    }

    public static boolean isSupported() {
        return supported;
    }
}
