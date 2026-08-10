package com.servermanagement.gui.economy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.client.ClientConfig;
import com.servermanagement.gui.widgets.ToggleSwitch;

public class ConfirmDeleteScreen extends Screen {

    private final Screen parent;
    private final Runnable onConfirm;
    private final int itemCount;
    private ToggleSwitch dontAskAgainSwitch;

    public ConfirmDeleteScreen(Screen parent, int itemCount, Runnable onConfirm) {
        super(Component.literal("Confirm Deletion"));
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.itemCount = itemCount;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(new ModernButton(
            centerX - 105, centerY + 30, 100, 20,
            Component.literal("Cancel"),
            btn -> this.minecraft.setScreen(this.parent),
            ModernButton.ButtonStyle.SECONDARY
        ));

        this.addRenderableWidget(new ModernButton(
            centerX + 5, centerY + 30, 100, 20,
            Component.literal("Delete"),
            btn -> {
                if (dontAskAgainSwitch.isToggled()) {
                    ClientConfig.setSkipBlacklistWarning(true);
                    
                }
                this.onConfirm.run();
                this.minecraft.setScreen(this.parent);
            },
            ModernButton.ButtonStyle.DANGER
        ));

        dontAskAgainSwitch = new ToggleSwitch(
            centerX - 95, centerY + 65,
            Component.literal("Don't ask me again"),
            ClientConfig.shouldSkipBlacklistWarning(),
            (state) -> {}
        );
        this.addRenderableWidget(dontAskAgainSwitch);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 40, 0xFF0000);
        guiGraphics.drawCenteredString(this.font, Component.literal("Are you sure you want to delete " + itemCount + " item(s)?"), centerX, centerY - 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Don't ask me again", centerX - 50, centerY + 70, 0xAAAAAA, true);
    }
}
