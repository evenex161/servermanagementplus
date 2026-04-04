package com.servermanagement.gui.economy;

import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Professional Achievements GUI
 * Displays earned achievements and total rewards
 */
public class AchievementsScreen extends AbstractContainerScreen<AchievementsMenu> {
    
    private static final int ACHIEVEMENTS_PER_PAGE = 8;
    private int currentPage = 0;
    private int maxPages = 0;
    private List<String> achievementsList;
    
    private ModernButton prevPageButton;
    private ModernButton nextPageButton;
    
    public AchievementsScreen(AchievementsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 300;
        this.imageWidth = 400;
        Set<String> earned = menu.getEarnedAchievements();
        this.achievementsList = earned != null ? new ArrayList<>(earned) : new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Back to Bank button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 10, 100, 20,
            Component.literal("← Bank"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.BANK)),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Close button
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 90, centerY + 10, 80, 20,
            Component.literal("Close"),
            button -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        // Navigation buttons
        prevPageButton = new ModernButton(
            centerX + 20, centerY + 270, 80, 20,
            Component.literal("← Previous"),
            button -> {
                if (currentPage > 0) {
                    currentPage--;
                }
            },
            ModernButton.ButtonStyle.SECONDARY
        );
        this.addRenderableWidget(prevPageButton);
        
        nextPageButton = new ModernButton(
            centerX + this.imageWidth - 100, centerY + 270, 80, 20,
            Component.literal("Next →"),
            button -> {
                if (currentPage < maxPages - 1) {
                    currentPage++;
                }
            },
            ModernButton.ButtonStyle.SECONDARY
        );
        this.addRenderableWidget(nextPageButton);
        
        updatePagination();
    }
    
    private void updatePagination() {
        maxPages = Math.max(1, (achievementsList.size() + ACHIEVEMENTS_PER_PAGE - 1) / ACHIEVEMENTS_PER_PAGE);
        
        if (currentPage >= maxPages) {
            currentPage = Math.max(0, maxPages - 1);
        }
        
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < maxPages - 1;
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Main dark background
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + this.imageHeight, 0xE0101010);
        
        // Header bar
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 40, 0xE0202020);
        
        // Stats section background
        guiGraphics.fill(centerX + 10, centerY + 50, centerX + this.imageWidth - 10, centerY + 90, 0xE01A1A1A);
        
        // Achievements list background
        guiGraphics.fill(centerX + 10, centerY + 100, centerX + this.imageWidth - 10, centerY + 260, 0xE01A1A1A);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Title
        Component titleText = Component.literal("Achievements");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, 
            centerX + (this.imageWidth - titleWidth) / 2, 
            centerY + 16, 
            0xFFFFFF, false);
        
        // Stats
        renderStats(guiGraphics, centerX, centerY);
        
        // Achievements list
        renderAchievements(guiGraphics, centerX, centerY);
        
        // Page indicator
        if (maxPages > 1) {
            String pageText = "Page " + (currentPage + 1) + " / " + maxPages;
            int pageWidth = this.font.width(pageText);
            guiGraphics.drawString(this.font, Component.literal(pageText),
                centerX + (this.imageWidth - pageWidth) / 2, 
                centerY + 275, 
                0x808080, false);
        }
    }
    
    private void renderStats(GuiGraphics guiGraphics, int centerX, int centerY) {
        int achievementsCount = achievementsList.size();
        int totalRewards = menu.getTotalRewards();
        
        // Achievements count
        guiGraphics.drawString(this.font, Component.literal("Achievements Earned:"),
            centerX + 20, centerY + 58, 0xCCCCCC, false);
        guiGraphics.drawString(this.font, Component.literal(String.valueOf(achievementsCount)),
            centerX + 180, centerY + 58, 0xFFAA00, false);
        
        // Total rewards
        guiGraphics.drawString(this.font, Component.literal("Total Rewards:"),
            centerX + 20, centerY + 73, 0xCCCCCC, false);
        guiGraphics.drawString(this.font, Component.literal("$" + totalRewards),
            centerX + 180, centerY + 73, 0x55FF55, false);
    }
    
    private void renderAchievements(GuiGraphics guiGraphics, int centerX, int centerY) {
        guiGraphics.drawString(this.font, Component.literal("Earned Achievements:"),
            centerX + 20, centerY + 107, 0xCCCCCC, false);
        
        if (achievementsList.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("No achievements earned yet"),
                centerX + 30, centerY + 150, 0x808080, false);
            guiGraphics.drawString(this.font, Component.literal("Complete vanilla Minecraft achievements"),
                centerX + 30, centerY + 165, 0x808080, false);
            guiGraphics.drawString(this.font, Component.literal("to earn rewards!"),
                centerX + 30, centerY + 180, 0x808080, false);
            return;
        }
        
        updatePagination();
        
        int startIndex = currentPage * ACHIEVEMENTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACHIEVEMENTS_PER_PAGE, achievementsList.size());
        
        int yOffset = 125;
        for (int i = startIndex; i < endIndex; i++) {
            String achievementId = achievementsList.get(i);
            
            // Alternating row background
            if ((i - startIndex) % 2 == 0) {
                guiGraphics.fill(centerX + 15, centerY + yOffset - 2, 
                    centerX + this.imageWidth - 15, centerY + yOffset + 14, 0x201A1A1A);
            } else {
                guiGraphics.fill(centerX + 15, centerY + yOffset - 2, 
                    centerX + this.imageWidth - 15, centerY + yOffset + 14, 0x20252525);
            }
            
            // Achievement icon/checkmark
            guiGraphics.drawString(this.font, Component.literal("✓"),
                centerX + 20, centerY + yOffset, 0x55FF55, false);
            
            // Achievement name (simplified ID)
            String displayName = formatAchievementName(achievementId);
            guiGraphics.drawString(this.font, Component.literal(displayName),
                centerX + 35, centerY + yOffset, 0xFFFFFF, false);
            
            yOffset += 18;
        }
    }
    
    private String formatAchievementName(String achievementId) {
        if (achievementId == null) return "Unknown";
        // Remove namespace prefix if present
        if (achievementId.contains(":")) {
            achievementId = achievementId.substring(achievementId.indexOf(":") + 1);
        }
        
        // Remove path separators
        achievementId = achievementId.replace("/", " ");
        achievementId = achievementId.replace("_", " ");
        
        // Capitalize
        String[] words = achievementId.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        
        String formatted = result.toString().trim();
        
        // Truncate if too long
        if (formatted.length() > 40) {
            formatted = formatted.substring(0, 37) + "...";
        }
        
        return formatted;
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
