package com.servermanagement.gui.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Visual debug overlay for GUI screens.
 * Toggle with F3+M in-game. Shows:
 * - Widget bounding boxes (green outlines)
 * - Active/inactive widget state (green vs red)
 * - Slot positions and hit areas
 * - Mouse coordinates
 * - Screen dimensions and scale info
 * - Scissor clipping region indicators
 * - Hovered widget details
 */
public final class GuiDebugOverlay {

    private static boolean enabled = false;
    private static boolean showSlots = true;
    private static boolean showWidgets = true;

    private GuiDebugOverlay() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    /**
     * Render the debug overlay on top of the current screen.
     * Call this at the END of any Screen's render() method.
     */
    public static void render(GuiGraphics guiGraphics, Screen screen, int mouseX, int mouseY) {
        if (!enabled) return;

        var font = Minecraft.getInstance().font;
        int screenW = screen.width;
        int screenH = screen.height;

        // ── Panel dimensions (for AbstractContainerScreens) ──
        int panelX = 0, panelY = 0, panelW = screenW, panelH = screenH;
        if (screen instanceof AbstractContainerScreen<?> acs) {
            panelX = acs.getGuiLeft();
            panelY = acs.getGuiTop();
            panelW = acs.getXSize();
            panelH = acs.getYSize();

            // Draw panel bounding box (cyan)
            drawOutline(guiGraphics, panelX, panelY, panelW, panelH, 0xCC00FFFF);

            // Draw panel origin crosshair
            guiGraphics.fill(panelX - 3, panelY, panelX + 4, panelY + 1, 0xCCFF00FF);
            guiGraphics.fill(panelX, panelY - 3, panelX + 1, panelY + 4, 0xCCFF00FF);
        }

        // ── Widget bounding boxes ──
        if (showWidgets) {
            // Access renderables through reflection-free approach: iterate children
            List<? extends Renderable> renderables = getRenderables(screen);
            if (renderables != null) {
                for (Renderable r : renderables) {
                    if (r instanceof AbstractWidget widget) {
                        renderWidgetDebug(guiGraphics, widget, mouseX, mouseY, font);
                    }
                }
            }
        }

        // ── Slot debug (for container screens) ──
        if (showSlots && screen instanceof AbstractContainerScreen<?> acs) {
            renderSlotDebug(guiGraphics, acs, mouseX, mouseY, font);
        }

        // ── Info panel (top-left corner) ──
        int infoX = 4;
        int infoY = 4;
        int lineH = 10;

        // Background for info panel
        int panelLines = 7 + (DebugLogger.isEnabled() ? 2 : 1);
        guiGraphics.fill(infoX - 2, infoY - 2, infoX + 250, infoY + lineH * panelLines + 4, 0xCC000000);

        guiGraphics.drawString(font, "[GUI Debug] F3+M overlay | F3+L logging", infoX, infoY, 0xFF00FF, false);
        infoY += lineH;

        guiGraphics.drawString(font,
                "Screen: " + screenW + "x" + screenH + " (GUI units)",
                infoX, infoY, 0xCCCCCC, false);
        infoY += lineH;

        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        guiGraphics.drawString(font,
                "GUI Scale: " + String.format("%.1f", guiScale),
                infoX, infoY, 0xCCCCCC, false);
        infoY += lineH;

        if (screen instanceof AbstractContainerScreen<?>) {
            guiGraphics.drawString(font,
                    "Panel: " + panelW + "x" + panelH + " at (" + panelX + "," + panelY + ")",
                    infoX, infoY, 0x00FFFF, false);
            infoY += lineH;

            float scaleFactor = com.servermanagement.gui.ScreenScaler.scaleFactor(screenW, screenH);
            guiGraphics.drawString(font,
                    "ScreenScaler: " + String.format("%.3f", scaleFactor),
                    infoX, infoY, 0x00FFFF, false);
            infoY += lineH;
        }

        guiGraphics.drawString(font,
                "Mouse: (" + mouseX + ", " + mouseY + ")",
                infoX, infoY, 0xFFFF00, false);
        infoY += lineH;

        // Hovered widget info
        String hoveredInfo = getHoveredWidgetInfo(screen, mouseX, mouseY);
        guiGraphics.drawString(font, hoveredInfo, infoX, infoY, 0x55FF55, false);
        infoY += lineH;

        // Debug logging status
        if (DebugLogger.isEnabled()) {
            guiGraphics.drawString(font, "LOG: " + DebugLogger.getSessionSummary(), infoX, infoY, 0xFF8844, false);
        } else {
            guiGraphics.drawString(font, "LOG: Off (F3+L to enable)", infoX, infoY, 0x666666, false);
        }

        // ── Mouse crosshair ──
        guiGraphics.fill(mouseX - 8, mouseY, mouseX + 9, mouseY + 1, 0x80FFFF00);
        guiGraphics.fill(mouseX, mouseY - 8, mouseX + 1, mouseY + 9, 0x80FFFF00);
    }

    private static void renderWidgetDebug(GuiGraphics guiGraphics, AbstractWidget widget,
                                           int mouseX, int mouseY,
                                           net.minecraft.client.gui.Font font) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();

        if (!widget.visible) {
            // Invisible widgets: faint red dashed outline
            drawOutline(guiGraphics, x, y, w, h, 0x40FF0000);
            return;
        }

        // Active = green, inactive = red
        int color = widget.active ? 0xAA00FF00 : 0xAAFF4444;

        // Hovered = brighter
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (hovered) {
            color = widget.active ? 0xFF00FF00 : 0xFFFF4444;
            // Fill with faint highlight
            guiGraphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
        }

        drawOutline(guiGraphics, x, y, w, h, color);

        // Size label on hovered widget
        if (hovered) {
            String sizeStr = w + "x" + h;
            int labelX = x + w + 2;
            int labelY = y;
            guiGraphics.fill(labelX - 1, labelY - 1, labelX + font.width(sizeStr) + 2, labelY + 9, 0xCC000000);
            guiGraphics.drawString(font, sizeStr, labelX, labelY, color, false);
        }
    }

    private static void renderSlotDebug(GuiGraphics guiGraphics, AbstractContainerScreen<?> acs,
                                         int mouseX, int mouseY,
                                         net.minecraft.client.gui.Font font) {
        int guiLeft = acs.getGuiLeft();
        int guiTop = acs.getGuiTop();

        for (Slot slot : acs.getMenu().slots) {
            int sx = guiLeft + slot.x;
            int sy = guiTop + slot.y;

            // Slot outline (yellow for active, gray for inactive)
            boolean active = slot.isActive();
            int color = active ? 0xAAFFFF00 : 0x40888888;

            drawOutline(guiGraphics, sx, sy, 16, 16, color);

            // Slot index label (tiny, at top-left of slot)
            String idxStr = String.valueOf(slot.index);
            guiGraphics.drawString(font, idxStr, sx + 1, sy + 1, 0xAAAAAA, false);
        }
    }

    private static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);           // top
        g.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        g.fill(x, y, x + 1, y + h, color);            // left
        g.fill(x + w - 1, y, x + w, y + h, color);   // right
    }

    @SuppressWarnings("unchecked")
    private static List<? extends Renderable> getRenderables(Screen screen) {
        try {
            // Screen.renderables is a protected field — access via reflection
            var field = Screen.class.getDeclaredField("renderables");
            field.setAccessible(true);
            return (List<? extends Renderable>) field.get(screen);
        } catch (NoSuchFieldException e) {
            // Obfuscated name fallback — try common SRG names
            try {
                for (var f : Screen.class.getDeclaredFields()) {
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object value = f.get(screen);
                        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Renderable) {
                            return (List<? extends Renderable>) list;
                        }
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return null;
    }

    private static String getHoveredWidgetInfo(Screen screen, int mouseX, int mouseY) {
        List<? extends Renderable> renderables = getRenderables(screen);
        if (renderables == null) return "Hover: (none)";

        for (Renderable r : renderables) {
            if (r instanceof AbstractWidget widget && widget.visible) {
                int x = widget.getX();
                int y = widget.getY();
                if (mouseX >= x && mouseX < x + widget.getWidth() &&
                    mouseY >= y && mouseY < y + widget.getHeight()) {
                    String className = widget.getClass().getSimpleName();
                    String msg = widget.getMessage().getString();
                    if (msg.length() > 20) msg = msg.substring(0, 17) + "...";
                    return "Hover: " + className + " \"" + msg + "\" @(" + x + "," + y + ")";
                }
            }
        }
        return "Hover: (none)";
    }
}
