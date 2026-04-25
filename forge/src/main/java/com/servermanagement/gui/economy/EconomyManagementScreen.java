package com.servermanagement.gui.economy;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.DeleteTemplatePacket;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.SaveFreeRewardSettingsPacket;
import com.servermanagement.network.packet.SaveTemplatePacket;
import com.servermanagement.network.packet.SyncEconomyTemplatesPacket;
import com.servermanagement.network.packet.ToggleTemplatePacket;
import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Economy Management Screen for admins to configure templates and rewards
 */
public class EconomyManagementScreen extends ScalableContainerScreen<EconomyManagementMenu> {
    
    private static final int TEMPLATE_HEIGHT = 70;
    private static final int TEMPLATE_PADDING = 5;
    
    private enum Tab {
        TASK_TEMPLATES,
        FREE_REWARD,
        STATISTICS
    }
    
    private Tab currentTab = Tab.TASK_TEMPLATES;
    private int scrollOffset = 0;
    private List<SyncEconomyTemplatesPacket.TemplateData> templates = new ArrayList<>();
    private EditBox searchBox;
    private int selectedTemplateIndex = -1;
    private String editTemplateId = null; // ID of template being edited, null for new
    
    // Edit mode fields
    private boolean editMode = false;
    private EditBox editDescriptionBox;
    private EditBox editGoalBox;
    private EditBox editRewardBox;
    private TaskType editTaskType = TaskType.BREAK_BLOCKS;
    private ItemStack editRewardItem = ItemStack.EMPTY; // Item reward for template editing
    // Persisted edit values (survive rebuildWidgets)
    private String pendingDescription = "";
    private String pendingGoal = "";
    private String pendingReward = "";
    
    // Free reward tab fields
    private EditBox freeRewardBox;
    private EditBox cooldownBox;
    private ItemStack freeRewardItemStack = ItemStack.EMPTY; // Item reward for free reward
    
    public EconomyManagementScreen(EconomyManagementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 600, 450);
        this.imageHeight = 450;
        this.imageWidth = 600;
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        
        // Clear all EditBox references when reinitializing
        searchBox = null;
        editDescriptionBox = null;
        editGoalBox = null;
        editRewardBox = null;
        freeRewardBox = null;
        cooldownBox = null;
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Back to Dashboard button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 10, 120, 20,
            Component.literal("← Dashboard"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD)),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Close button
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 90, centerY + 10, 80, 20,
            Component.literal("Close"),
            button -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        // Tab buttons
        int tabW = (this.imageWidth - 40) / 3;
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 50, tabW, 25,
            Component.literal("Task Templates"),
            button -> switchTab(Tab.TASK_TEMPLATES),
            currentTab == Tab.TASK_TEMPLATES ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));
        
        this.addRenderableWidget(new ModernButton(
            centerX + 10 + tabW + 5, centerY + 50, tabW, 25,
            Component.literal("Free Reward"),
            button -> switchTab(Tab.FREE_REWARD),
            currentTab == Tab.FREE_REWARD ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));
        
        this.addRenderableWidget(new ModernButton(
            centerX + 10 + (tabW + 5) * 2, centerY + 50, tabW, 25,
            Component.literal("Statistics"),
            button -> switchTab(Tab.STATISTICS),
            currentTab == Tab.STATISTICS ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Load templates from client cache
        templates = new ArrayList<>(ClientPacketHandler.getCachedTemplates());
        
        if (currentTab == Tab.TASK_TEMPLATES) {
            initTaskTemplatesTab(centerX, centerY);
        } else if (currentTab == Tab.FREE_REWARD) {
            initFreeRewardTab(centerX, centerY);
        } else if (currentTab == Tab.STATISTICS) {
            initStatisticsTab(centerX, centerY);
        }
    }
    
    private void initTaskTemplatesTab(int centerX, int centerY) {
        if (!editMode) {
            // Search box (left side, below tabs)
            if (searchBox == null) {
                searchBox = new EditBox(this.font, centerX + 20, centerY + 88, 180, 15, Component.literal("Search"));
                searchBox.setMaxLength(50);
                searchBox.setHint(Component.literal("Search templates..."));
            }
            searchBox.setPosition(centerX + 20, centerY + 88);
            this.addRenderableWidget(searchBox);
            
            // Create New Template button (right side, below tabs)
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 150, centerY + 85, 130, 22,
                Component.literal("+ New Template"),
                button -> createNewTemplate(),
                ModernButton.ButtonStyle.SUCCESS
            ));
            // Template list buttons (Edit/Delete for each template)
            for (int i = 0; i < Math.min(3, templates.size() - scrollOffset); i++) {
                int templateIndex = i + scrollOffset;
                int yPos = centerY + 112 + (i * (TEMPLATE_HEIGHT + TEMPLATE_PADDING));
                
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 145, yPos + 22, 55, 20,
                    Component.literal("Edit"),
                    button -> editTemplate(templateIndex),
                    ModernButton.ButtonStyle.PRIMARY
                ));
                
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 85, yPos + 22, 60, 20,
                    Component.literal("Delete"),
                    button -> deleteTemplate(templateIndex),
                    ModernButton.ButtonStyle.DANGER
                ));
                
                // Enable/Disable toggle
                SyncEconomyTemplatesPacket.TemplateData template = templates.get(templateIndex);
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 145, yPos + 46, 120, 20,
                    Component.literal(template.enabled() ? "Enabled" : "Disabled"),
                    button -> toggleTemplate(templateIndex),
                    template.enabled() ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
                ));
            }
            
            // Scroll buttons (positioned below cards, side by side)
            int scrollY = centerY + this.imageHeight - 42;
            if (scrollOffset > 0) {
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth / 2 - 85, scrollY, 80, 22,
                    Component.literal("▲ Previous"),
                    button -> { scrollOffset--; this.rebuildWidgets(); },
                    ModernButton.ButtonStyle.SECONDARY
                ));
            }
            
            if (scrollOffset + 3 < templates.size()) {
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth / 2 + 5, scrollY, 80, 22,
                    Component.literal("▼ Next"),
                    button -> { scrollOffset++; this.rebuildWidgets(); },
                    ModernButton.ButtonStyle.SECONDARY
                ));
            }
        } else {
            // Edit mode UI
            initEditMode(centerX, centerY);
        }
    }
    
    private void initEditMode(int centerX, int centerY) {
        int formX = centerX + 30;
        int formY = centerY + 115;
        
        // Task Type selector
        this.addRenderableWidget(new ModernButton(
            formX, formY, 100, 20,
            Component.literal("◄ Type"),
            button -> cycleTaskType(-1),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        this.addRenderableWidget(new ModernButton(
            formX + 110, formY, 100, 20,
            Component.literal("Type ►"),
            button -> cycleTaskType(1),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Description input
        editDescriptionBox = new EditBox(this.font, formX, formY + 50, this.imageWidth - 100, 20, Component.literal("Description"));
        editDescriptionBox.setMaxLength(100);
        editDescriptionBox.setHint(Component.literal("Task description..."));
        editDescriptionBox.setValue(pendingDescription);
        this.addRenderableWidget(editDescriptionBox);
        
        // Goal input
        editGoalBox = new EditBox(this.font, formX, formY + 100, 150, 20, Component.literal("Goal"));
        editGoalBox.setMaxLength(10);
        editGoalBox.setHint(Component.literal("Goal amount..."));
        editGoalBox.setFilter(s -> s.matches("\\d*")); // Numbers only
        editGoalBox.setValue(pendingGoal);
        this.addRenderableWidget(editGoalBox);
        
        // Reward input
        editRewardBox = new EditBox(this.font, formX + 170, formY + 100, 150, 20, Component.literal("Reward"));
        editRewardBox.setMaxLength(10);
        editRewardBox.setHint(Component.literal("Reward ($)..."));
        editRewardBox.setFilter(s -> s.matches("\\d*")); // Numbers only
        editRewardBox.setValue(pendingReward);
        this.addRenderableWidget(editRewardBox);
        
        // Save button (below item slot row)
        this.addRenderableWidget(new ModernButton(
            formX, formY + 200, 120, 25,
            Component.literal("Save"),
            button -> saveTemplate(),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        // Cancel button
        this.addRenderableWidget(new ModernButton(
            formX + 130, formY + 200, 120, 25,
            Component.literal("Cancel"),
            button -> cancelEdit(),
            ModernButton.ButtonStyle.DANGER
        ));
    }
    
    private void initFreeRewardTab(int centerX, int centerY) {
        int formX = centerX + 50;
        int formY = centerY + 130;
        
        // Free reward amount
        if (freeRewardBox == null) {
            freeRewardBox = new EditBox(this.font, formX, formY, 200, 20, Component.literal("Free Reward Amount"));
            freeRewardBox.setMaxLength(10);
            freeRewardBox.setValue(String.valueOf(ClientPacketHandler.getCachedFreeRewardAmount()));
            freeRewardBox.setFilter(s -> s.matches("\\d*"));
        }
        this.addRenderableWidget(freeRewardBox);
        
        // Cooldown hours
        if (cooldownBox == null) {
            cooldownBox = new EditBox(this.font, formX, formY + 40, 200, 20, Component.literal("Cooldown (hours)"));
            cooldownBox.setMaxLength(3);
            cooldownBox.setValue(String.valueOf(ClientPacketHandler.getCachedFreeRewardCooldownHours()));
            cooldownBox.setFilter(s -> s.matches("\\d*"));
        }
        this.addRenderableWidget(cooldownBox);
        
        // Save button (below item slot help text)
        this.addRenderableWidget(new ModernButton(
            formX, formY + 125, 100, 25,
            Component.literal("Save"),
            button -> saveFreeRewardSettings(freeRewardBox.getValue(), cooldownBox.getValue()),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    
    private void initStatisticsTab(int centerX, int centerY) {
        // Refresh button
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 140, centerY + 88, 120, 20,
            Component.literal("↻ Refresh"),
            button -> {
                ModNetworking.sendToServer(new com.servermanagement.network.packet.RequestEconomyStatsPacket());
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void switchTab(Tab tab) {
        com.servermanagement.gui.debug.DebugLogger.logTabChange("EconomyManagementScreen", currentTab.name(), tab.name());
        this.currentTab = tab;
        this.scrollOffset = 0;
        this.editMode = false;
        this.selectedTemplateIndex = -1;
        this.rebuildWidgets();
    }
    
    private void createNewTemplate() {
        editMode = true;
        selectedTemplateIndex = -1;
        editTemplateId = null;
        editTaskType = TaskType.BREAK_BLOCKS;
        editRewardItem = ItemStack.EMPTY;
        pendingDescription = "";
        pendingGoal = "";
        pendingReward = "";
        this.rebuildWidgets();
    }
    
    private void editTemplate(int index) {
        editMode = true;
        selectedTemplateIndex = index;
        SyncEconomyTemplatesPacket.TemplateData template = templates.get(index);
        editTemplateId = template.id();
        editTaskType = template.getTaskType();
        editRewardItem = template.rewardItem() != null ? template.rewardItem().copy() : ItemStack.EMPTY;
        pendingDescription = template.description() != null ? template.description() : "";
        pendingGoal = String.valueOf(template.goal());
        pendingReward = String.valueOf(template.rewardAmount());
        this.rebuildWidgets();
    }
    
    private void deleteTemplate(int index) {
        SyncEconomyTemplatesPacket.TemplateData template = templates.get(index);
        ModNetworking.sendToServer(new DeleteTemplatePacket(template.id()));
        templates.remove(index);
        this.rebuildWidgets();
    }
    
    private void toggleTemplate(int index) {
        SyncEconomyTemplatesPacket.TemplateData template = templates.get(index);
        ModNetworking.sendToServer(new ToggleTemplatePacket(template.id()));
        // Optimistic UI update
        templates.set(index, new SyncEconomyTemplatesPacket.TemplateData(
            template.id(), template.taskTypeOrdinal(), template.description(),
            template.goal(), template.rewardAmount(), !template.enabled(),
            template.rewardItem()
        ));
        this.rebuildWidgets();
    }
    
    private void cycleTaskType(int direction) {
        TaskType[] types = TaskType.values();
        int currentIndex = editTaskType.ordinal();
        int newIndex = (currentIndex + direction + types.length) % types.length;
        editTaskType = types[newIndex];
    }
    
    private void saveTemplate() {
        String description = editDescriptionBox != null ? editDescriptionBox.getValue() : "";
        int goal = 0;
        int reward = 0;
        try {
            goal = editGoalBox != null && !editGoalBox.getValue().isEmpty() ? Integer.parseInt(editGoalBox.getValue()) : 0;
            reward = editRewardBox != null && !editRewardBox.getValue().isEmpty() ? Integer.parseInt(editRewardBox.getValue()) : 0;
        } catch (NumberFormatException ignored) {}
        
        if (goal > 0 && reward > 0) {
            ModNetworking.sendToServer(new SaveTemplatePacket(
                editTemplateId, editTaskType, description, goal, reward, editRewardItem
            ));
        }
        
        editMode = false;
        editTemplateId = null;
        this.rebuildWidgets();
    }
    
    private void cancelEdit() {
        editMode = false;
        editTemplateId = null;
        selectedTemplateIndex = -1;
        this.rebuildWidgets();
    }
    
    private void saveFreeRewardSettings(String amount, String cooldown) {
        int rewardAmount = 0;
        int cooldownHours = 0;
        try {
            rewardAmount = !amount.isEmpty() ? Integer.parseInt(amount) : 0;
            cooldownHours = !cooldown.isEmpty() ? Integer.parseInt(cooldown) : 0;
        } catch (NumberFormatException ignored) {}
        
        if (rewardAmount > 0 && cooldownHours > 0) {
            ModNetworking.sendToServer(new SaveFreeRewardSettingsPacket(rewardAmount, cooldownHours, freeRewardItemStack));
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Main background
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + this.imageHeight, 0xE0101010);
        
        // Header bar
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 40, 0xFF1A1A2E);
        guiGraphics.fill(centerX, centerY + 40, centerX + this.imageWidth, centerY + 41, 0xFF333333);
        
        // Tab content area
        guiGraphics.fill(centerX + 10, centerY + 80, centerX + this.imageWidth - 10, 
            centerY + this.imageHeight - 15, 0xE01A1A1A);
        
        if (currentTab == Tab.TASK_TEMPLATES && !editMode) {
            // Render template boxes
            for (int i = 0; i < Math.min(3, templates.size() - scrollOffset); i++) {
                int templateIndex = i + scrollOffset;
                int yPos = centerY + 112 + (i * (TEMPLATE_HEIGHT + TEMPLATE_PADDING));
                
                // Template background
                guiGraphics.fill(centerX + 20, yPos, centerX + this.imageWidth - 20, 
                    yPos + TEMPLATE_HEIGHT, 0xE0252525);
            }
        } else if (editMode) {
            // Edit form background
            guiGraphics.fill(centerX + 15, centerY + 100, centerX + this.imageWidth - 15, 
                centerY + this.imageHeight - 100, 0xE0252525);
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Title
        Component titleText = Component.literal("Economy Management");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, 
            centerX + (this.imageWidth - titleWidth) / 2, 
            centerY + 16, 
            0xFFD700, true);
        
        if (currentTab == Tab.TASK_TEMPLATES) {
            renderTaskTemplatesTab(guiGraphics, centerX, centerY);
        } else if (currentTab == Tab.FREE_REWARD) {
            renderFreeRewardTab(guiGraphics, centerX, centerY);
        } else if (currentTab == Tab.STATISTICS) {
            renderStatisticsTab(guiGraphics, centerX, centerY);
        }
    }
    
    private void renderTaskTemplatesTab(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (!editMode) {
            if (templates.isEmpty()) {
                guiGraphics.drawString(this.font, Component.literal("No templates created yet. Click '+ New Template' to create one."),
                    centerX + 30, centerY + 150, 0x888888, true);
            } else {
                // Render template list
                for (int i = 0; i < Math.min(3, templates.size() - scrollOffset); i++) {
                    int templateIndex = i + scrollOffset;
                    SyncEconomyTemplatesPacket.TemplateData template = templates.get(templateIndex);
                    int yPos = centerY + 112 + (i * (TEMPLATE_HEIGHT + TEMPLATE_PADDING));
                    
                    // Template number
                    guiGraphics.drawString(this.font, Component.literal("#" + (templateIndex + 1)),
                        centerX + 30, yPos + 5, 0xFFAA00, true);
                    
                    // Task type and description
                    guiGraphics.drawString(this.font, Component.literal(template.getTaskType().getDisplayName() + ": " + template.description()),
                        centerX + 60, yPos + 5, 0xFFFFFF, true);
                    
                    // Goal and reward
                    guiGraphics.drawString(this.font, Component.literal("Goal: " + template.goal()),
                        centerX + 30, yPos + 18, 0xCCCCCC, true);
                    
                    guiGraphics.drawString(this.font, Component.literal("Reward: $" + template.rewardAmount()),
                        centerX + 30, yPos + 31, 0x55FF55, true);
                    
                    // Show reward item if set
                    if (template.rewardItem() != null && !template.rewardItem().isEmpty()) {
                        int itemX = centerX + 160;
                        guiGraphics.renderItem(template.rewardItem(), itemX, yPos + 27);
                        guiGraphics.drawString(this.font, 
                            Component.literal("+ " + template.rewardItem().getHoverName().getString()),
                            itemX + 20, yPos + 31, 0x55FFAA, true);
                    }
                    
                    // Status
                    String status = template.enabled() ? "Active" : "Disabled";
                    int statusColor = template.enabled() ? 0x55FF55 : 0x888888;
                    guiGraphics.drawString(this.font, Component.literal(status),
                        centerX + 30, yPos + 52, statusColor, true);
                }
            }
        } else {
            // Render edit form
            guiGraphics.drawString(this.font, Component.literal(selectedTemplateIndex == -1 ? "Create New Template" : "Edit Template"),
                centerX + 35, centerY + 90, 0xFFD700, true);
            
            int formY = centerY + 115;
            
            // Labels
            guiGraphics.drawString(this.font, Component.literal("Task Type:"),
                centerX + 35, formY - 10, 0xFFFFFF, true);
            
            // Show current task type
            if (editTaskType != null) {
                guiGraphics.drawString(this.font, Component.literal(editTaskType.getDisplayName()),
                    centerX + 220, formY + 5, 0xFFD700, true);
            }
            
            guiGraphics.drawString(this.font, Component.literal("Description:"),
                centerX + 35, formY + 35, 0xFFFFFF, true);
            
            guiGraphics.drawString(this.font, Component.literal("Goal:"),
                centerX + 35, formY + 85, 0xFFFFFF, true);
            
            guiGraphics.drawString(this.font, Component.literal("Reward ($):"),
                centerX + 205, formY + 85, 0xFFFFFF, true);
            
            guiGraphics.drawString(this.font, Component.literal("Reward Item (optional):"),
                centerX + 35, formY + 135, 0xFFFFFF, true);
            
            // Item slot visual
            int itemSlotX = centerX + 35;
            int itemSlotY = formY + 150;
            guiGraphics.fill(itemSlotX - 1, itemSlotY - 1, itemSlotX + 19, itemSlotY + 19, 0xFFFFFFFF);
            guiGraphics.fill(itemSlotX, itemSlotY, itemSlotX + 18, itemSlotY + 18, 0xFF8B8B8B);
            
            if (!editRewardItem.isEmpty()) {
                guiGraphics.renderItem(editRewardItem, itemSlotX, itemSlotY);
                guiGraphics.renderItemDecorations(this.font, editRewardItem, itemSlotX, itemSlotY);
                
                guiGraphics.drawString(this.font, 
                    Component.literal(editRewardItem.getHoverName().getString() + " x" + editRewardItem.getCount()),
                    itemSlotX + 25, itemSlotY + 5, 0x55FF55, true);
            } else {
                guiGraphics.drawString(this.font, 
                    Component.literal("(None)"),
                    itemSlotX + 25, itemSlotY + 5, 0x888888, true);
            }
            
            guiGraphics.drawString(this.font, 
                Component.literal("Tip: Select an item in your hotbar and click the slot to set reward item"),
                centerX + 35, itemSlotY + 30, 0x888888, true);
        }
    }
    
    private void renderFreeRewardTab(GuiGraphics guiGraphics, int centerX, int centerY) {
        int formY = centerY + 130;
        
        guiGraphics.drawString(this.font, Component.literal("Configure Free Daily Reward"),
            centerX + 55, centerY + 95, 0xFFAA00, true);
        
        guiGraphics.drawString(this.font, Component.literal("Reward Amount ($):"),
            centerX + 55, formY - 15, 0xFFFFFF, true);
        
        guiGraphics.drawString(this.font, Component.literal("Cooldown (hours):"),
            centerX + 55, formY + 25, 0xFFFFFF, true);
        
        guiGraphics.drawString(this.font, Component.literal("Reward Item (optional):"),
            centerX + 55, formY + 65, 0xFFFFFF, true);
        
        // Item slot visual
        int itemSlotX = centerX + 55;
        int itemSlotY = formY + 80;
        guiGraphics.fill(itemSlotX - 1, itemSlotY - 1, itemSlotX + 19, itemSlotY + 19, 0xFFFFFFFF);
        guiGraphics.fill(itemSlotX, itemSlotY, itemSlotX + 18, itemSlotY + 18, 0xFF8B8B8B);
        
        if (!freeRewardItemStack.isEmpty()) {
            guiGraphics.renderItem(freeRewardItemStack, itemSlotX, itemSlotY);
            guiGraphics.renderItemDecorations(this.font, freeRewardItemStack, itemSlotX, itemSlotY);
            
            guiGraphics.drawString(this.font, 
                Component.literal(freeRewardItemStack.getHoverName().getString() + " x" + freeRewardItemStack.getCount()),
                itemSlotX + 25, itemSlotY + 5, 0x55FF55, true);
        } else {
            guiGraphics.drawString(this.font, 
                Component.literal("(None)"),
                itemSlotX + 25, itemSlotY + 5, 0x888888, true);
        }
        
        guiGraphics.drawString(this.font, 
            Component.literal("Select an item in your hotbar and click the slot to set reward item"),
            centerX + 55, itemSlotY + 25, 0x888888, true);
        
        guiGraphics.drawString(this.font, Component.literal("Tip: Players can claim this reward once per cooldown period"),
            centerX + 55, formY + 160, 0x888888, true);
    }
    
    private void renderStatisticsTab(GuiGraphics guiGraphics, int centerX, int centerY) {
        int x = centerX + 25;
        int y = centerY + 90;
        int col2 = centerX + this.imageWidth / 2 + 10;
        int lineH = 14;
        
        // Section: Economy Overview
        guiGraphics.drawString(this.font, Component.literal("Economy Overview"),
            x, y, 0xFFD700, true);
        y += lineH + 2;
        
        drawStatLine(guiGraphics, x, y, "Total Accounts:", 
            String.valueOf(ClientPacketHandler.getStatTotalAccounts()), 0xFFFFFF, 0x55FF55);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Money in Circulation:", 
            "$" + String.format("%,.2f", ClientPacketHandler.getStatTotalMoney()), 0xFFFFFF, 0xFFAA00);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Average Balance:", 
            "$" + String.format("%,.2f", ClientPacketHandler.getStatAverageBalance()), 0xFFFFFF, 0x55FFFF);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Richest Player:", 
            ClientPacketHandler.getStatRichestPlayerName() + " ($" + String.format("%,.2f", ClientPacketHandler.getStatRichestBalance()) + ")",
            0xFFFFFF, 0xFFD700);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Inflation Multiplier:", 
            String.format("%.2fx", ClientPacketHandler.getStatInflation()), 0xFFFFFF, getInflationColor(ClientPacketHandler.getStatInflation()));
        y += lineH + 6;
        
        // Section: Marketplace
        guiGraphics.drawString(this.font, Component.literal("Marketplace"),
            x, y, 0xFFD700, true);
        y += lineH + 2;
        
        drawStatLine(guiGraphics, x, y, "Active MineBay Listings:", 
            String.valueOf(ClientPacketHandler.getStatActiveListings()), 0xFFFFFF, 0x55FF55);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Total Purchases:", 
            ClientPacketHandler.getStatPurchaseCount() + " ($" + String.format("%,.2f", ClientPacketHandler.getStatTotalPurchaseVolume()) + ")",
            0xFFFFFF, 0x55FFFF);
        y += lineH;
        drawStatLine(guiGraphics, x, y, "Total Sales:", 
            ClientPacketHandler.getStatSaleCount() + " ($" + String.format("%,.2f", ClientPacketHandler.getStatTotalSaleVolume()) + ")",
            0xFFFFFF, 0x55FF55);
        y += lineH + 6;
        
        // Section: Gambling (right column or continue below)
        int y2 = centerY + 90;
        guiGraphics.drawString(this.font, Component.literal("MineStacks Gambling"),
            col2, y2, 0xFFD700, true);
        y2 += lineH + 2;
        
        drawStatLine(guiGraphics, col2, y2, "Total Bets:", 
            String.valueOf(ClientPacketHandler.getStatGamblingBetCount()), 0xFFFFFF, 0xFFAA00);
        y2 += lineH;
        drawStatLine(guiGraphics, col2, y2, "Total Wins:", 
            String.valueOf(ClientPacketHandler.getStatGamblingWinCount()), 0xFFFFFF, 0x55FF55);
        y2 += lineH;
        drawStatLine(guiGraphics, col2, y2, "Total Wagered:", 
            "$" + String.format("%,.2f", ClientPacketHandler.getStatTotalGamblingWagered()), 0xFFFFFF, 0xFF5555);
        y2 += lineH;
        drawStatLine(guiGraphics, col2, y2, "Total Won:", 
            "$" + String.format("%,.2f", ClientPacketHandler.getStatTotalGamblingWon()), 0xFFFFFF, 0x55FF55);
        y2 += lineH;
        double houseProfit = ClientPacketHandler.getStatTotalGamblingWagered() - ClientPacketHandler.getStatTotalGamblingWon();
        int houseProfitColor = houseProfit >= 0 ? 0x55FF55 : 0xFF5555;
        drawStatLine(guiGraphics, col2, y2, "House Profit:", 
            "$" + String.format("%,.2f", houseProfit), 0xFFFFFF, houseProfitColor);
        y2 += lineH + 6;
        
        // Section: Daily Tasks & Rewards
        guiGraphics.drawString(this.font, Component.literal("Tasks & Rewards"),
            col2, y2, 0xFFD700, true);
        y2 += lineH + 2;
        
        drawStatLine(guiGraphics, col2, y2, "Task Templates:", 
            ClientPacketHandler.getStatEnabledTemplates() + " / " + ClientPacketHandler.getStatTotalTemplates() + " enabled",
            0xFFFFFF, 0x55FFFF);
        y2 += lineH;
        drawStatLine(guiGraphics, col2, y2, "Free Rewards Claimed:", 
            String.valueOf(ClientPacketHandler.getStatFreeRewardCount()), 0xFFFFFF, 0x55FF55);
        y2 += lineH;
        drawStatLine(guiGraphics, col2, y2, "Player Transfers:", 
            String.valueOf(ClientPacketHandler.getStatTransferCount()), 0xFFFFFF, 0xFFAA00);
        y2 += lineH + 6;
        
        // Section: Transaction Summary
        guiGraphics.drawString(this.font, Component.literal("Transaction Summary"),
            col2, y2, 0xFFD700, true);
        y2 += lineH + 2;
        drawStatLine(guiGraphics, col2, y2, "Total Transactions:", 
            String.valueOf(ClientPacketHandler.getStatTotalTransactions()), 0xFFFFFF, 0xFFFFFF);
    }
    
    private void drawStatLine(GuiGraphics guiGraphics, int x, int y, String label, String value, int labelColor, int valueColor) {
        guiGraphics.drawString(this.font, Component.literal(label), x, y, labelColor, true);
        int labelWidth = this.font.width(label);
        guiGraphics.drawString(this.font, Component.literal(" " + value), x + labelWidth, y, valueColor, true);
    }
    
    private int getInflationColor(double inflation) {
        if (inflation <= 1.0) return 0x55FF55;    // Green - low/normal
        if (inflation <= 2.0) return 0xFFFF55;     // Yellow - moderate
        if (inflation <= 5.0) return 0xFFAA00;     // Orange - high
        return 0xFF5555;                            // Red - extreme
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Handle item slot clicks for template editing
        if (editMode && currentTab == Tab.TASK_TEMPLATES) {
            int formY = centerY + 115;
            int itemSlotX = centerX + 35;
            int itemSlotY = formY + 150;
            
            if (mouseX >= itemSlotX && mouseX < itemSlotX + 18 && 
                mouseY >= itemSlotY && mouseY < itemSlotY + 18) {
                handleItemSlotClick(true);
                return true;
            }
        }
        
        // Handle item slot clicks for free reward
        if (currentTab == Tab.FREE_REWARD) {
            int formY = centerY + 130;
            int itemSlotX = centerX + 55;
            int itemSlotY = formY + 80;
            
            if (mouseX >= itemSlotX && mouseX < itemSlotX + 18 && 
                mouseY >= itemSlotY && mouseY < itemSlotY + 18) {
                handleItemSlotClick(false);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void handleItemSlotClick(boolean isTemplateEdit) {
        if (this.minecraft != null && this.minecraft.player != null) {
            // Use currently selected hotbar item (since this screen has no inventory slots)
            ItemStack selectedItem = this.minecraft.player.getInventory().getSelected();
            
            if (!selectedItem.isEmpty()) {
                // Set the selected hotbar item as the reward item
                if (isTemplateEdit) {
                    editRewardItem = selectedItem.copy();
                } else {
                    freeRewardItemStack = selectedItem.copy();
                }
            } else {
                // Clear item when clicking with empty hand
                if (isTemplateEdit) {
                    editRewardItem = ItemStack.EMPTY;
                } else {
                    freeRewardItemStack = ItemStack.EMPTY;
                }
            }
        }
    }
}
