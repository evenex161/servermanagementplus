package com.servermanagement.gui.screen;

import com.servermanagement.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChangelogScreen extends Screen {
    
    private final Screen previousScreen;
    private final List<String> changelogLines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    private int leftPos;
    private int topPos;
    private int imageWidth = 400;
    private int imageHeight = 250;
    
    public ChangelogScreen(Screen previousScreen) {
        super(Component.literal("Update Changelog"));
        this.previousScreen = previousScreen;
        loadChangelog();
    }
    
    private void loadChangelog() {
        try {
            InputStream stream = ChangelogScreen.class.getResourceAsStream("/assets/servermanagement/changelog.txt");
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // strip emojis since MC font doesn't support them well
                        line = line.replaceAll("[✨🐛]", "").trim();
                        // parse basic markdown bold
                        line = line.replace("**", "");
                        changelogLines.add(line);
                    }
                }
            } else {
                fallbackChangelog();
            }
        } catch (Exception e) {
            fallbackChangelog();
            Constants.LOG.error("Failed to load changelog.txt", e);
        }
    }
    
    private void fallbackChangelog() {
        changelogLines.clear();
        changelogLines.add("Successfully updated ServerManagement+!");
        changelogLines.add("");
        changelogLines.add("Check the Discord or GitHub for full patch notes.");
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.imageWidth = Math.min(500, this.width - 40);
        this.imageHeight = Math.min(300, this.height - 40);
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        int btnWidth = 100;
        int btnHeight = 20;
        
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Awesome!"),
            btn -> this.minecraft.setScreen(this.previousScreen)
        )
        .bounds(this.leftPos + this.imageWidth / 2 - btnWidth / 2, this.topPos + this.imageHeight - 30, btnWidth, btnHeight)
        .build());
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY != 0) {
            this.scrollOffset -= (int)(scrollY * 15);
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xCC1a1a1a, 0xCC2d2d2d);
    }
    
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        
        // Background
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xE0101010);
        // Header
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 35, 0xFF1A1A2E);
        // Separator
        g.fill(this.leftPos, this.topPos + 35, this.leftPos + this.imageWidth, this.topPos + 36, 0xFF333333);
        
        g.drawCenteredString(this.font, "ServerManagement+ Updated Successfully!", this.leftPos + this.imageWidth / 2, this.topPos + 15, 0xFFD700);
        
        // Render text
        int contentTop = this.topPos + 40;
        int contentBottom = this.topPos + this.imageHeight - 40;
        int contentHeight = contentBottom - contentTop;
        
        g.enableScissor(this.leftPos, contentTop, this.leftPos + this.imageWidth, contentBottom);
        
        int y = contentTop - this.scrollOffset;
        int lineSpacing = 12;
        
        for (String line : changelogLines) {
            List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(Component.literal(line), this.imageWidth - 40);
            for (net.minecraft.util.FormattedCharSequence seq : wrapped) {
                if (y + lineSpacing > contentTop && y < contentBottom + lineSpacing) {
                    g.drawString(this.font, seq, this.leftPos + 20, y, 0xFFFFFF, false);
                }
                y += lineSpacing;
            }
        }
        
        g.disableScissor();
        
        // Calculate max scroll
        int totalTextHeight = y + this.scrollOffset - contentTop;
        this.maxScroll = Math.max(0, totalTextHeight - contentHeight);
        
        // Render scrollbar if needed
        if (this.maxScroll > 0) {
            int scrollbarX = this.leftPos + this.imageWidth - 10;
            int scrollbarHeight = Math.max(20, (int)((contentHeight / (float)totalTextHeight) * contentHeight));
            int scrollbarY = contentTop + (int)(((float)this.scrollOffset / this.maxScroll) * (contentHeight - scrollbarHeight));
            
            g.fill(scrollbarX, contentTop, scrollbarX + 4, contentBottom, 0x55000000);
            g.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
        }
        
        super.render(g, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
