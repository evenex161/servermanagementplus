package com.servermanagement.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.servermanagement.ServerManagementMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Screen shown during OTA update process with progress bar
 */
public class OTAUpdateScreen extends Screen {
    
    private final String serverVersion;
    private final String clientVersion;
    private final long totalSize;
    private String currentStatus = "Preparing update...";
    private float progress = 0.0f; // 0.0 to 1.0
    private boolean updateComplete = false;
    private boolean updateFailed = false;
    private String errorMessage = "";
    
    public OTAUpdateScreen(String clientVersion, String serverVersion, long totalSize) {
        super(Component.literal("Mod Update Required"));
        this.clientVersion = clientVersion;
        this.serverVersion = serverVersion;
        this.totalSize = totalSize;
    }
    
    @Override
    protected void init() {
        super.init();
        // No buttons - force update
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xFF1a1a1a, 0xFF2d2d2d);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Title
        drawCenteredString(guiGraphics, this.font, "§6§lMOD UPDATE REQUIRED", centerX, centerY - 80, 0xFFFFFF);
        
        // Version info
        drawCenteredString(guiGraphics, this.font, "§7Your version: §c" + clientVersion, centerX, centerY - 60, 0xFFFFFF);
        drawCenteredString(guiGraphics, this.font, "§7Server version: §a" + serverVersion, centerX, centerY - 45, 0xFFFFFF);
        
        if (updateFailed) {
            // Error state
            drawCenteredString(guiGraphics, this.font, "§c§lUPDATE FAILED", centerX, centerY - 20, 0xFFFFFF);
            drawCenteredString(guiGraphics, this.font, "§7" + errorMessage, centerX, centerY, 0xFFFFFF);
            drawCenteredString(guiGraphics, this.font, "§7Please update manually or contact server admin", centerX, centerY + 15, 0xFFFFFF);
        } else if (updateComplete) {
            // Success state
            drawCenteredString(guiGraphics, this.font, "§a§lUPDATE COMPLETE", centerX, centerY - 20, 0xFFFFFF);
            drawCenteredString(guiGraphics, this.font, "§7Please restart your game to apply the update", centerX, centerY, 0xFFFFFF);
            drawCenteredString(guiGraphics, this.font, "§eRestarting is required...", centerX, centerY + 20, 0xFFFFFF);
        } else {
            // Progress state
            drawCenteredString(guiGraphics, this.font, "§e" + currentStatus, centerX, centerY - 20, 0xFFFFFF);
            
            // Progress bar background
            int barWidth = 300;
            int barHeight = 20;
            int barX = centerX - barWidth / 2;
            int barY = centerY + 10;
            
            // Background (dark gray)
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            
            // Progress fill (green)
            int progressWidth = (int)(barWidth * progress);
            if (progressWidth > 0) {
                guiGraphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00AA00);
            }
            
            // Border
            guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFFFFFFFF); // Top
            guiGraphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFFFFFFFF); // Bottom
            guiGraphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFFFFFFFF); // Left
            guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFFFFFFFF); // Right
            
            // Percentage text
            String percentText = String.format("%.1f%%", progress * 100);
            drawCenteredString(guiGraphics, this.font, percentText, centerX, centerY + 40, 0xFFFFFF);
            
            // Size info
            if (totalSize > 0) {
                long downloadedSize = (long)(totalSize * progress);
                String sizeText = formatFileSize(downloadedSize) + " / " + formatFileSize(totalSize);
                drawCenteredString(guiGraphics, this.font, "§7" + sizeText, centerX, centerY + 55, 0xFFFFFF);
            }
        }
        
        // Warning message
        if (!updateFailed && !updateComplete) {
            drawCenteredString(guiGraphics, this.font, "§7§oPlease do not close this window", centerX, centerY + 80, 0xFFFFFF);
        } else if (updateFailed) {
            drawCenteredString(guiGraphics, this.font, "§7§oPress ESC to return to main menu", centerX, centerY + 80, 0xFFFFFF);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void drawCenteredString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, 
                                    String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }
    
    public void updateProgress(float progress, String status) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        this.currentStatus = status;
    }
    
    public void setComplete() {
        this.updateComplete = true;
        this.progress = 1.0f;
        this.currentStatus = "Update completed successfully!";
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Quit Game"),
            b -> this.minecraft.stop()
        ).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }
    
    public void setFailed(String errorMessage) {
        this.updateFailed = true;
        this.errorMessage = errorMessage;
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Quit Game"),
            b -> this.minecraft.stop()
        ).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return updateFailed; // Allow closing with ESC if failed
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
