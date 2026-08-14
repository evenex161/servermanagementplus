package com.servermanagement.gui.screen;

import com.servermanagement.updater.UpdateCheckResult;
import com.servermanagement.updater.UpdateInfo;
import com.servermanagement.updater.UpdateManager;
import com.servermanagement.updater.UpdatePreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;

public class UpdateSourceScreen extends Screen {
    private final Screen previousScreen;
    private final UpdateCheckResult result;
    private final String currentVersion;
    private final boolean isClient;

    private boolean askEveryTime;
    private boolean checkFallback;
    private String updateChannel;

    private final java.util.function.Consumer<UpdateInfo> onProceed;
    private final Runnable onRefresh;

    public UpdateSourceScreen(Screen previousScreen, UpdateCheckResult result, String currentVersion, boolean isClient, java.util.function.Consumer<UpdateInfo> onProceed, Runnable onRefresh) {
        super(Component.literal("Update Source Configuration"));
        this.previousScreen = previousScreen;
        this.result = result;
        this.currentVersion = currentVersion;
        this.isClient = isClient;
        this.onProceed = onProceed;
        this.onRefresh = onRefresh;
        
        // Initialize with saved preferences
        this.askEveryTime = UpdatePreferences.getMainSource().equals("ask");
        this.checkFallback = UpdatePreferences.isCheckFallback();
        this.updateChannel = UpdatePreferences.getUpdateChannel(currentVersion);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Modrinth"), b -> selectSource("modrinth"))
                .bounds(centerX - 105, centerY + 20, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("CurseForge"), b -> selectSource("curseforge"))
                .bounds(centerX + 5, centerY + 20, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(askEveryTime ? "Ask every time: [ON]" : "Ask every time: [OFF]"), b -> {
            this.askEveryTime = !this.askEveryTime;
            this.refreshWidgets();
        }).bounds(centerX - 105, centerY + 50, 210, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(checkFallback ? "Fallback if delayed: [ON]" : "Fallback if delayed: [OFF]"), b -> {
            this.checkFallback = !this.checkFallback;
            this.refreshWidgets();
        }).bounds(centerX - 105, centerY + 70, 210, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(updateChannel.equals("beta") ? "Update Path: [Beta Releases]" : "Update Path: [Public Releases]"), b -> {
            this.updateChannel = updateChannel.equals("beta") ? "release" : "beta";
            UpdatePreferences.setUpdateChannel(this.updateChannel);
            UpdatePreferences.save();
            if (this.onRefresh != null) {
                this.onRefresh.run();
                this.minecraft.setScreen(null); // Show dirt background or previous while loading
            }
        }).bounds(centerX - 105, centerY + 95, 210, 20).build());
        
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            this.minecraft.setScreen(this.previousScreen);
        }).bounds(centerX - 105, centerY + 125, 210, 20).build());
    }
    
    private void refreshWidgets() {
        this.clearWidgets();
        this.init();
    }

    private void selectSource(String source) {
        UpdatePreferences.setMainSource(askEveryTime ? "ask" : source);
        UpdatePreferences.setCheckFallback(checkFallback);
        UpdatePreferences.save();

        UpdateInfo target = result.resolve(source, checkFallback);
        
        if (target != null) {
            onProceed.accept(target);
        } else {
            // Source doesn't have an update
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xCC1a1a1a, 0xCC2d2d2d);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, "§6§lUpdate Source Configuration", centerX, centerY - 80, 0xFFFFFF);
        
        String desc = "Select your preferred primary source for downloading updates.";
        guiGraphics.drawCenteredString(this.font, desc, centerX, centerY - 60, 0xAAAAAA);
        
        String desc2 = "If your main source is delayed in approving updates, ServerManagement+";
        String desc3 = "can automatically fallback to the other portal to ensure you are up to date.";
        guiGraphics.drawCenteredString(this.font, desc2, centerX, centerY - 40, 0xAAAAAA);
        guiGraphics.drawCenteredString(this.font, desc3, centerX, centerY - 30, 0xAAAAAA);
    }
}
