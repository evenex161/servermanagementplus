package com.servermanagement.features.serverperformance;

import com.servermanagement.Constants;
import com.servermanagement.integration.dh.DistantHorizonsHook;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Set;

/**
 * Detects the active JVM garbage collector and advises on optimal GC configuration.
 * Especially important for Distant Horizons compatibility, which suffers heavily
 * under G1GC's stop-the-world pauses.
 */
public class GCAdvisor {

    public enum GCType {
        ZGC("ZGC", false),
        SHENANDOAH("Shenandoah", false),
        G1GC("G1GC", true),
        PARALLEL("Parallel GC", true),
        SERIAL("Serial GC", true),
        UNKNOWN("Unknown", false);

        private final String displayName;
        private final boolean suboptimal;

        GCType(String displayName, boolean suboptimal) {
            this.displayName = displayName;
            this.suboptimal = suboptimal;
        }

        public String getDisplayName() { return displayName; }
        public boolean isSuboptimal() { return suboptimal; }
    }

    public enum UrgencyLevel {
        OK,           // ZGC or Shenandoah — all good
        WARNING,      // G1GC standalone — works but can stutter
        CRITICAL      // G1GC + Distant Horizons — compounded stuttering
    }

    private static GCType detectedGC = GCType.UNKNOWN;
    private static UrgencyLevel urgency = UrgencyLevel.OK;
    private static long maxHeapMB = 0;
    private static boolean initialized = false;
    private static volatile boolean scriptPatched = false;
    private static volatile boolean dismissed = false;

    /**
     * Call once during mod initialization (both client and server).
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        detectedGC = detectGarbageCollector();
        maxHeapMB = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / (1024 * 1024);

        // Determine urgency
        if (!detectedGC.isSuboptimal()) {
            urgency = UrgencyLevel.OK;
            Constants.LOG.info("GC Advisor: {} detected — optimal for Minecraft.", detectedGC.getDisplayName());
        } else {
            boolean dhPresent = DistantHorizonsHook.isAvailable();
            urgency = dhPresent ? UrgencyLevel.CRITICAL : UrgencyLevel.WARNING;

            if (urgency == UrgencyLevel.CRITICAL) {
                Constants.LOG.warn("GC Advisor: {} + Distant Horizons detected. FPS stuttering likely. " +
                    "Recommended: add -XX:+UseZGC -XX:+ZGenerational to your JVM arguments.", detectedGC.getDisplayName());
            } else {
                Constants.LOG.info("GC Advisor: {} detected. For smoother performance, consider switching to ZGC: " +
                    "-XX:+UseZGC -XX:+ZGenerational", detectedGC.getDisplayName());
            }
        }

        Constants.LOG.debug("GC Advisor: Max heap = {}MB, Java version = {}", maxHeapMB,
            Runtime.version().toString());
    }

    private static GCType detectGarbageCollector() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : gcBeans) {
            String name = bean.getName().toLowerCase();
            if (name.contains("zgc")) return GCType.ZGC;
            if (name.contains("shenandoah")) return GCType.SHENANDOAH;
            if (name.contains("g1")) return GCType.G1GC;
            if (name.contains("ps") || name.contains("parallel")) return GCType.PARALLEL;
            if (name.contains("copy") || name.contains("marksweep")) return GCType.SERIAL;
        }
        return GCType.UNKNOWN;
    }

    // --- Public API ---

    public static GCType getDetectedGC() { return detectedGC; }
    public static UrgencyLevel getUrgency() { return urgency; }
    public static boolean isUsingSuboptimalGC() { return detectedGC.isSuboptimal(); }
    public static boolean isScriptPatched() { return scriptPatched; }
    public static void setScriptPatched(boolean patched) { scriptPatched = patched; }
    public static boolean isDismissed() { return dismissed; }
    public static void setDismissed(boolean value) { dismissed = value; }

    /**
     * Returns the set of conflicting GC flags that should be removed when patching scripts.
     */
    public static Set<String> getRemovalFlags() {
        return Set.of(
            "-XX:+UseG1GC", "-XX:+UseParallelGC", "-XX:+UseSerialGC",
            "-XX:+UseParNewGC", "-XX:+UseConcMarkSweepGC"
        );
    }
    public static long getMaxHeapMB() { return maxHeapMB; }

    /**
     * Returns the recommended JVM flags for the current Java version.
     */
    public static String getRecommendedFlags() {
        int javaVersion = Runtime.version().feature();
        if (javaVersion >= 21) {
            return "-XX:+UseZGC -XX:+ZGenerational";
        } else if (javaVersion >= 17) {
            return "-XX:+UseZGC";
        } else {
            return "-XX:+UseShenandoahGC";
        }
    }

    /**
     * Returns a full recommended JVM argument line including memory settings.
     */
    public static String getFullRecommendedArgs(boolean isServer) {
        StringBuilder sb = new StringBuilder();
        sb.append(getRecommendedFlags());
        sb.append(" -XX:+AlwaysPreTouch");
        if (isServer) {
            sb.append(" -Xms6G -Xmx6G");
        } else {
            sb.append(" -Xms4G -Xmx4G");
        }
        return sb.toString();
    }

    /**
     * Returns total GC pause time in milliseconds across all collectors.
     */
    public static long getTotalGCPauseMs() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = bean.getCollectionTime();
            if (time > 0) total += time;
        }
        return total;
    }

    /**
     * Returns total GC collection count across all collectors.
     */
    public static long getTotalGCCount() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count > 0) total += count;
        }
        return total;
    }
}
