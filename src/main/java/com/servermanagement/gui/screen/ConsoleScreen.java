package com.servermanagement.gui.screen;

import com.servermanagement.gui.ConsoleMenu;
import com.servermanagement.gui.widgets.ConsoleOutput;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.ConsoleCommandPacket;
import com.servermanagement.network.packet.OpenGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * In-game console GUI for executing server commands and viewing logs
 */
public class ConsoleScreen extends AbstractContainerScreen<ConsoleMenu> {
    
    private ConsoleOutput consoleOutput;
    private EditBox commandInput;
    
    public ConsoleScreen(ConsoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 300;
        this.imageWidth = 500;
    }
    
    /**
     * Called by ConsoleResponsePacket to add server output to the console
     */
    public void addConsoleLine(String line) {
        if (this.consoleOutput != null) {
            this.consoleOutput.addLine(line);
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Console output area
        this.consoleOutput = new ConsoleOutput(
            centerX + 10, centerY + 40,
            this.imageWidth - 20, 200
        );
        this.addRenderableWidget(this.consoleOutput);
        
        // Add some initial lines (these will be populated by server in real implementation)
        this.consoleOutput.addLine("[INFO] Server console ready");
        this.consoleOutput.addLine("[INFO] Type 'help' for available commands");
        
        // Back to Dashboard button (in header area)
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("\u2190"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(centerX + 5, centerY + 5, 20, 18)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        // Command input row at bottom
        int inputY = centerY + 250;
        this.commandInput = new EditBox(this.font, centerX + 10, inputY, this.imageWidth - 130, 20,
            Component.literal("Command"));
        this.commandInput.setMaxLength(256);
        this.commandInput.setHint(Component.literal("Enter command..."));
        this.addRenderableWidget(this.commandInput);
        
        // Send button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Send"),
            button -> {
                String command = this.commandInput.getValue().trim();
                if (!command.isEmpty()) {
                    // Send command to server
                    ModNetworking.sendToServer(new ConsoleCommandPacket(command));
                    // Echo in console
                    this.consoleOutput.addLine("> " + command);
                    this.commandInput.setValue("");
                }
            })
            .bounds(centerX + this.imageWidth - 115, inputY, 60, 20)
            .style(ModernButton.ButtonStyle.SUCCESS)
            .build());
        
        // Clear button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Clear"),
            button -> this.consoleOutput.clear())
            .bounds(centerX + this.imageWidth - 50, inputY, 50, 20)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        // Set focus to command input
        this.commandInput.setFocused(true);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + this.imageHeight, 0xE0101010);
        
        // Header bar
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + 30, 0xFF1A1A2E);
        guiGraphics.fill(this.leftPos, this.topPos + 30, this.leftPos + this.imageWidth, 
                        this.topPos + 31, 0xFF333333);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        guiGraphics.drawString(this.font, "Server Console", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Info text
        guiGraphics.drawString(this.font, "Execute commands and view server logs", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When command input is focused, handle text input specially
        if (this.commandInput.isFocused()) {
            // Enter key sends command
            if (keyCode == 257) { // 257 = ENTER
                String command = this.commandInput.getValue().trim();
                if (!command.isEmpty()) {
                    ModNetworking.sendToServer(new ConsoleCommandPacket(command));
                    this.consoleOutput.addLine("> " + command);
                    this.commandInput.setValue("");
                }
                return true;
            }
            // Let the EditBox handle all other keys (prevents keybind activation)
            return this.commandInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // When command input is focused, all typed characters go to the input box
        if (this.commandInput.isFocused()) {
            return this.commandInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels (prevents "Inventory" text)
    }
}
