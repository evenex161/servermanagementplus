package com.servermanagement.gui.screen;

import com.servermanagement.gui.ConfigMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.ToggleFeaturePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern Config Screen - Feature Toggles (shows ALL features regardless of state)
 */
public class ConfigScreen extends AbstractContainerScreen<ConfigMenu> {
    
    private ToggleSwitch worldManagerSwitch;
    private ToggleSwitch playerManagerSwitch;
    private ToggleSwitch economySwitch;
    private ToggleSwitch slimeHeadSwitch;
    private ToggleSwitch serverPerformanceSwitch;
    
    public ConfigScreen(ConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 295; // Height for 5 toggles
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int rightCol = centerX + 240;
        int startY = centerY + 50;
        int spacing = 35;
        
        // WorldManager toggle - ALWAYS shown
        this.worldManagerSwitch = new ToggleSwitch(
            rightCol, startY + 3,
            Component.literal("World Manager"),
            this.menu.isWorldManagerEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("world_manager", newState, clientTick));
                this.menu.setWorldManagerEnabled(newState);
            }
        );
        this.addRenderableWidget(this.worldManagerSwitch);
        
        // PlayerManager toggle - ALWAYS shown
        this.playerManagerSwitch = new ToggleSwitch(
            rightCol, startY + spacing + 3,
            Component.literal("Player Manager"),
            this.menu.isPlayerManagerEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("player_manager", newState, clientTick));
                this.menu.setPlayerManagerEnabled(newState);
            }
        );
        this.addRenderableWidget(this.playerManagerSwitch);
        
        // Economy toggle - ALWAYS shown
        this.economySwitch = new ToggleSwitch(
            rightCol, startY + spacing * 2 + 3,
            Component.literal("Economy System"),
            this.menu.isEconomyEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("economy", newState, clientTick));
                this.menu.setEconomyEnabled(newState);
            }
        );
        this.addRenderableWidget(this.economySwitch);
        
        // SlimeHead toggle - ALWAYS shown (even if disabled)
        this.slimeHeadSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 3 + 3,
            Component.literal("SlimeHead Feature"),
            this.menu.isSlimeHeadEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("slimehead", newState, clientTick));
                this.menu.setSlimeHeadEnabled(newState);
            }
        );
        this.addRenderableWidget(this.slimeHeadSwitch);
        
        // Server Performance toggle - ALWAYS shown
        this.serverPerformanceSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 4 + 3,
            Component.literal("Server Performance"),
            this.menu.isServerPerformanceEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("server_performance", newState, clientTick));
                this.menu.setServerPerformanceEnabled(newState);
            }
        );
        this.addRenderableWidget(this.serverPerformanceSwitch);
        
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
        guiGraphics.drawString(this.font, "Mod Configuration", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Enable or disable features", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Labels
        guiGraphics.drawString(this.font, "World Manager:", 
            this.leftPos + 20, this.topPos + 53, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Player Manager:", 
            this.leftPos + 20, this.topPos + 88, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Economy System:", 
            this.leftPos + 20, this.topPos + 123, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "SlimeHead Feature:", 
            this.leftPos + 20, this.topPos + 158, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Server Performance:", 
            this.leftPos + 20, this.topPos + 193, 0xFFFFFF, true);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
