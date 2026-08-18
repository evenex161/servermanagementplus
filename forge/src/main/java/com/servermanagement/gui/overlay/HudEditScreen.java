package com.servermanagement.gui.overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Stub HUD Edit Screen — placeholder for the full HUD position editor.
 * Opens via /sm hud edit or /bank hud edit commands.
 */
public class HudEditScreen extends Screen {

    public HudEditScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();

        // Close button centered
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose()
        ).bounds(this.width / 2 - 50, this.height / 2 + 40, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Dark overlay background
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // Title
        Component title = Component.literal("HUD Editor");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title,
            (this.width - titleWidth) / 2, this.height / 2 - 30, 0xFFD700, true);

        // Coming soon message
        Component message = Component.literal("HUD position editor — Coming Soon");
        int msgWidth = this.font.width(message);
        guiGraphics.drawString(this.font, message,
            (this.width - msgWidth) / 2, this.height / 2, 0xAAAAAA, false);

        // Instruction
        Component instruction = Component.literal("Use this screen to reposition the stats bar overlay");
        int instrWidth = this.font.width(instruction);
        guiGraphics.drawString(this.font, instruction,
            (this.width - instrWidth) / 2, this.height / 2 + 15, 0x666666, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
