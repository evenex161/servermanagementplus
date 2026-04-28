package com.servermanagement.gui.debug;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import com.servermanagement.ServerManagementMod;

/**
 * Handles the F3+M keybind to toggle GUI debug overlay, and injects the
 * overlay rendering into all screens that belong to this mod.
 */
public final class GuiDebugEventHandler {

    private static boolean f3Held = false;

    private GuiDebugEventHandler() {}

    public static void onKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        // Track F3 state
        if (keyCode == GLFW.GLFW_KEY_F3) {
            f3Held = true;
        }

        // F3+M toggles debug overlay
        if (f3Held && keyCode == GLFW.GLFW_KEY_M) {
            GuiDebugOverlay.toggle();
        }
    }

    public static void onKeyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F3) {
            f3Held = false;
        }
    }

    /**
     * Inject debug overlay rendering after each screen renders.
     * Only renders on screens from this mod's package.
     */
    public static void onScreenRenderPost(Screen screen, net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        if (!GuiDebugOverlay.isEnabled()) return;

        // Only render on our mod's screens
        if (screen.getClass().getPackageName().startsWith("com.servermanagement.gui")) {
            GuiDebugOverlay.render(guiGraphics, screen, mouseX, mouseY);
        }
    }
}
