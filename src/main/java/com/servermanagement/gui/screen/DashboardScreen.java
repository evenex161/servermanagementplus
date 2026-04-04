package com.servermanagement.gui.screen;

import com.servermanagement.gui.DashboardMenu;
import com.servermanagement.gui.widgets.DashboardCard;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Main ServerManagement Dashboard - Central hub for all features
 */
public class DashboardScreen extends AbstractContainerScreen<DashboardMenu> {
    
    public DashboardScreen(DashboardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 330; // Increased for 3 rows
        this.imageWidth = 400;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int cardWidth = 120;
        int cardHeight = 65;
        int spacing = 10;
        
        int row1Y = centerY + 40;
        int row2Y = row1Y + cardHeight + spacing;
        int row3Y = row2Y + cardHeight + spacing;
        
        // Row 1: Main features
        // World Manager
        this.addRenderableWidget(new DashboardCard(
            centerX + 10, row1Y, cardWidth, cardHeight,
            Component.literal("World Manager"),
            "*", "Portals & Timers",
            DashboardCard.CardStyle.BLUE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_LIST, ""))
        ));
        
        // Player Manager
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth + 20, row1Y, cardWidth, cardHeight,
            Component.literal("Player Manager"),
            ">", "Spectate & Inventory",
            DashboardCard.CardStyle.GREEN,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.PLAYER_MANAGER, ""))
        ));
        
        // Console
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth * 2 + 30, row1Y, cardWidth, cardHeight,
            Component.literal("Console"),
            "#", "Server Commands",
            DashboardCard.CardStyle.PURPLE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.CONSOLE, ""))
        ));
        
        // Row 2: Settings and utilities
        // Global Settings
        this.addRenderableWidget(new DashboardCard(
            centerX + 10, row2Y, cardWidth, cardHeight,
            Component.literal("Global Settings"),
            "@", "Chat & Tab",
            DashboardCard.CardStyle.ORANGE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.GLOBAL_SETTINGS, ""))
        ));
        
        // Economy Management
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth + 20, row2Y, cardWidth, cardHeight,
            Component.literal("Economy"),
            "$", "Tasks & Rewards",
            DashboardCard.CardStyle.GREEN,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.ECONOMY_MANAGEMENT, ""))
        ));
        
        // Row 3: Mod Settings
        // Mod Settings
        this.addRenderableWidget(new DashboardCard(
            centerX + 10, row3Y, cardWidth, cardHeight,
            Component.literal("Mod Settings"),
            "+", "Features Config",
            DashboardCard.CardStyle.GRAY,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.CONFIG, ""))
        ));
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + 150, row3Y + cardHeight + 15, 100, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
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
        
        // Title - render BEFORE super.render to prevent overlap
        guiGraphics.drawString(this.font, "ServerManagement Dashboard", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Select a feature to manage", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
