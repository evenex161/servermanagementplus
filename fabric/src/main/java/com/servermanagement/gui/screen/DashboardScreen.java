package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.DashboardMenu;
import com.servermanagement.gui.widgets.DashboardCard;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Main ServerManagement Dashboard - Central hub for all features
 */
public class DashboardScreen extends ScalableContainerScreen<DashboardMenu> {
    
    public DashboardScreen(DashboardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 400, 330);
        this.imageHeight = 330; // Increased for 3 rows
        this.imageWidth = 400;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int cardWidth = (this.imageWidth - 40) / 3;
        int spacing = 10;
        // Reserve space for Close button inside the panel (30px header + cards + close)
        int availCardHeight = this.imageHeight - 40 - 15 - 30; // header, padding, close button area
        int cardHeight = (availCardHeight - spacing * 2) / 3;
        
        int row1Y = centerY + 40;
        int row2Y = row1Y + cardHeight + spacing;
        int row3Y = row2Y + cardHeight + spacing;
        
        // Row 1: Main features
        this.addRenderableWidget(new DashboardCard(
            centerX + 10, row1Y, cardWidth, cardHeight,
            Component.literal("World Manager"),
            "*", "Portals & Timers",
            DashboardCard.CardStyle.BLUE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_LIST, ""))
        ));
        
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth + 20, row1Y, cardWidth, cardHeight,
            Component.literal("Player Manager"),
            ">", "Spectate & Inventory",
            DashboardCard.CardStyle.GREEN,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.PLAYER_MANAGER, ""))
        ));
        
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth * 2 + 30, row1Y, cardWidth, cardHeight,
            Component.literal("Console"),
            "#", "Server Commands",
            DashboardCard.CardStyle.PURPLE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.CONSOLE, ""))
        ));
        
        // Row 2
        this.addRenderableWidget(new DashboardCard(
            centerX + 10, row2Y, cardWidth, cardHeight,
            Component.literal("Global Settings"),
            "@", "Chat & Tab",
            DashboardCard.CardStyle.ORANGE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.GLOBAL_SETTINGS, ""))
        ));
        
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth + 20, row2Y, cardWidth, cardHeight,
            Component.literal("Economy"),
            "$", "Tasks & Rewards",
            DashboardCard.CardStyle.GREEN,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.ECONOMY_MANAGEMENT, ""))
        ));
        
        this.addRenderableWidget(new DashboardCard(
            centerX + cardWidth * 2 + 30, row2Y, cardWidth, cardHeight,
            Component.literal("Performance"),
            "~", "TPS & Optimization",
            DashboardCard.CardStyle.RED,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.PERFORMANCE_SETTINGS, ""))
        ));
        
        // Row 3: 2 cards centered + close button to the right
        int halfGap = spacing / 2;
        int twoCardWidth = cardWidth * 2 + spacing;
        int row3StartX = centerX + (this.imageWidth - twoCardWidth - cardWidth - spacing) / 2;
        
        this.addRenderableWidget(new DashboardCard(
            row3StartX, row3Y, cardWidth, cardHeight,
            Component.literal("MOTD Editor"),
            "=", "Server Message",
            DashboardCard.CardStyle.BLUE,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.MOTD_EDITOR, ""))
        ));
        
        this.addRenderableWidget(new DashboardCard(
            row3StartX + cardWidth + spacing, row3Y, cardWidth, cardHeight,
            Component.literal("Mod Settings"),
            "+", "Features Config",
            DashboardCard.CardStyle.GRAY,
            () -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.CONFIG, ""))
        ));
        
        // Close button - inside panel, at bottom center
        int closeY = row3Y + cardHeight + 8;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + (this.imageWidth - 120) / 2, closeY, 120, 24)
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
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title - render BEFORE super.render to prevent overlap
        guiGraphics.drawString(this.font, "ServerManagement Dashboard", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Select a feature to manage", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Render widgets on top
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
