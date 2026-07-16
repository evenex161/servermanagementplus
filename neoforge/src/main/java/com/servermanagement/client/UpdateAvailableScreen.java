package com.servermanagement.client;

import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdateAvailableScreen extends Screen {
    private final Screen previousScreen;
    private final UpdateInfo updateInfo;
    private final String currentVersion;

    public UpdateAvailableScreen(Screen previousScreen, UpdateInfo updateInfo, String currentVersion) {
        super(Component.literal("Update Available"));
        this.previousScreen = previousScreen;
        this.updateInfo = updateInfo;
        this.currentVersion = currentVersion;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int totalBtnWidth = 310;
        if (this.width < totalBtnWidth + 20) {
            // Stack vertically
            this.addRenderableWidget(Button.builder(Component.literal("Update Now"), b -> {
                OTAUpdateScreen otaScreen = new OTAUpdateScreen(currentVersion, updateInfo.version(), 0);
                this.minecraft.setScreen(otaScreen);
                
                java.nio.file.Path currentJar = net.neoforged.fml.ModList.get().getModFileById("servermanagement").getFile().getFilePath();
                UpdateManager.downloadAndHandoff(updateInfo.downloadUrl(), true, currentJar,
                    (progress, status) -> otaScreen.updateProgress(progress, status),
                    () -> otaScreen.setComplete(),
                    (error) -> otaScreen.setFailed(error)
                );
            }).bounds(centerX - 100, centerY + 50, 200, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Update Later"), b -> {
                this.minecraft.setScreen(this.previousScreen);
            }).bounds(centerX - 100, centerY + 75, 200, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Skip this update"), b -> {
                UpdatePreferences.skipVersion(updateInfo.version());
                this.minecraft.setScreen(this.previousScreen);
            }).bounds(centerX - 100, centerY + 100, 200, 20).build());
        } else {
            // Original horizontal layout
            this.addRenderableWidget(Button.builder(Component.literal("Update Now"), b -> {
                OTAUpdateScreen otaScreen = new OTAUpdateScreen(currentVersion, updateInfo.version(), 0);
                this.minecraft.setScreen(otaScreen);
                
                java.nio.file.Path currentJar = net.neoforged.fml.ModList.get().getModFileById("servermanagement").getFile().getFilePath();
                UpdateManager.downloadAndHandoff(updateInfo.downloadUrl(), true, currentJar,
                    (progress, status) -> otaScreen.updateProgress(progress, status),
                    () -> otaScreen.setComplete(),
                    (error) -> otaScreen.setFailed(error)
                );
            }).bounds(centerX - 155, centerY + 80, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Update Later"), b -> {
                this.minecraft.setScreen(this.previousScreen);
            }).bounds(centerX - 50, centerY + 80, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Skip this update"), b -> {
                UpdatePreferences.skipVersion(updateInfo.version());
                this.minecraft.setScreen(this.previousScreen);
            }).bounds(centerX + 55, centerY + 80, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xFF1a1a1a, 0xFF2d2d2d);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, "§6§lA New ServerManagement+ Update is Available!", centerX, centerY - 90, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "§7Version: §a" + updateInfo.version() + " §7(Released: " + updateInfo.releaseDate() + ")", centerX, centerY - 75, 0xFFFFFF);
        
        // Render basic changelog
        String[] lines = updateInfo.changelog().split("\n");
        int y = centerY - 55;
        int maxLines = 10;
        
        int textStartX = Math.max(10, centerX - 160);
        int maxTextWidth = this.width - 20;
        
        for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            // Basic markdown stripping
            line = line.replace("**", "").replace("__", "").replace("##", "").replace("#", "");
            if (line.startsWith("- ")) line = "• " + line.substring(2);
            if (line.startsWith("* ")) line = "• " + line.substring(2);
            
            if (line.length() > 65) line = line.substring(0, 62) + "...";
            
            guiGraphics.drawString(this.font, "§f" + line, textStartX, y, 0xFFFFFF, false);
            y += 10;
        }
        
        if (lines.length > maxLines) {
            guiGraphics.drawString(this.font, "§f§o...and more (view on " + updateInfo.source() + ")", textStartX, y, 0xFFFFFF, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

