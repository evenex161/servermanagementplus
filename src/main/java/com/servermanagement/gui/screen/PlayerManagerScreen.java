package com.servermanagement.gui.screen;

import com.servermanagement.gui.PlayerManagerMenu;
import com.servermanagement.gui.ScreenScaler;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.PMSpectatePlayerPacket;
import com.servermanagement.network.packet.PMViewInventoryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Modern Player Manager Screen
 */
public class PlayerManagerScreen extends AbstractContainerScreen<PlayerManagerMenu> {
    
    private List<ModernButton> playerButtons = new ArrayList<>();
    private String selectedPlayer = null;
    private boolean showingActionMenu = false;

    public PlayerManagerScreen(PlayerManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        int[] dim = ScreenScaler.scale(320, 240, this.width, this.height);
        this.imageWidth = dim[0];
        this.imageHeight = dim[1];
        super.init();
        
        if (!showingActionMenu) {
            updatePlayerButtons();
        } else {
            showPlayerActionMenu();
        }
    }

    private void updatePlayerButtons() {
        // Clear old buttons
        playerButtons.forEach(this::removeWidget);
        playerButtons.clear();
        
        // Get online players
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;
        
        Collection<PlayerInfo> players = connection.getOnlinePlayers();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        int yPos = centerY + 45;
        
        int count = 0;
        for (PlayerInfo playerInfo : players) {
            if (count >= 6) break;
            
            String playerName = playerInfo.getProfile().getName();
            
            ModernButton playerButton = new ModernButton.Builder(
                Component.literal(playerName),
                button -> {
                    // Open player actions submenu
                    selectedPlayer = playerName;
                    showingActionMenu = true;
                    this.rebuildWidgets();
                })
                .bounds(centerX + 10, yPos, this.imageWidth - 20, 24)
                .style(ModernButton.ButtonStyle.PRIMARY)
                .build();
            
            playerButtons.add(playerButton);
            this.addRenderableWidget(playerButton);
            
            yPos += 28;
            count++;
        }
        
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
    
    private void showPlayerActionMenu() {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        int startY = centerY + 70;
        
        // Spectate button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Spectate Player"),
            button -> {
                ModNetworking.sendToServer(new PMSpectatePlayerPacket(selectedPlayer));
                this.onClose();
            })
            .bounds(centerX + 10, startY, this.imageWidth - 20, 24)
            .style(ModernButton.ButtonStyle.SUCCESS)
            .build());
        
        // View Inventory button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("View Inventory"),
            button -> {
                ModNetworking.sendToServer(new PMViewInventoryPacket(selectedPlayer));
                this.onClose();
            })
            .bounds(centerX + 10, startY + 30, this.imageWidth - 20, 24)
            .style(ModernButton.ButtonStyle.PRIMARY)
            .build());
        
        // Back button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Back"),
            button -> {
                showingActionMenu = false;
                selectedPlayer = null;
                this.rebuildWidgets();
            })
            .bounds(centerX + 10, centerY + this.imageHeight - 35, 100, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + this.imageWidth - 90, centerY + this.imageHeight - 35, 80, 24)
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
        
        // Draw title/header
        if (showingActionMenu && selectedPlayer != null) {
            String title = "Actions for " + selectedPlayer;
            guiGraphics.drawString(this.font, title, 
                this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
            
            guiGraphics.drawString(this.font, "Manage " + selectedPlayer, 
                this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
        } else {
            guiGraphics.drawString(this.font, "Player Manager", 
                this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
            
            // Count online players
            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener connection = mc.getConnection();
            if (connection != null) {
                int playerCount = connection.getOnlinePlayers().size();
                guiGraphics.drawString(this.font, playerCount + " player(s) online", 
                    this.leftPos + 15, this.topPos + 20, 0xAAAAAA, true);
            }
        }
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
