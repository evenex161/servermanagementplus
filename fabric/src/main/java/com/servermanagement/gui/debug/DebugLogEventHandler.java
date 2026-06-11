package com.servermanagement.gui.debug;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.gui.ScreenScaler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * Forge event handler that captures ALL GUI events for debug logging.
 * Listens with LOWEST priority so it sees events after they're processed.
 *
 * Captures:
 *   - Screen open / close / init / resize
 *   - Mouse clicks and scroll
 *   - Key presses
 *   - Widget identification on click
 *   - Slot hover/click detection
 */
public final class DebugLogEventHandler {

    private static String lastOpenScreen = null;

    private DebugLogEventHandler() {}

    // ── Toggle keybind: F3+L ──

    public static void onKeyForToggle(Screen screen, int keyCode, int scanCode, int modifiers) {
        // F3+L toggles debug logging (separate from F3+M for overlay)
        if (keyCode == GLFW.GLFW_KEY_L && !new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers).hasControlDown()) {
            // Check if F3 is held
            long window = Minecraft.getInstance().getWindow().handle();
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS) {
                DebugLogger.toggle();
            }
        }
    }

    // ── Screen lifecycle ──

    public static void onScreenOpen(Screen screen) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        String className = screen.getClass().getSimpleName();
        DebugLogger.logScreenOpen(className, screen.width, screen.height);
        lastOpenScreen = className;
    }

    public static void onScreenInit(Screen screen) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        String className = screen.getClass().getSimpleName();

        if (screen instanceof AbstractContainerScreen<?> acs) {
            float sf = ScreenScaler.scaleFactor(screen.width, screen.height);
            DebugLogger.logScreenInit(className,
                    acs.imageWidth, acs.imageHeight,
                    acs.leftPos, acs.topPos, sf);

            // Log slot count
            int slotCount = acs.getMenu().slots.size();
            int activeSlots = (int) acs.getMenu().slots.stream().filter(Slot::isActive).count();
            DebugLogger.log(DebugLogger.Category.GUI,
                    "  Slots: %d total, %d active", slotCount, activeSlots);
        }
    }

    public static void onScreenClose(Screen screen) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        DebugLogger.logScreenClose(screen.getClass().getSimpleName());
        DebugLogger.flush();
        lastOpenScreen = null;
    }

    // ── Mouse events ──

    public static void onMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        String screenName = screen.getClass().getSimpleName();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        DebugLogger.logMouseClick(screenName, mx, my, button);

        // Try to identify what was clicked
        identifyClickTarget(screen, mx, my);
    }

    public static void onMouseScroll(Screen screen, double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        DebugLogger.logMouseScroll(screen.getClass().getSimpleName(), verticalAmount);
    }

    // ── Keyboard events ──

    public static void onKeyPress(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (keyName == null) {
            keyName = switch (keyCode) {
                case GLFW.GLFW_KEY_ENTER -> "ENTER";
                case GLFW.GLFW_KEY_TAB -> "TAB";
                case GLFW.GLFW_KEY_ESCAPE -> "ESC";
                case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
                case GLFW.GLFW_KEY_DELETE -> "DELETE";
                case GLFW.GLFW_KEY_LEFT -> "LEFT";
                case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
                case GLFW.GLFW_KEY_UP -> "UP";
                case GLFW.GLFW_KEY_DOWN -> "DOWN";
                default -> "KEY_" + keyCode;
            };
        }

        DebugLogger.logKeyPress(screen.getClass().getSimpleName(), keyCode, keyName);
    }

    // ── Render event — log summary periodically ──

    private static long lastSummaryTime = 0;

    public static void onRender(Screen screen, net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        if (!DebugLogger.isEnabled()) return;
        if (!isModScreen(screen)) return;

        // Log session summary every 10 seconds
        long now = System.currentTimeMillis();
        if (now - lastSummaryTime > 10000) {
            lastSummaryTime = now;
            DebugLogger.log(DebugLogger.Category.GUI, "--- %s ---", DebugLogger.getSessionSummary());
            DebugLogger.flush();
        }
    }

    // ── Helpers ──

    private static boolean isModScreen(Screen screen) {
        return screen != null && screen.getClass().getPackageName().startsWith("com.servermanagement");
    }

    /**
     * Try to identify which widget or slot was clicked at the given coordinates.
     */
    @SuppressWarnings("unchecked")
    private static void identifyClickTarget(Screen screen, int mx, int my) {
        // Check widgets
        try {
            var field = Screen.class.getDeclaredField("renderables");
            field.setAccessible(true);
            var renderables = (java.util.List<?>) field.get(screen);
            if (renderables != null) {
                for (Object r : renderables) {
                    if (r instanceof AbstractWidget widget && widget.visible) {
                        if (mx >= widget.getX() && mx < widget.getX() + widget.getWidth()
                                && my >= widget.getY() && my < widget.getY() + widget.getHeight()) {
                            String msg = widget.getMessage().getString();
                            DebugLogger.logWidgetClick(widget.getClass().getSimpleName(),
                                    msg.isEmpty() ? "(no label)" : msg, widget.getX(), widget.getY());
                        }
                    }
                }
            }
        } catch (NoSuchFieldException e) {
            // Try fallback for obfuscated field
            try {
                for (var f : Screen.class.getDeclaredFields()) {
                    if (java.util.List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        var list = (java.util.List<?>) f.get(screen);
                        if (list != null && !list.isEmpty() && list.get(0) instanceof AbstractWidget) {
                            for (Object r : list) {
                                if (r instanceof AbstractWidget widget && widget.visible) {
                                    if (mx >= widget.getX() && mx < widget.getX() + widget.getWidth()
                                            && my >= widget.getY() && my < widget.getY() + widget.getHeight()) {
                                        String msg = widget.getMessage().getString();
                                        DebugLogger.logWidgetClick(widget.getClass().getSimpleName(),
                                                msg.isEmpty() ? "(no label)" : msg, widget.getX(), widget.getY());
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        // Check slots for container screens
        if (screen instanceof AbstractContainerScreen<?> acs) {
            int guiLeft = acs.leftPos;
            int guiTop = acs.topPos;
            for (Slot slot : acs.getMenu().slots) {
                if (!slot.isActive()) continue;
                int sx = guiLeft + slot.x;
                int sy = guiTop + slot.y;
                if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                    String itemName = slot.hasItem() ? slot.getItem().getHoverName().getString() : "(empty)";
                    int count = slot.hasItem() ? slot.getItem().getCount() : 0;
                    DebugLogger.logSlotClick(slot.index, slot.getClass().getSimpleName(), itemName, count);
                }
            }
        }
    }
}
