package com.servermanagement.gui.screen;

import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.gui.WorldListMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.RequestWorldListPacket;
import com.servermanagement.network.packet.SyncWorldListPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern World List Screen
 */
public class WorldListScreen extends AbstractContainerScreen<WorldListMenu> {
    
    private List<ModernButton> worldButtons = new ArrayList<>();
    private int scrollOffset = 0;

    public WorldListScreen(WorldListMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        
        // Request world list from server
        ModNetworking.sendToServer(new RequestWorldListPacket());
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Back to Dashboard button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Dashboard"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(centerX + 10, centerY + this.imageHeight - 35, 100, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build()
        );
        
        // Refresh button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Refresh"),
            button -> {
                ModNetworking.sendToServer(new RequestWorldListPacket());
                updateWorldButtons();
            })
            .bounds(centerX + 120, centerY + this.imageHeight - 35, 80, 24)
            .style(ModernButton.ButtonStyle.PRIMARY)
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
        
        // Initialize world buttons
        updateWorldButtons();
    }

    private void updateWorldButtons() {
        // Clear old buttons
        worldButtons.forEach(this::removeWidget);
        worldButtons.clear();
        
        // Get world list from cache
        List<SyncWorldListPacket.WorldInfo> worlds = ClientPacketHandler.getCachedWorldList();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        int yPos = centerY + 45;
        
        for (int i = scrollOffset; i < Math.min(worlds.size(), scrollOffset + 6); i++) {
            SyncWorldListPacket.WorldInfo world = worlds.get(i);
            
            String buttonText = world.name + " (" + world.playerCount + ")";
            
            ModernButton worldButton = new ModernButton.Builder(
                Component.literal(buttonText),
                button -> {
                    // Open world detail screen for this dimension
                    ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_DETAIL, world.dimensionId));
                })
                .bounds(centerX + 10, yPos, 300, 24)
                .style(world.areAllPortalsEnabled() ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.DANGER)
                .build();
            
            worldButtons.add(worldButton);
            this.addRenderableWidget(worldButton);
            
            yPos += 28;
        }
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
        guiGraphics.drawString(this.font, "World Manager", 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        // Subtitle
        List<SyncWorldListPacket.WorldInfo> worlds = ClientPacketHandler.getCachedWorldList();
        guiGraphics.drawString(this.font, worlds.size() + " dimension(s)", 
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
