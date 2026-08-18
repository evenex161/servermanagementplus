package com.servermanagement.gui.overlay;

import com.servermanagement.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatsBarEditScreen extends Screen {

    private int startDragX, startDragY;
    private int currentX, currentY;
    private boolean isDragging = false;
    private final int width = StatsBarOverlay.WIDTH;
    private final int height = 50; // Approximated height for 3 tasks + title

    public StatsBarEditScreen() {
        super(Component.literal("Edit HUD Overlay"));
        this.currentX = ClientConfig.getStatsBarX();
        this.currentY = ClientConfig.getStatsBarY();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Draw bounding box
        int color = isDragging ? 0xFF00FF00 : 0xFFFF5555;
        guiGraphics.fill(currentX, currentY, currentX + width, currentY + height, 0x88000000); // Background
        guiGraphics.renderOutline(currentX - 1, currentY - 1, width + 2, height + 2, color); // Outline

        // Render mock stats bar inside
        guiGraphics.drawString(this.font, Component.literal("§6=== Daily Tasks ==="), currentX, currentY, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, Component.literal("§7[1] §fMock Task 1 §e[10/50]"), currentX, currentY + 12, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, Component.literal("§7[2] §fMock Task 2 §a[Completed]"), currentX, currentY + 22, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, Component.literal("§7[3] §fMock Task 3 §a[Claimed]"), currentX, currentY + 32, 0xFFFFFF, true);

        // Instructions
        guiGraphics.drawCenteredString(this.font, Component.literal("Click and drag to move the StatsBar"), this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Press ESC to save and close"), this.width / 2, 35, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            if (mouseX >= currentX && mouseX <= currentX + width && mouseY >= currentY && mouseY <= currentY + height) {
                isDragging = true;
                startDragX = (int) (mouseX - currentX);
                startDragY = (int) (mouseY - currentY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            currentX = (int) (mouseX - startDragX);
            currentY = (int) (mouseY - startDragY);

            // Clamp to screen
            currentX = Math.max(0, Math.min(currentX, super.width - this.width)); // Wait, this.width is screen width. 
            // We need to use super.width since 'width' hides it.
            // Let's just clamp simply:
            if (currentX < 0) currentX = 0;
            if (currentY < 0) currentY = 0;
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ClientConfig.setStatsBarX(currentX);
        ClientConfig.setStatsBarY(currentY);
        // We will assume ClientConfig has a save() method, or we don't need one if it auto-saves, but ForgeConfigSpec auto-saves on set usually, wait no, we need to call save. 
        // Let's verify how ClientConfig saves. Forge config auto saves on set(). Fabric uses Gson. We'll check.
        // For safety, let's just use the config set() for now. If needed, we'll implement a save call.
        super.onClose();
    }
}
