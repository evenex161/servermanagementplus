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
    
    // Unsaved changes state
    private Runnable pendingNavigation = null;
    private boolean showingUnsavedDialog = false;


    public EconomyManagementScreen(EconomyManagementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 600, 450);
        this.imageHeight = 450;
        this.imageWidth = 600;
    }

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
                button -> handleNavigation(() -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD))),
                ModernButton.ButtonStyle.SECONDARY
            ));
            
            // Close button
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 90, centerY + 10, 80, 20,
                Component.literal("Close"),
                button -> handleNavigation(() -> super.onClose()),
                ModernButton.ButtonStyle.DANGER
            ));
            
            // Settings button
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 120, centerY + 10, 25, 20,
                Component.literal("⚙"),
                button -> { if (currentTab != Tab.SETTINGS) handleNavigation(() -> switchTab(Tab.SETTINGS)); },
                currentTab == Tab.SETTINGS ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
            ));
            
            // Tab buttons
            int tabW = (this.imageWidth - 40) / 3;
            this.addRenderableWidget(new ModernButton(
                centerX + 10, centerY + 50, tabW, 25,
                Component.literal("Task Templates"),
                button -> { if (currentTab != Tab.TASK_TEMPLATES) handleNavigation(() -> switchTab(Tab.TASK_TEMPLATES)); },
                currentTab == Tab.TASK_TEMPLATES ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
            ));
            
            this.addRenderableWidget(new ModernButton(
                centerX + 10 + tabW + 5, centerY + 50, tabW, 25,
                Component.literal("Free Reward"),
                button -> { if (currentTab != Tab.FREE_REWARD) handleNavigation(() -> switchTab(Tab.FREE_REWARD)); },
                currentTab == Tab.FREE_REWARD ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
            ));
            
            this.addRenderableWidget(new ModernButton(
                centerX + 10 + (tabW + 5) * 2, centerY + 50, tabW, 25,
                Component.literal("Statistics"),
                button -> { if (currentTab != Tab.STATISTICS) handleNavigation(() -> switchTab(Tab.STATISTICS)); },
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
                searchBox.setResponder(query -> {
                    String q = query.toLowerCase();
                    templates = com.servermanagement.client.ClientPacketHandler.getCachedTemplates().stream()
                        .filter(t -> t.getTaskType().getDisplayName().toLowerCase().contains(q) || 
                                     (t.description().toLowerCase().contains(q)))
                        .toList();
                    scrollOffset = 0;
                });
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
        int editorX = isFullscreen ? (int) inverseMouseX(0) : centerX + 20;
        int editorY = isFullscreen ? (int) inverseMouseY(0) : centerY + 110;
        int editorWidth = isFullscreen ? (int) (this.width / getGuiScale()) : this.imageWidth - 40;
        int editorHeight = isFullscreen ? (int) (this.height / getGuiScale()) - 60 : this.imageHeight - 160;
        
        nodeEditor = new NodeBasedTemplateEditorWidget(editorX, editorY, editorWidth, editorHeight);
        nodeEditor.setFullscreen(isFullscreen);

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
                nodeEditor.setTemplateData(java.util.List.of(), td.rewardAmount(), td.rewardItem() != null && !td.rewardItem().isEmpty() ? java.util.List.of(td.rewardItem()) : new java.util.ArrayList<>());
            } else {
                nodeEditor.setTemplateData(fallbackComps, rew, editRewardItems);
            }
        } else {
            nodeEditor.setTemplateData(fallbackComps, rew, editRewardItems);
        }
        this.addRenderableWidget(nodeEditor);
        
        // Reward widget moved into NodeBasedTemplateEditorWidget

        // Save / Cancel buttons below the reward widget
        int buttonY = isFullscreen ? (int) inverseMouseY(this.height) - 40 : editorY + editorHeight + 15;
        int buttonX = isFullscreen ? (int) inverseMouseX(0) + 20 : editorX;
        
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
            freeRewardEditorWidget = new FreeRewardEditorWidget(formX, formY + 30, 310, 160, new java.util.ArrayList<>());
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
        this.isFullscreen = false;

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
        editRewardItems = (template.rewardItem() != null && !template.rewardItem().isEmpty() ? java.util.List.of(template.rewardItem()) : new java.util.ArrayList<>()) != null ? new ArrayList<>((template.rewardItem() != null && !template.rewardItem().isEmpty() ? java.util.List.of(template.rewardItem()) : new java.util.ArrayList<>())) : new ArrayList<>();
        pendingDescription = (template.description());
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
                template.id(), template.taskTypeOrdinal(), template.description(), template.goal(), template.rewardAmount(), !template.enabled(),
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
        
        java.util.List<ItemStack> rewardItems = nodeEditor != null ? nodeEditor.getRewardItems() : new ArrayList<>();
        
        if (goal > 0 && (reward > 0 || !rewardItems.isEmpty())) {
            ItemStack singleReward = rewardItems.isEmpty() ? ItemStack.EMPTY : rewardItems.get(0);
            ModNetworking.sendToServer(new SaveTemplatePacket(
                editTemplateId, taskType, description, goal, reward, singleReward
            ));
        }
        
        editMode = false;
        isFullscreen = false;
        editTemplateId = null;
        this.rebuildWidgets();
    }
    
    private void cancelEdit() {
        editMode = false;
        isFullscreen = false;

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
            java.util.List<ItemStack> items = freeRewardEditorWidget != null ? freeRewardEditorWidget.getRewardItems() : new java.util.ArrayList<>();
            ItemStack singleItem = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
            ModNetworking.sendToServer(new SaveFreeRewardSettingsPacket(rewardAmount, cooldownHours, singleItem));
        }
    }
    
    private void saveSettings() {
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
    }
    
    private boolean hasUnsavedChanges() {
        if (currentTab == Tab.TASK_TEMPLATES && editMode && nodeEditor != null) {
            java.util.List<com.servermanagement.features.economy.TaskComponent> currentComps = nodeEditor.getComponents();
            double currentReward = nodeEditor.getRewardAmount();
            java.util.List<ItemStack> currentRewardItems = nodeEditor.getRewardItems();
            
            if (editTemplateId == null) {
                return !currentComps.isEmpty() || currentReward > 0 || !currentRewardItems.isEmpty();
            } else {
                SyncEconomyTemplatesPacket.TemplateData td = null;
                for(SyncEconomyTemplatesPacket.TemplateData t : templates) {
                    if (t.id().equals(editTemplateId)) { td = t; break; }
                }
                if (td != null) {
                    if (currentReward != td.rewardAmount()) return true;
                    if ((currentRewardItems == null ? 0 : currentRewardItems.size()) != (td.rewardItem() == null || td.rewardItem().isEmpty() ? 0 : 1)) return true;
                    if (currentComps.size() != 1) return true;
                    if (!currentComps.isEmpty()) {
                        if (!currentComps.get(0).getCustomDescription().equals(td.description())) return true;
                        if (currentComps.get(0).getTargetAmount() != td.goal()) return true;
                        if (currentComps.get(0).getType() != td.getTaskType()) return true;
                    }
                }
            }
        } else if (currentTab == Tab.FREE_REWARD && freeRewardBox != null && cooldownBox != null && freeRewardEditorWidget != null) {
            int cachedReward = com.servermanagement.client.ClientPacketHandler.getCachedFreeRewardAmount();
            int cachedCooldown = com.servermanagement.client.ClientPacketHandler.getCachedFreeRewardCooldownHours();
            int currentReward = freeRewardBox.getValue().isEmpty() ? 0 : Integer.parseInt(freeRewardBox.getValue());
            int currentCooldown = cooldownBox.getValue().isEmpty() ? 0 : Integer.parseInt(cooldownBox.getValue());
            if (currentReward != cachedReward || currentCooldown != cachedCooldown) return true;
            if (freeRewardEditorWidget.getRewardItems().size() != 0) return true;
        } else if (currentTab == Tab.SETTINGS && tooltipSwitch != null && minebaySwitch != null && minestacksSwitch != null && startingBalanceBox != null && blacklistWidget != null) {
            if (tooltipSwitch.isToggled() != com.servermanagement.client.ClientPacketHandler.showMarketValueTooltips()) return true;
            if (minebaySwitch.isToggled() != com.servermanagement.client.ClientPacketHandler.minebayEnabled()) return true;
            if (minestacksSwitch.isToggled() != com.servermanagement.client.ClientPacketHandler.minestacksEnabled()) return true;
            double currentBalance = startingBalanceBox.getValue().isEmpty() ? 0.0 : Double.parseDouble(startingBalanceBox.getValue());
            if (currentBalance != com.servermanagement.client.ClientPacketHandler.getStartingBalance()) return true;
            if (!blacklistWidget.getBlacklistString().equals(com.servermanagement.client.ClientPacketHandler.getTradeBlacklist())) return true;
        }
        return false;
    }

    private void handleNavigation(Runnable action) {
        if (!showingUnsavedDialog && hasUnsavedChanges()) {
            showingUnsavedDialog = true;
            pendingNavigation = action;
        } else {
            action.run();
        }
    }

    @Override
    public void onClose() {
        if (showingUnsavedDialog) {
            super.onClose();
            return;
        }
        handleNavigation(() -> super.onClose());
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
        
        if (showingUnsavedDialog) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500); // Force overlay above all other widgets including EditBox
            
            // Dim background
            fillScreen(guiGraphics, 0xAA000000);
            
            // Dialog box
            int boxW = 280;
            int boxH = 100;
            int boxX = (this.width - boxW) / 2;
            int boxY = (this.height - boxH) / 2;
            
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF222222);
            guiGraphics.renderOutline(boxX, boxY, boxW, boxH, 0xFFFFAA00);
            
            Component title = Component.literal("Unsaved Changes");
            guiGraphics.drawString(this.font, title, boxX + (boxW - this.font.width(title)) / 2, boxY + 15, 0xFFFFAA00, true);
            
            Component msg = Component.literal("Do you want to apply your unsaved changes?");
            guiGraphics.drawString(this.font, msg, boxX + (boxW - this.font.width(msg)) / 2, boxY + 40, 0xFFFFFF, true);
            
            // Note: The buttons for the dialog are rendered below by manually drawing them since they are dynamic
            // But to use standard hit testing, it's easier to just draw rectangles and check mouse bounds in mouseClicked
            int btnY = boxY + 65;
            // Yes Button
            guiGraphics.fill(boxX + 20, btnY, boxX + 90, btnY + 20, 0xFF28a745); // Green
            guiGraphics.drawString(this.font, "Yes", boxX + 55 - this.font.width("Yes") / 2, btnY + 6, 0xFFFFFF, true);
            
            // No Button
            guiGraphics.fill(boxX + 105, btnY, boxX + 175, btnY + 20, 0xFFdc3545); // Red
            guiGraphics.drawString(this.font, "No", boxX + 140 - this.font.width("No") / 2, btnY + 6, 0xFFFFFF, true);
            
            // Cancel Button
            guiGraphics.fill(boxX + 190, btnY, boxX + 260, btnY + 20, 0xFF6c757d); // Gray
            guiGraphics.drawString(this.font, "Cancel", boxX + 225 - this.font.width("Cancel") / 2, btnY + 6, 0xFFFFFF, true);
            
            guiGraphics.pose().popPose();
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
                    guiGraphics.drawString(this.font, Component.literal(template.getTaskType().getDisplayName() + ": " + (template.description())),
                        centerX + 60, yPos + 5, 0xFFFFFF, true);
                    
                    // Goal and reward
                    guiGraphics.drawString(this.font, Component.literal("Goal: " + (template.goal())),
                        centerX + 30, yPos + 18, 0xCCCCCC, true);
                    
                    guiGraphics.drawString(this.font, Component.literal("Reward: $" + template.rewardAmount()),
                        centerX + 30, yPos + 31, 0x55FF55, true);
                    
                    // Show reward items if set
                    if (template.rewardItem() != null && !template.rewardItem().isEmpty()) {
                        int itemX = centerX + 160;
                        ItemStack firstItem = template.rewardItem();
                        guiGraphics.renderItem(firstItem, itemX, yPos + 27);
                        
                        String text = "+ " + firstItem.getHoverName().getString();
                        if (false) {
                            text += " (+" + ((template.rewardItem() != null && !template.rewardItem().isEmpty() ? java.util.List.of(template.rewardItem()) : new java.util.ArrayList<>()).size() - 1) + " more)";
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
        if (showingUnsavedDialog) return true; // Block keyboard inputs while dialog is open

        // ESC while editing: step back instead of closing the screen
        if (keyCode == 256 && editMode) { // GLFW_KEY_ESCAPE = 256
            if (isFullscreen) {
                // Step 1: exit fullscreen back to windowed editor
                isFullscreen = false;
                this.rebuildWidgets();
            } else {
                // Step 2: cancel edit back to template list
                handleNavigation(() -> cancelEdit());
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (currentTab == Tab.TASK_TEMPLATES && !editMode) {
            if (scrollY > 0 && scrollOffset > 0) {
                scrollOffset--;
                this.rebuildWidgets();
                return true;
            } else if (scrollY < 0 && scrollOffset + 3 < templates.size()) {
                scrollOffset++;
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
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
        // Handle unsaved changes dialog clicks first (these are in raw coords, not design-space, if rendering happened outside scaled pose, but renderContent is inside scaled pose! Wait, renderContent scales, so this must be design coords.)
        double designMouseX = inverseMouseX(mouseX);
        double designMouseY = inverseMouseY(mouseY);
        
        if (showingUnsavedDialog) {
            int boxW = 280;
            int boxH = 100;
            int boxX = (this.width - boxW) / 2;
            int boxY = (this.height - boxH) / 2;
            int btnY = boxY + 65;
            
            // Yes button (boxX + 20 to boxX + 90)
            if (designMouseX >= boxX + 20 && designMouseX <= boxX + 90 && designMouseY >= btnY && designMouseY <= btnY + 20) {
                if (currentTab == Tab.TASK_TEMPLATES) saveTemplate();
                else if (currentTab == Tab.FREE_REWARD) saveFreeRewardSettings(freeRewardBox.getValue(), cooldownBox.getValue());
                else if (currentTab == Tab.SETTINGS) saveSettings();
                
                showingUnsavedDialog = false;
                if (pendingNavigation != null) pendingNavigation.run();
                return true;
            }
            // No button (boxX + 105 to boxX + 175)
            if (designMouseX >= boxX + 105 && designMouseX <= boxX + 175 && designMouseY >= btnY && designMouseY <= btnY + 20) {
                showingUnsavedDialog = false;
                if (pendingNavigation != null) pendingNavigation.run();
                return true;
            }
            // Cancel button (boxX + 190 to boxX + 260)
            if (designMouseX >= boxX + 190 && designMouseX <= boxX + 260 && designMouseY >= btnY && designMouseY <= btnY + 20) {
                showingUnsavedDialog = false;
                pendingNavigation = null;
                return true;
            }
            return true; // Swallow all other clicks
        }

        if (searchBox != null && searchBox.isFocused() &&
            !(designMouseX >= searchBox.getX() && designMouseX < searchBox.getX() + searchBox.getWidth() &&
              designMouseY >= searchBox.getY() && designMouseY < searchBox.getY() + searchBox.getHeight())) {
            searchBox.setFocused(false);
        }

        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        

        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (editMode && currentTab == Tab.TASK_TEMPLATES && nodeEditor != null) {
            double designMouseX = inverseMouseX(mouseX);
            double designMouseY = inverseMouseY(mouseY);
            if (nodeEditor.mouseDragged(designMouseX, designMouseY, button, dragX / getGuiScale(), dragY / getGuiScale())) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
            button -> saveSettings(),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    @Override
    public void containerTick() {
        super.containerTick();
        if (currentTab == Tab.FREE_REWARD && freeRewardEditorWidget != null) freeRewardEditorWidget.tick();
        if (currentTab == Tab.SETTINGS && blacklistWidget != null) blacklistWidget.tick();
    }
}
