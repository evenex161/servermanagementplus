package com.servermanagement.client;

import com.servermanagement.gui.overlay.HudEditScreen;
import com.servermanagement.network.packet.OpenGuiPacket.GuiType;
import net.minecraft.client.Minecraft;

public class ClientGuiOpener {
    public static void openGui(GuiType guiType) {
        if (guiType == GuiType.HUD_EDIT) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new HudEditScreen());
            });
        }
    }
}
