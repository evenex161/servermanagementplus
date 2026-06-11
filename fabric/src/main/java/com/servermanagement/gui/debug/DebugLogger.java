package com.servermanagement.gui.debug;

import com.servermanagement.ServerManagementMod;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive debug logger for ServerManagement+.
 * Writes EVERYTHING that happens behind the scenes to a dedicated log file.
 * Toggle with F3+L in-game, or call DebugLogger.setEnabled(true).
 *
 * Log file: .minecraft/servermanagement-debug.log (or run/servermanagement-debug.log in dev)
 *
 * Categories:
 *   GUI       - Screen lifecycle, rendering, scaling
 *   NETWORK   - Packets sent and received
 *   STATE     - Screen state transitions, tab changes
 *   WIDGET    - Button clicks, toggle changes, EditBox input
 *   SLOT      - Inventory slot interactions
 *   CONFIG    - Config value changes
 *   FEATURE   - Feature enable/disable
 *   INPUT     - Mouse and keyboard events
 *   CACHE     - ClientPacketHandler data cache updates
 *   MENU      - Menu/container events
 *   ERROR     - Exceptions and error conditions
 */
public final class DebugLogger {

    public enum Category {
        GUI, NETWORK, STATE, WIDGET, SLOT, CONFIG, FEATURE, INPUT, CACHE, MENU, ERROR
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String SEPARATOR = "═".repeat(100);
    private static final int MAX_QUEUE_SIZE = 10000;

    private static volatile boolean enabled = false;
    private static volatile boolean initialized = false;
    private static final AtomicBoolean flushing = new AtomicBoolean(false);
    private static final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private static Path logFilePath;
    private static long sessionStartMillis;

    // Packet counters
    private static long packetsSent = 0;
    private static long packetsReceived = 0;
    private static long screenOpens = 0;
    private static long widgetClicks = 0;
    private static long stateChanges = 0;

    private DebugLogger() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        if (state && !enabled) {
            initialize();
            enabled = true;
            log(Category.GUI, "=== Debug logging ENABLED ===");
            logSystemInfo();
        } else if (!state && enabled) {
            log(Category.GUI, "=== Debug logging DISABLED === (Sent: %d, Received: %d, Screens: %d, Clicks: %d, States: %d)",
                    packetsSent, packetsReceived, screenOpens, widgetClicks, stateChanges);
            flush();
            enabled = false;
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Log a debug message under a category.
     */
    public static void log(Category category, String message, Object... args) {
        if (!enabled) return;

        String formatted;
        try {
            formatted = args.length > 0 ? String.format(message, args) : message;
        } catch (Exception e) {
            formatted = message + " [FORMAT_ERROR: " + e.getMessage() + "]";
        }

        String timestamp = LocalDateTime.now().format(TIME_FMT);
        long elapsed = System.currentTimeMillis() - sessionStartMillis;
        String entry = String.format("[%s] [+%6dms] [%-8s] %s", timestamp, elapsed, category, formatted);

        // Also echo to game log at debug level
        ServerManagementMod.LOGGER.debug("[SM-DBG][{}] {}", category, formatted);

        // Queue for file write
        if (logQueue.size() < MAX_QUEUE_SIZE) {
            logQueue.add(entry);
        }

        // Auto-flush every 50 entries
        if (logQueue.size() >= 50) {
            flush();
        }
    }

    // ════════════════════════════════════════════════════════════
    // GUI / Screen lifecycle
    // ════════════════════════════════════════════════════════════

    public static void logScreenOpen(String screenClass, int screenWidth, int screenHeight) {
        screenOpens++;
        log(Category.GUI, SEPARATOR);
        log(Category.GUI, "SCREEN OPENED: %s", screenClass);
        log(Category.GUI, "  Window: %dx%d", screenWidth, screenHeight);
    }

    public static void logScreenInit(String screenClass, int imageWidth, int imageHeight,
                                      int leftPos, int topPos, float scaleFactor) {
        log(Category.GUI, "SCREEN INIT: %s", screenClass);
        log(Category.GUI, "  Panel size: %dx%d", imageWidth, imageHeight);
        log(Category.GUI, "  Panel position: (%d, %d)", leftPos, topPos);
        log(Category.GUI, "  Scale factor: %.3f", scaleFactor);
    }

    public static void logScreenClose(String screenClass) {
        log(Category.GUI, "SCREEN CLOSED: %s", screenClass);
        log(Category.GUI, SEPARATOR);
    }

    public static void logScreenResize(String screenClass, int newWidth, int newHeight) {
        log(Category.GUI, "SCREEN RESIZE: %s → %dx%d", screenClass, newWidth, newHeight);
    }

    // ════════════════════════════════════════════════════════════
    // Network / Packets
    // ════════════════════════════════════════════════════════════

    public static void logPacketSent(Object packet) {
        packetsSent++;
        String name = packet.getClass().getSimpleName();
        String details = extractPacketDetails(packet);
        log(Category.NETWORK, "→ SENT: %s%s", name, details);
    }

    public static void logPacketReceived(Object packet) {
        packetsReceived++;
        String name = packet.getClass().getSimpleName();
        String details = extractPacketDetails(packet);
        log(Category.NETWORK, "← RECV: %s%s", name, details);
    }

    public static void logPacketHandled(String packetName, String result) {
        log(Category.NETWORK, "  HANDLED: %s → %s", packetName, result);
    }

    // ════════════════════════════════════════════════════════════
    // State transitions
    // ════════════════════════════════════════════════════════════

    public static void logStateChange(String screen, String field, Object oldValue, Object newValue) {
        stateChanges++;
        log(Category.STATE, "STATE CHANGE in %s: %s: %s → %s", screen, field, oldValue, newValue);
    }

    public static void logTabChange(String screen, String oldTab, String newTab) {
        stateChanges++;
        log(Category.STATE, "TAB CHANGE in %s: %s → %s", screen, oldTab, newTab);
    }

    // ════════════════════════════════════════════════════════════
    // Widget interactions
    // ════════════════════════════════════════════════════════════

    public static void logWidgetClick(String widgetClass, String label, int x, int y) {
        widgetClicks++;
        log(Category.WIDGET, "CLICK: %s \"%s\" at (%d, %d)", widgetClass, label, x, y);
    }

    public static void logToggleChange(String label, boolean newState) {
        log(Category.WIDGET, "TOGGLE: \"%s\" → %s", label, newState ? "ON" : "OFF");
    }

    public static void logEditBoxChange(String fieldName, String newValue) {
        log(Category.WIDGET, "EDIT: \"%s\" = \"%s\"", fieldName, truncate(newValue, 50));
    }

    public static void logButtonAction(String screen, String buttonLabel, String action) {
        widgetClicks++;
        log(Category.WIDGET, "BUTTON in %s: \"%s\" → %s", screen, buttonLabel, action);
    }

    // ════════════════════════════════════════════════════════════
    // Slot / inventory
    // ════════════════════════════════════════════════════════════

    public static void logSlotClick(int slotIndex, String slotType, String itemName, int count) {
        log(Category.SLOT, "SLOT CLICK: #%d (%s) item=%s x%d", slotIndex, slotType, itemName, count);
    }

    public static void logSlotVisibilityChange(String context, int slotIndex, boolean visible) {
        log(Category.SLOT, "SLOT VISIBILITY: #%d in %s → %s", slotIndex, context, visible ? "VISIBLE" : "HIDDEN");
    }

    public static void logInventoryToggle(String screen, boolean visible) {
        log(Category.SLOT, "INVENTORY TOGGLE in %s → %s", screen, visible ? "SHOWN" : "HIDDEN");
    }

    // ════════════════════════════════════════════════════════════
    // Config / Feature
    // ════════════════════════════════════════════════════════════

    public static void logConfigChange(String key, Object oldValue, Object newValue) {
        log(Category.CONFIG, "CONFIG: %s: %s → %s", key, oldValue, newValue);
    }

    public static void logFeatureToggle(String featureId, boolean enabled) {
        log(Category.FEATURE, "FEATURE: %s → %s", featureId, enabled ? "ENABLED" : "DISABLED");
    }

    // ════════════════════════════════════════════════════════════
    // Input events
    // ════════════════════════════════════════════════════════════

    public static void logMouseClick(String screen, int mouseX, int mouseY, int button) {
        log(Category.INPUT, "MOUSE CLICK in %s: (%d, %d) button=%d", screen, mouseX, mouseY, button);
    }

    public static void logKeyPress(String screen, int keyCode, String keyName) {
        log(Category.INPUT, "KEY PRESS in %s: %d (%s)", screen, keyCode, keyName);
    }

    public static void logMouseScroll(String screen, double scrollDelta) {
        log(Category.INPUT, "SCROLL in %s: %.1f", screen, scrollDelta);
    }

    // ════════════════════════════════════════════════════════════
    // Cache updates (ClientPacketHandler)
    // ════════════════════════════════════════════════════════════

    public static void logCacheUpdate(String cacheGroup, String details) {
        log(Category.CACHE, "CACHE UPDATE [%s]: %s", cacheGroup, details);
    }

    // ════════════════════════════════════════════════════════════
    // Menu / Container
    // ════════════════════════════════════════════════════════════

    public static void logMenuOpened(String menuClass, int containerId) {
        log(Category.MENU, "MENU OPENED: %s (id=%d)", menuClass, containerId);
    }

    public static void logMenuClosed(String menuClass) {
        log(Category.MENU, "MENU CLOSED: %s", menuClass);
    }

    // ════════════════════════════════════════════════════════════
    // Errors
    // ════════════════════════════════════════════════════════════

    public static void logError(String context, Throwable error) {
        log(Category.ERROR, "ERROR in %s: %s: %s", context, error.getClass().getSimpleName(), error.getMessage());
        // Log first 5 stack frames
        StackTraceElement[] stack = error.getStackTrace();
        int frames = Math.min(5, stack.length);
        for (int i = 0; i < frames; i++) {
            log(Category.ERROR, "  at %s", stack[i].toString());
        }
    }

    public static void logWarning(String context, String message, Object... args) {
        log(Category.ERROR, "WARNING in %s: %s", context, args.length > 0 ? String.format(message, args) : message);
    }

    // ════════════════════════════════════════════════════════════
    // Internal helpers
    // ════════════════════════════════════════════════════════════

    private static void initialize() {
        if (initialized) return;
        try {
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            logFilePath = gameDir.resolve("servermanagement-debug.log");

            // Write session header
            sessionStartMillis = System.currentTimeMillis();
            String header = String.format(
                    "%s%n=== ServerManagement+ Debug Log ===%n=== Session started: %s ===%n=== Mod version: %s ===%n%s%n",
                    SEPARATOR, LocalDateTime.now().format(FILE_TIME_FMT),
                    ServerManagementMod.getModVersion(), SEPARATOR);

            Files.writeString(logFilePath, header,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            initialized = true;
            ServerManagementMod.LOGGER.info("Debug log initialized at: {}", logFilePath);
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to initialize debug log", e);
        }
    }

    private static void logSystemInfo() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var window = mc.getWindow();
        log(Category.GUI, "System info:");
        log(Category.GUI, "  MC version: %s", net.minecraft.SharedConstants.getCurrentVersion().name());
        log(Category.GUI, "  Window: %dx%d (GUI scale: %.1f)",
                window.getWidth(), window.getHeight(), window.getGuiScale());
        log(Category.GUI, "  GUI scaled: %dx%d", window.getGuiScaledWidth(), window.getGuiScaledHeight());
        log(Category.GUI, "  ScreenScaler ref: 1010x570, min scale: 0.85");
        Runtime rt = Runtime.getRuntime();
        log(Category.GUI, "  Memory: %dMB / %dMB",
                (rt.totalMemory() - rt.freeMemory()) / 1048576, rt.maxMemory() / 1048576);
    }

    /**
     * Extract meaningful details from a packet using reflection on common field patterns.
     */
    private static String extractPacketDetails(Object packet) {
        try {
            var sb = new StringBuilder();
            for (var field : packet.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object val = field.get(packet);
                if (val == null) continue;
                String name = field.getName();
                // Skip large data fields
                if (val instanceof byte[] bytes) {
                    sb.append(" ").append(name).append("=<").append(bytes.length).append(" bytes>");
                } else if (val instanceof java.util.List<?> list) {
                    sb.append(" ").append(name).append("=[").append(list.size()).append(" items]");
                } else if (val instanceof java.util.Map<?, ?> map) {
                    sb.append(" ").append(name).append("={").append(map.size()).append(" entries}");
                } else {
                    String str = val.toString();
                    sb.append(" ").append(name).append("=").append(truncate(str, 40));
                }
            }
            return sb.length() > 0 ? " |" + sb : "";
        } catch (Exception e) {
            return "";
        }
    }

    static void flush() {
        if (!initialized || logQueue.isEmpty()) return;
        if (!flushing.compareAndSet(false, true)) return;

        try (BufferedWriter writer = Files.newBufferedWriter(logFilePath,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String entry;
            while ((entry = logQueue.poll()) != null) {
                writer.write(entry);
                writer.newLine();
            }
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to write debug log", e);
        } finally {
            flushing.set(false);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    /**
     * Get a summary of the current session stats.
     */
    public static String getSessionSummary() {
        long elapsed = System.currentTimeMillis() - sessionStartMillis;
        return String.format("Session: %ds | Sent: %d | Recv: %d | Screens: %d | Clicks: %d | States: %d",
                elapsed / 1000, packetsSent, packetsReceived, screenOpens, widgetClicks, stateChanges);
    }
}
