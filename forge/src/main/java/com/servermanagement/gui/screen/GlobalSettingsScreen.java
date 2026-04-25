package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.GlobalSettingsMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.WMToggleChatIsolationPacket;
import com.servermanagement.network.packet.WMToggleTabIsolationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern Global Settings Screen - Chat and Tab Isolation
 */
public class GlobalSettingsScreen extends ScalableContainerScreen<GlobalSettingsMenu> {
    
    private ToggleSwitch chatIsolationSwitch;
    private ToggleSwitch tabIsolationSwitch;

    public GlobalSettingsScreen(GlobalSettingsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 360, 240);
        this.imageWidth = 320;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int rightCol = centerX + this.imageWidth - 80;
        int leftCol = centerX + 20;
        int startY = centerY + 80;
        int spacing = 50;
        
        // Chat Isolation toggle
        this.chatIsolationSwitch = new ToggleSwitch(
            rightCol, startY + 5,
            Component.literal("Chat Isolation"),
            this.menu.isChatIsolationEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMToggleChatIsolationPacket("", newState, clientTick));
                this.menu.setChatIsolationEnabled(newState);
            }
        );
        this.addRenderableWidget(this.chatIsolationSwitch);
        
        // Tab Isolation toggle
        this.tabIsolationSwitch = new ToggleSwitch(
            rightCol, startY + spacing + 5,
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
        int btnW = (this.imageWidth - 30) / 2;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Dashboard"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(centerX + 10, centerY + this.imageHeight - 38, btnW, 26)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + this.imageWidth - btnW - 10, centerY + this.imageHeight - 38, btnW, 26)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        int x0 = this.leftPos;
        int y0 = this.topPos;
        
        // Title
        guiGraphics.drawString(this.font, "Global Settings", 
            x0 + 15, y0 + 10, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Cross-dimension isolation settings", 
            x0 + 15, y0 + 24, 0xAAAAAA, true);
        
        // Section header
        guiGraphics.drawString(this.font, "Feature Toggles", 
            x0 + 20, y0 + 55, 0xFFFFFF, true);
        guiGraphics.fill(x0 + 20, y0 + 66, x0 + this.imageWidth - 20, y0 + 67, 0x40FFFFFF);
        
        int startY = y0 + 80;
        int spacing = 50;
        
        // Chat Isolation row
        guiGraphics.fill(x0 + 15, startY - 3, x0 + this.imageWidth - 15, startY + 32, 0x18FFFFFF);
        guiGraphics.drawString(this.font, "Chat Isolation", 
            x0 + 25, startY + 2, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Restrict chat to players in the same dimension.", 
            x0 + 25, startY + 15, 0x888888, false);
        
        // Tab Isolation row
        int tabY = startY + spacing;
        guiGraphics.fill(x0 + 15, tabY - 3, x0 + this.imageWidth - 15, tabY + 32, 0x10FFFFFF);
        guiGraphics.drawString(this.font, "Tab Isolation", 
            x0 + 25, tabY + 2, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Only show players from the same dimension in Tab.", 
            x0 + 25, tabY + 15, 0x888888, false);
        
        // Info note at bottom
        guiGraphics.drawString(this.font, "Per-world overrides can be set in World Manager.", 
            x0 + 20, y0 + this.imageHeight - 55, 0x666666, false);
        
        // Render widgets on top
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        
        // Border
        guiGraphics.fill(x0 - 1, y0 - 1, x0 + this.imageWidth + 1, y0 + this.imageHeight + 1, 0xFF000000);
        // Main dark background
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + this.imageHeight, 0xE0101010);
        
        // Header bar with gradient effect
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + 40, 0xFF1A1A2E);
        guiGraphics.fill(x0, y0 + 39, x0 + this.imageWidth, y0 + 40, 0xFF333355);
        guiGraphics.fill(x0, y0 + 40, x0 + this.imageWidth, y0 + 41, 0xFF222222);
        
        // Content area subtle border
        guiGraphics.fill(x0 + 10, y0 + 48, x0 + this.imageWidth - 10, y0 + this.imageHeight - 48, 0x0AFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
