package com.servermanagement.gui.screen;

import com.servermanagement.gui.GlobalSettingsMenu;
import com.servermanagement.gui.ScreenScaler;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.WMToggleChatIsolationPacket;
import com.servermanagement.network.packet.WMToggleTabIsolationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern Global Settings Screen - Chat and Tab Isolation
 */
public class GlobalSettingsScreen extends AbstractContainerScreen<GlobalSettingsMenu> {
    
    private ToggleSwitch chatIsolationSwitch;
    private ToggleSwitch tabIsolationSwitch;

    public GlobalSettingsScreen(GlobalSettingsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        int[] dim = ScreenScaler.scale(320, 200, this.width, this.height);
        this.imageWidth = dim[0];
        this.imageHeight = dim[1];
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int rightCol = centerX + this.imageWidth - 100;
        int startY = centerY + 60;
        int spacing = 35;
        
        // Chat Isolation toggle
        this.chatIsolationSwitch = new ToggleSwitch(
            rightCol, startY + 3,
            Component.literal("Chat Isolation"),
            this.menu.isChatIsolationEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMToggleChatIsolationPacket(newState, clientTick));
                this.menu.setChatIsolationEnabled(newState);
            }
        );
        this.addRenderableWidget(this.chatIsolationSwitch);
        
        // Tab Isolation toggle
        this.tabIsolationSwitch = new ToggleSwitch(
            rightCol, startY + spacing + 3,
            Component.literal("Tab Isolation"),
            this.menu.isTabIsolationEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMToggleTabIsolationPacket(newState, clientTick));
                this.menu.setTabIsolationEnabled(newState);
            }
        );
        this.addRenderableWidget(this.tabIsolationSwitch);
        
        // Back to Dashboard button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Dashboard"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(centerX + 10, centerY + this.imageHeight - 35, 120, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + this.imageWidth - 90, centerY + this.imageHeight - 35, 80, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        guiGraphics.drawString(this.font, "Global Settings", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Cross-dimension settings", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Labels
        guiGraphics.drawString(this.font, "Chat Isolation:", 
            this.leftPos + 20, this.topPos + 63, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Tab Isolation:", 
            this.leftPos + 20, this.topPos + 98, 0xFFFFFF, true);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
