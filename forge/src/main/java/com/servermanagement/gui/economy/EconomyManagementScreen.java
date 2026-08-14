package com.servermanagement.gui.economy;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.NodeBasedTemplateEditorWidget;
import com.servermanagement.gui.widgets.FreeRewardEditorWidget;
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
        STATISTICS,
        SETTINGS
    }
    
    private Tab currentTab = Tab.TASK_TEMPLATES;
    private int scrollOffset = 0;
    private List<SyncEconomyTemplatesPacket.TemplateData> templates = new ArrayList<>();
    private EditBox searchBox;
    private int selectedTemplateIndex = -1;
    private String editTemplateId = null; // ID of template being edited, null for new
    
    // Edit mode fields
    private boolean editMode = false;
    private boolean isFullscreen = false;
    private NodeBasedTemplateEditorWidget nodeEditor;
    private TaskType editTaskType = TaskType.BREAK_BLOCKS;
    private List<ItemStack> editRewardItems = new ArrayList<>();
    private FreeRewardEditorWidget templateRewardEditorWidget; // Item reward for template editing
    // Persisted edit values (survive rebuildWidgets)
    private String pendingDescription = "";
    private String pendingGoal = "";
    private String pendingReward = "";
    
    // Free reward tab fields
    private EditBox freeRewardBox;
    private EditBox cooldownBox;
    private FreeRewardEditorWidget freeRewardEditorWidget;
    
    // Vision Pro Camera fields
    private boolean cameraLocked = true;
    
    public EconomyManagementScreen(EconomyManagementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 600, 450);
        this.imageHeight = 450;
        this.imageWidth = 600;
    }

    @Override
    protected boolean isFullscreenMode() { return isFullscreen; }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        
        // Clear all EditBox references when reinitializing
        searchBox = null;
        nodeEditor = null;
        freeRewardBox = null;
        cooldownBox = null;
        freeRewardEditorWidget = null;
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        if (!isFullscreen) {
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
            
            // Settings button
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 120, centerY + 10, 25, 20,
                Component.literal("⚙"),
                button -> switchTab(Tab.SETTINGS),
                currentTab == Tab.SETTINGS ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
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
        }
        
        // Load templates from client cache
        templates = new ArrayList<>(ClientPacketHandler.getCachedTemplates());
        
        if (currentTab == Tab.TASK_TEMPLATES) {
            initTaskTemplatesTab(centerX, centerY);
        } else if (currentTab == Tab.FREE_REWARD) {
            initFreeRewardTab(centerX, centerY);
        } else if (currentTab == Tab.STATISTICS) {
            initStatisticsTab(centerX, centerY);
        } else if (currentTab == Tab.SETTINGS) {
            initSettingsTab(centerX, centerY);
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
        // Node-based template editor widget
        int editorWidth = isFullscreen ? this.width : this.imageWidth - 40;
        int editorHeight = isFullscreen ? this.height : this.imageHeight - 160;
        int editorX = isFullscreen ? 0 : centerX + 20;
        int editorY = isFullscreen ? 0 : centerY + 110;
        
        nodeEditor = new NodeBasedTemplateEditorWidget(editorX, editorY, editorWidth, editorHeight);
        nodeEditor.setFullscreen(isFullscreen);
        nodeEditor.setOnCameraToggle(() -> {
            if (this.cameraLocked && this.minecraft != null) {
                this.cameraLocked = false;
                this.minecraft.mouseHandler.grabMouse();
            }
        });
        
        java.util.List<com.servermanagement.features.economy.TaskComponent> fallbackComps = new java.util.ArrayList<>();
        if (editTaskType != null) {
            int g = 1;
            try { g = Integer.parseInt(pendingGoal); } catch(Exception e) {}
            fallbackComps.add(new com.servermanagement.features.economy.TaskComponent(editTaskType, g, pendingDescription));
        }
        double rew = 0;
        try { rew = Double.parseDouble(pendingReward); } catch(Exception e) {}
        
        if (editTemplateId != null && !editTemplateId.isEmpty()) {
            SyncEconomyTemplatesPacket.TemplateData td = null;
            for(SyncEconomyTemplatesPacket.TemplateData t : templates) {
                if (t.id().equals(editTemplateId)) { td = t; break; }
            }
            if (td != null) {
                nodeEditor.setTemplateData(td.components(), td.rewardAmount(), td.rewardItems());
            } else {
                nodeEditor.setTemplateData(fallbackComps, rew, editRewardItems);
            }
        } else {
            nodeEditor.setTemplateData(fallbackComps, rew, editRewardItems);
        }
        this.addRenderableWidget(nodeEditor);
        
        // Reward widget moved into NodeBasedTemplateEditorWidget

        // Save / Cancel buttons below the reward widget
        int buttonY = isFullscreen ? this.height - 40 : editorY + editorHeight + 15;
        int buttonX = isFullscreen ? 20 : editorX;
        
        this.addRenderableWidget(new ModernButton(
            buttonX, buttonY, 120, 25,
            Component.literal("Save"),
            button -> saveTemplate(),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        this.addRenderableWidget(new ModernButton(
            buttonX + 130, buttonY, 120, 25,
            Component.literal("Cancel"),
            button -> cancelEdit(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        this.addRenderableWidget(new ModernButton(
            buttonX + 260, buttonY, 120, 25,
            Component.literal(isFullscreen ? "Exit Fullscreen" : "Fullscreen"),
            button -> {
                isFullscreen = !isFullscreen;
                this.rebuildWidgets();
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void initFreeRewardTab(int centerX, int centerY) {
        int formX = centerX + 50;
        int formY = centerY + 130;
        
        // Free reward amount
        if (freeRewardBox == null) {
            freeRewardBox = new EditBox(this.font, formX, formY, 150, 20, Component.literal("Free Reward Amount"));
            freeRewardBox.setMaxLength(10);
            freeRewardBox.setValue(String.valueOf(ClientPacketHandler.getCachedFreeRewardAmount()));
            freeRewardBox.setFilter(s -> s.matches("\\d*"));
        }
        this.addRenderableWidget(freeRewardBox);
        
        // Cooldown hours
        if (cooldownBox == null) {
            cooldownBox = new EditBox(this.font, formX + 160, formY, 150, 20, Component.literal("Cooldown (hours)"));
            cooldownBox.setMaxLength(3);
            cooldownBox.setValue(String.valueOf(ClientPacketHandler.getCachedFreeRewardCooldownHours()));
            cooldownBox.setFilter(s -> s.matches("\\d*"));
        }
        this.addRenderableWidget(cooldownBox);
        
        if (freeRewardEditorWidget == null) {
            freeRewardEditorWidget = new FreeRewardEditorWidget(formX, formY + 30, 310, 160, ClientPacketHandler.getCachedFreeRewardItems());
        }
        this.addRenderableWidget(freeRewardEditorWidget);
        
        // Save button (below item slot help text)
        this.addRenderableWidget(new ModernButton(
            formX, formY + 225, 100, 25,
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
        this.cameraLocked = true;
        if (this.minecraft != null && !this.minecraft.mouseHandler.isMouseGrabbed() == false) {
            this.minecraft.mouseHandler.releaseMouse();
        }
        this.templateRewardEditorWidget = null;
        this.selectedTemplateIndex = -1;
        this.rebuildWidgets();
    }
    
    private void createNewTemplate() {
        editMode = true;
        selectedTemplateIndex = -1;
        editTemplateId = null;
        editTaskType = TaskType.BREAK_BLOCKS;
        editRewardItems = new ArrayList<>();
        pendingDescription = "";
        pendingGoal = "";
        pendingReward = "";
        this.rebuildWidgets();
        // Tick the node editor once to initialize animation
        if (nodeEditor != null) nodeEditor.tick();
    }
    
    private void editTemplate(int index) {
        editMode = true;
        selectedTemplateIndex = index;
        SyncEconomyTemplatesPacket.TemplateData template = templates.get(index);
        editTemplateId = template.id();
        editTaskType = template.getTaskType();
        editRewardItems = template.rewardItems() != null ? new ArrayList<>(template.rewardItems()) : new ArrayList<>();
        pendingDescription = (!template.components().isEmpty() ? template.components().get(0).getCustomDescription() : "");
        pendingGoal = String.valueOf(!template.components().isEmpty() ? template.components().get(0).getTargetAmount() : 1);
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
                template.id(), template.components(), template.rewardAmount(), !template.enabled(),
                template.rewardItems()
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
        String description = "";
        int goal = 0;
        int reward = 0;
        TaskType taskType = editTaskType;
        java.util.List<com.servermanagement.features.economy.TaskComponent> components = new java.util.ArrayList<>();
        
        if (nodeEditor != null) {
            components = nodeEditor.getComponents();
            reward = (int) nodeEditor.getRewardAmount();
            if (!components.isEmpty()) {
                description = components.get(0).getCustomDescription();
                goal = components.get(0).getTargetAmount();
                taskType = components.get(0).getType();
            }
        }
        
        if (goal > 0 && reward > 0) {
            ModNetworking.sendToServer(new SaveTemplatePacket(
                editTemplateId, components, reward, templateRewardEditorWidget != null ? templateRewardEditorWidget.getRewardItems() : new ArrayList<>()
            ));
        }
        
        editMode = false;
        editTemplateId = null;
        this.rebuildWidgets();
    }
    
    private void cancelEdit() {
        editMode = false;
        cameraLocked = true;
        if (this.minecraft != null && !this.minecraft.mouseHandler.isMouseGrabbed() == false) {
            this.minecraft.mouseHandler.releaseMouse();
        }
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
            ModNetworking.sendToServer(new SaveFreeRewardSettingsPacket(rewardAmount, cooldownHours, freeRewardEditorWidget != null ? freeRewardEditorWidget.getRewardItems() : new java.util.ArrayList<>()));
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (isFullscreen) return;
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        if (!editMode || currentTab != Tab.TASK_TEMPLATES) {
            // Main background
            guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + this.imageHeight, 0xE0101010);
            
            // Header bar
            guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 40, 0xFF1A1A2E);
            guiGraphics.fill(centerX, centerY + 40, centerX + this.imageWidth, centerY + 41, 0xFF333333);
            
            // Tab content area
            guiGraphics.fill(centerX + 10, centerY + 80, centerX + this.imageWidth - 10, 
                centerY + this.imageHeight - 15, 0xE01A1A1A);
        } else {
            // Fullscreen edit mode: subtle translucent header only
            guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 40, 0xCC1A1A2E);
            guiGraphics.fill(centerX, centerY + 40, centerX + this.imageWidth, centerY + 41, 0xCC333333);
        }
        
        if (currentTab == Tab.TASK_TEMPLATES && !editMode) {
            // Render template boxes
            for (int i = 0; i < Math.min(3, templates.size() - scrollOffset); i++) {
                int templateIndex = i + scrollOffset;
                int yPos = centerY + 112 + (i * (TEMPLATE_HEIGHT + TEMPLATE_PADDING));
                
                // Template background
                guiGraphics.fill(centerX + 20, yPos, centerX + this.imageWidth - 20, 
                    yPos + TEMPLATE_HEIGHT, 0xE0252525);
            }
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        if (isFullscreen) return;
        
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
        } else if (currentTab == Tab.SETTINGS) {
            int startY = centerY + 90;
            int spacing = 35;
            guiGraphics.drawString(this.font, "Show Market Value Tooltips", centerX + 20, startY + 5, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Enable MineBay", centerX + 20, startY + spacing + 5, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Enable MineStacks", centerX + 20, startY + spacing * 2 + 5, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Starting Balance ($):", centerX + 20, startY + spacing * 3 + 5, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Trade Blacklist:", centerX + 20, startY + spacing * 4 + 2, 0xAAAAAA, true);
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
                    guiGraphics.drawString(this.font, Component.literal(template.getTaskType().getDisplayName() + ": " + (!template.components().isEmpty() ? template.components().get(0).getCustomDescription() : "")),
                        centerX + 60, yPos + 5, 0xFFFFFF, true);
                    
                    // Goal and reward
                    guiGraphics.drawString(this.font, Component.literal("Goal: " + (!template.components().isEmpty() ? template.components().get(0).getTargetAmount() : 1)),
                        centerX + 30, yPos + 18, 0xCCCCCC, true);
                    
                    guiGraphics.drawString(this.font, Component.literal("Reward: $" + template.rewardAmount()),
                        centerX + 30, yPos + 31, 0x55FF55, true);
                    
                    // Show reward items if set
                    if (template.rewardItems() != null && !template.rewardItems().isEmpty()) {
                        int itemX = centerX + 160;
                        ItemStack firstItem = template.rewardItems().get(0);
                        guiGraphics.renderItem(firstItem, itemX, yPos + 27);
                        
                        String text = "+ " + firstItem.getHoverName().getString();
                        if (template.rewardItems().size() > 1) {
                            text += " (+" + (template.rewardItems().size() - 1) + " more)";
                        }
                        
                        guiGraphics.drawString(this.font, 
                            Component.literal(text),
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
            // Render node-based editor title
            guiGraphics.drawString(this.font, Component.literal(selectedTemplateIndex == -1 ? "Create New Template" : "Edit Template"),
                centerX + 35, centerY + 90, 0xFFD700, true);
            
            // Node editor widget renders itself via addRenderableWidget

        }
    }
    
    private void renderFreeRewardTab(GuiGraphics guiGraphics, int centerX, int centerY) {
        int formY = centerY + 130;
        
        guiGraphics.drawString(this.font, Component.literal("Configure Free Daily Reward"),
            centerX + 55, centerY + 95, 0xFFAA00, true);
        
        guiGraphics.drawString(this.font, Component.literal("Reward Amount ($):"),
            centerX + 55, formY - 15, 0xFFFFFF, true);
        
        guiGraphics.drawString(this.font, Component.literal("Cooldown (hours):"),
            centerX + 215, formY - 15, 0xFFFFFF, true);
        
        guiGraphics.drawString(this.font, Component.literal("Tip: Players can claim this reward once per cooldown period"),
            centerX + 55, formY + 255, 0x888888, true);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC while editing: step back instead of closing the screen
        if (keyCode == 256 && editMode) { // GLFW_KEY_ESCAPE = 256
            if (isFullscreen) {
                // Step 1: exit fullscreen back to windowed editor
                isFullscreen = false;
                this.rebuildWidgets();
            } else {
                // Step 2: cancel edit back to template list
                cancelEdit();
            }
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (this.searchBox != null && this.searchBox.isFocused()) return true;
            if (this.freeRewardBox != null && this.freeRewardBox.isFocused()) return true;
            if (this.cooldownBox != null && this.cooldownBox.isFocused()) return true;
            if (this.blacklistWidget != null && this.blacklistWidget.isSearchBoxFocused()) return true;
            if (this.nodeEditor != null && this.nodeEditor.isAnyTextFieldFocused()) return true;
            if (this.freeRewardEditorWidget != null && this.freeRewardEditorWidget.isSearchBoxFocused()) return true;
        }

        if (keyCode != 256 && this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!cameraLocked && button == 1) {
            this.cameraLocked = true;
            if (this.minecraft != null) {
                this.minecraft.mouseHandler.releaseMouse();
            }
            return true;
        }

        // Convert raw screen-pixel coords to design-space because the screen
        // is rendered through ScalableContainerScreen's pose scale. Without
        // this the custom slot hit-tests below silently miss at non-1.0 GUI
        // scales (Auto / Scale 4 / Scale 5).
        double designMouseX = inverseMouseX(mouseX);
        double designMouseY = inverseMouseY(mouseY);

        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Handle item slot clicks for template editing
        if (editMode && currentTab == Tab.TASK_TEMPLATES) {
            int itemSlotX = centerX + 35;
            int itemSlotY = centerY + 110 + 150 + 50; // Below save/cancel buttons, matching render
            
            if (designMouseX >= itemSlotX && designMouseX < itemSlotX + 18 && 
                designMouseY >= itemSlotY && designMouseY < itemSlotY + 18) {
                handleItemSlotClick(true);
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
                    
                }
            } else {
                // Clear item when clicking with empty hand
                if (isTemplateEdit) {
                    editRewardItems = new ArrayList<>();
                }
            }
        }
    }

    private com.servermanagement.gui.widgets.ToggleSwitch tooltipSwitch;
    private com.servermanagement.gui.widgets.ToggleSwitch minebaySwitch;
    private com.servermanagement.gui.widgets.ToggleSwitch minestacksSwitch;
    private EditBox startingBalanceBox;
    private com.servermanagement.gui.widgets.TradeBlacklistEditorWidget blacklistWidget;

    private void initSettingsTab(int centerX, int centerY) {
        int startY = centerY + 90;
        int spacing = 35;
        
        int rightCol = centerX + this.imageWidth - 70;

        tooltipSwitch = new com.servermanagement.gui.widgets.ToggleSwitch(
            rightCol, startY,
            Component.literal("Tooltips"),
            com.servermanagement.client.ClientPacketHandler.showMarketValueTooltips(),
            (newState) -> {}
        );
        this.addRenderableWidget(tooltipSwitch);

        minebaySwitch = new com.servermanagement.gui.widgets.ToggleSwitch(
            rightCol, startY + spacing,
            Component.literal("MineBay"),
            com.servermanagement.client.ClientPacketHandler.minebayEnabled(),
            (newState) -> {}
        );
        this.addRenderableWidget(minebaySwitch);

        minestacksSwitch = new com.servermanagement.gui.widgets.ToggleSwitch(
            rightCol, startY + spacing * 2,
            Component.literal("MineStacks"),
            com.servermanagement.client.ClientPacketHandler.minestacksEnabled(),
            (newState) -> {}
        );
        this.addRenderableWidget(minestacksSwitch);

        startingBalanceBox = new EditBox(this.font, rightCol - 50, startY + spacing * 3, 100, 20, Component.literal("Starting Balance"));
        startingBalanceBox.setMaxLength(15);
        startingBalanceBox.setFilter(s -> s.matches("\\d*(\\.\\d*)?")); // Allow numbers and decimals
        startingBalanceBox.setValue(String.valueOf(com.servermanagement.client.ClientPacketHandler.getStartingBalance()));
        this.addRenderableWidget(startingBalanceBox);

        // Initialize TradeBlacklistEditorWidget below the toggles
        blacklistWidget = new com.servermanagement.gui.widgets.TradeBlacklistEditorWidget(
            centerX + 20, startY + spacing * 4 + 15, this.imageWidth - 40, 100,
            com.servermanagement.client.ClientPacketHandler.getTradeBlacklist()
        );
        this.addRenderableWidget(blacklistWidget);

        this.addRenderableWidget(new ModernButton(
            centerX + (this.imageWidth / 2) - 60, centerY + this.imageHeight - 45, 120, 25,
            Component.literal("Save Settings"),
            button -> {
                double startingBalance = 1000.0;
                try {
                    startingBalance = startingBalanceBox.getValue().isEmpty() ? 0.0 : Double.parseDouble(startingBalanceBox.getValue());
                } catch (NumberFormatException ignored) {}
                com.servermanagement.network.ModNetworking.sendToServer(new com.servermanagement.network.packet.SaveEconomySettingsPacket(
                    tooltipSwitch.isToggled(),
                    minebaySwitch.isToggled(),
                    minestacksSwitch.isToggled(),
                    blacklistWidget.getBlacklistString(),
                    startingBalance
                ));
            },
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    @Override
    public void containerTick() {
        super.containerTick();
        if (currentTab == Tab.SETTINGS && blacklistWidget != null) blacklistWidget.tick();
    }
}
