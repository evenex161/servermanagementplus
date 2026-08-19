package com.servermanagement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ClientScreenManager {
    public static void openHudEditScreen() {
        Minecraft.getInstance().setScreen(new com.servermanagement.gui.overlay.HudEditScreen());
    }

    public static void refreshOpenScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Screen screen = mc.screen;
        if (screen == null) {
            return;
        }
        if (!screen.getClass().getName().startsWith("com.servermanagement")) {
            return;
        }
        try {
            screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        } catch (Throwable t) {
            com.servermanagement.gui.debug.DebugLogger.logCacheUpdate("ScreenRefresh",
                    "failed for " + screen.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}
