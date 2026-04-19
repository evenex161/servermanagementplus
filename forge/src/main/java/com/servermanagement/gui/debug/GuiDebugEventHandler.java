package com.servermanagement.gui.debug;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import com.servermanagement.ServerManagementMod;

/**
 * Handles the F3+M keybind to toggle GUI debug overlay, and injects the
 * overlay rendering into all screens that belong to this mod.
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, value = Dist.CLIENT)
public final class GuiDebugEventHandler {

    private static boolean f3Held = false;

    private GuiDebugEventHandler() {}

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        int keyCode = event.getKeyCode();

        // Track F3 state
        if (keyCode == GLFW.GLFW_KEY_F3) {
            f3Held = true;
        }

        // F3+M toggles debug overlay
        if (f3Held && keyCode == GLFW.GLFW_KEY_M) {
            GuiDebugOverlay.toggle();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (event.getKeyCode() == GLFW.GLFW_KEY_F3) {
            f3Held = false;
        }
    }

    /**
     * Inject debug overlay rendering after each screen renders.
     * Only renders on screens from this mod's package.
     */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!GuiDebugOverlay.isEnabled()) return;

        Screen screen = event.getScreen();

        // Only render on our mod's screens
        if (screen.getClass().getPackageName().startsWith("com.servermanagement.gui")) {
            GuiDebugOverlay.render(event.getGuiGraphics(), screen,
                    event.getMouseX(), event.getMouseY());
        }
    }
}
