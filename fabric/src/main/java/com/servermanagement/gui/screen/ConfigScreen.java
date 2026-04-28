package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.ConfigMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.ToggleFeaturePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern Config Screen - Feature Toggles (shows ALL features regardless of state)
 */
public class ConfigScreen extends ScalableContainerScreen<ConfigMenu> {
    
    private ToggleSwitch worldManagerSwitch;
    private ToggleSwitch playerManagerSwitch;
    private ToggleSwitch economySwitch;
    private ToggleSwitch slimeHeadSwitch;
    private ToggleSwitch serverPerformanceSwitch;
    private ToggleSwitch motdSwitch;
    
    public ConfigScreen(ConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 400, 380);
        this.imageWidth = 400;
        this.imageHeight = 380;
    }

    @Override
    protected void init() {
        super.init();
        // Refresh feature toggle states from FeatureManager so that init() re-runs
        // triggered by SyncFeatureStatesPacket reflect the latest server state.
        this.menu.refreshStates();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int rightCol = centerX + this.imageWidth - 70;
        int startY = centerY + 45;
        int spacing = (this.imageHeight - 45 - 45) / 6; // evenly distribute 6 rows
        
        // WorldManager toggle
        this.worldManagerSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 0 + 8,
            Component.literal("World Manager"),
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
            rightCol, startY + spacing * 1 + 8,
            Component.literal("Player Manager"),
            this.menu.isPlayerManagerEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("player_manager", newState, clientTick));
                this.menu.setPlayerManagerEnabled(newState);
            }
        );
        this.addRenderableWidget(this.playerManagerSwitch);
        
        // Economy toggle
        this.economySwitch = new ToggleSwitch(
            rightCol, startY + spacing * 2 + 8,
            Component.literal("Economy System"),
            this.menu.isEconomyEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("economy", newState, clientTick));
                this.menu.setEconomyEnabled(newState);
            }
        );
        this.addRenderableWidget(this.economySwitch);
        
        // SlimeHead toggle
        this.slimeHeadSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 3 + 8,
            Component.literal("SlimeHead Feature"),
            this.menu.isSlimeHeadEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("slimehead", newState, clientTick));
                this.menu.setSlimeHeadEnabled(newState);
            }
        );
        this.addRenderableWidget(this.slimeHeadSwitch);
        
        // Server Performance toggle
        this.serverPerformanceSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 4 + 8,
            Component.literal("Server Performance"),
            this.menu.isServerPerformanceEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("server_performance", newState, clientTick));
                this.menu.setServerPerformanceEnabled(newState);
            }
        );
        this.addRenderableWidget(this.serverPerformanceSwitch);
        
        // MOTD Editor toggle
        this.motdSwitch = new ToggleSwitch(
            rightCol, startY + spacing * 5 + 8,
            Component.literal("MOTD Editor"),
            this.menu.isMotdEnabled(),
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new ToggleFeaturePacket("motd_editor", newState, clientTick));
                this.menu.setMotdEnabled(newState);
            }
        );
        this.addRenderableWidget(this.motdSwitch);
        
        // Bottom buttons - symmetrical
        int btnW = (this.imageWidth - 30) / 2;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("ÔåÉ Dashboard"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(centerX + 10, centerY + this.imageHeight - 35, btnW, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + this.imageWidth - btnW - 10, centerY + this.imageHeight - 35, btnW, 24)
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
        
        // Alternating row backgrounds for visual structure
        int startY = this.topPos + 45;
        int spacing = (this.imageHeight - 45 - 45) / 6;
        for (int i = 0; i < 6; i++) {
            int rowY = startY + spacing * i;
            if (i % 2 == 0) {
                guiGraphics.fill(this.leftPos + 5, rowY, 
                    this.leftPos + this.imageWidth - 5, rowY + spacing - 2, 0x18FFFFFF);
            }
            // Subtle divider line between rows
            if (i > 0) {
                guiGraphics.fill(this.leftPos + 15, rowY - 1, 
                    this.leftPos + this.imageWidth - 15, rowY, 0x20FFFFFF);
            }
        }
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        guiGraphics.drawString(this.font, "Mod Configuration", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        guiGraphics.drawString(this.font, "Enable or disable features", 
            this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        
        // Feature labels with descriptions
        int startY = this.topPos + 45;
        int spacing = (this.imageHeight - 45 - 45) / 6;
        
        String[][] features = {
            {"World Manager", "Manage dimensions, portals & timers"},
            {"Player Manager", "Ban, whitelist & manage players"},
            {"Economy System", "Currency, trading & marketplace"},
            {"SlimeHead Feature", "Custom slime head drops"},
            {"Server Performance", "TPS optimization & monitoring"},
            {"MOTD Editor", "Customize server message of the day"}
        };
        
        for (int i = 0; i < features.length; i++) {
            int rowY = startY + spacing * i;
            guiGraphics.drawString(this.font, features[i][0], 
                this.leftPos + 20, rowY + 5, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, features[i][1], 
                this.leftPos + 20, rowY + 17, 0x777777, true);
        }
        
        // Render widgets on top
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
