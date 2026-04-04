package com.servermanagement.gui.screen;

import com.servermanagement.gui.menu.ServerManagementMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.ToggleFeaturePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern, minimal ServerManagement GUI
 */
public class ServerManagementScreen extends AbstractContainerScreen<ServerManagementMenu> {
    
    private ToggleSwitch worldManagerSwitch;
    private ToggleSwitch playerManagerSwitch;
    private ToggleSwitch slimeHeadSwitch;
    
    public ServerManagementScreen(ServerManagementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 180;
        this.imageWidth = 300;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Refresh menu states from FeatureManager before displaying
        this.menu.refreshStates();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int leftCol = centerX + 20;
        int rightCol = centerX + 240;
        int startY = centerY + 40;
        int spacing = 35;
        
        // WorldManager toggle
        this.worldManagerSwitch = new ToggleSwitch(
            rightCol, startY + 3,
            Component.literal("WorldManager"),
            this.menu.isWorldManagerEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("world_manager", newState, clientTick));
                this.menu.setWorldManagerEnabled(newState);
            }
        );
        this.addRenderableWidget(this.worldManagerSwitch);
        
        // PlayerManager toggle
        this.playerManagerSwitch = new ToggleSwitch(
            rightCol, startY + spacing + 3,
            Component.literal("PlayerManager"),
            this.menu.isPlayerManagerEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("player_manager", newState, clientTick));
                this.menu.setPlayerManagerEnabled(newState);
            }
        );
        this.addRenderableWidget(this.playerManagerSwitch);
        
        // SlimeHead toggle
        this.slimeHeadSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 2 + 3,
            Component.literal("SlimeHead"),
            this.menu.isSlimeHeadEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("slimehead", newState, clientTick));
                this.menu.setSlimeHeadEnabled(newState);
            }
        );
        this.addRenderableWidget(this.slimeHeadSwitch);
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + 100, startY + spacing * 3 + 10, 100, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Dark background with subtle gradient
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + this.imageHeight, 0xE0101010);
        
        // Header bar
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + 25, 0xE0202020);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title in header
        guiGraphics.drawString(this.font, "Server Management", 
            this.leftPos + 15, this.topPos + 8, 0xFFFFFF, false);
        
        int leftCol = this.leftPos + 20;
        int rightCol = this.leftPos + 240;
        int startY = this.topPos + 40;
        int spacing = 35;
        
        // Feature labels and descriptions
        guiGraphics.drawString(this.font, "World Manager", 
            leftCol, startY + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Portal & timer control", 
            leftCol, startY + 18, 0x808080, false);
        
        guiGraphics.drawString(this.font, "Player Manager", 
            leftCol, startY + spacing + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Spectate & inventory", 
            leftCol, startY + spacing + 18, 0x808080, false);
        
        guiGraphics.drawString(this.font, "Slime Heads", 
            leftCol, startY + spacing * 2 + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Decorative heads", 
            leftCol, startY + spacing * 2 + 18, 0x808080, false);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
