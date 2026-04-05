package com.servermanagement.gui.screen;

import com.servermanagement.gui.menu.PerformanceSettingsMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.UpdatePerformanceSettingPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PerformanceSettingsScreen extends AbstractContainerScreen<PerformanceSettingsMenu> {

    // Scroll state
    private int scrollOffset = 0;
    private static final int MAX_SCROLL = 260;
    private static final int SCROLL_STEP = 16;

    // Current page (0 = toggles, 1 = tunables)
    private int currentPage = 0;

    public PerformanceSettingsScreen(PerformanceSettingsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 380;
        this.imageHeight = 340;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        int cX = this.leftPos;
        int cY = this.topPos;

        // --- TPS status bar at top ---
        // (rendered in render(), not a widget)

        // --- Page tabs ---
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Toggles"),
            btn -> { currentPage = 0; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 10, cY + 42, 80, 20)
            .style(currentPage == 0 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Settings"),
            btn -> { currentPage = 1; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 95, cY + 42, 80, 20)
            .style(currentPage == 1 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Stats"),
            btn -> { currentPage = 2; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 180, cY + 42, 80, 20)
            .style(currentPage == 2 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        int contentY = cY + 70 - scrollOffset;
        int rightCol = cX + 290;
        int spacing = 30;

        if (currentPage == 0) {
            buildTogglesPage(cX, contentY, rightCol, spacing);
        } else if (currentPage == 1) {
            buildSettingsPage(cX, contentY, spacing);
        }
        // Page 2 (Stats) is rendered in render() method only

        // --- Bottom buttons ---
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("\u2190 Dashboard"),
            btn -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(cX + 10, cY + this.imageHeight - 30, 110, 22)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            btn -> this.onClose())
            .bounds(cX + this.imageWidth - 80, cY + this.imageHeight - 30, 70, 22)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }

    private void buildTogglesPage(int cX, int contentY, int rightCol, int spacing) {
        int row = 0;

        // Master toggle
        addToggle(rightCol, contentY + spacing * row, "feature_enabled",
            this.menu.isFeatureEnabled(), v -> this.menu.setFeatureEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "auto_optimize",
            this.menu.isTpsAutoOptimize(), v -> this.menu.setTpsAutoOptimize(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "item_merging",
            this.menu.isItemMergingEnabled(), v -> this.menu.setItemMergingEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "mob_spawn_limiter",
            this.menu.isMobSpawnLimiterEnabled(), v -> this.menu.setMobSpawnLimiterEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "entity_activation_range",
            this.menu.isEntityActivationRangeEnabled(), v -> this.menu.setEntityActivationRangeEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "villager_throttle",
            this.menu.isVillagerThrottleEnabled(), v -> this.menu.setVillagerThrottleEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "redstone_throttle",
            this.menu.isRedstoneThrottleEnabled(), v -> this.menu.setRedstoneThrottleEnabled(v));
        row++;

        addToggle(rightCol, contentY + spacing * row, "tps_monitor",
            this.menu.isTpsMonitorEnabled(), v -> this.menu.setTpsMonitorEnabled(v));
    }

    private void addToggle(int x, int y, String key, boolean initialState, java.util.function.Consumer<Boolean> setter) {
        this.addRenderableWidget(new ToggleSwitch(
            x, y, Component.literal(key),
            initialState,
            newState -> {
                setter.accept(newState);
                long tick = minecraft.player != null ? minecraft.player.tickCount : 0;
                ModNetworking.sendToServer(new UpdatePerformanceSettingPacket(key, String.valueOf(newState), tick));
            }
        ));
    }

    private void buildSettingsPage(int cX, int contentY, int spacing) {
        int btnWidth = 30;
        int row = 0;

        // Item Merge Radius
        addValueButtons(cX + 260, contentY + spacing * row, "item_merge_radius",
            this.menu.getItemMergeRadius(), 0.5, 1.0, 10.0, true);
        row++;

        // Item Merge Interval
        addValueButtons(cX + 260, contentY + spacing * row, "item_merge_interval",
            this.menu.getItemMergeInterval(), 10, 10, 200, false);
        row++;

        // Mob Cap Multiplier
        addValueButtons(cX + 260, contentY + spacing * row, "mob_cap_multiplier",
            this.menu.getMobCapMultiplier(), 5, 10, 100, false);
        row++;

        // Monster Activation Range
        addValueButtons(cX + 260, contentY + spacing * row, "monster_activation_range",
            this.menu.getMonsterActivationRange(), 4, 8, 128, false);
        row++;

        // Animal Activation Range
        addValueButtons(cX + 260, contentY + spacing * row, "animal_activation_range",
            this.menu.getAnimalActivationRange(), 4, 8, 128, false);
        row++;

        // Misc Activation Range
        addValueButtons(cX + 260, contentY + spacing * row, "misc_activation_range",
            this.menu.getMiscActivationRange(), 2, 4, 64, false);
        row++;

        // Villager Tick Interval
        addValueButtons(cX + 260, contentY + spacing * row, "villager_tick_interval",
            this.menu.getVillagerTickInterval(), 1, 1, 10, false);
        row++;

        // Redstone Updates Per Tick
        addValueButtons(cX + 260, contentY + spacing * row, "redstone_updates_per_tick",
            this.menu.getRedstoneUpdatesPerTick(), 100, 100, 100000, false);
        row++;

        // TPS Warning Threshold
        addValueButtons(cX + 260, contentY + spacing * row, "tps_warning_threshold",
            this.menu.getTpsWarningThreshold(), 0.5, 5.0, 20.0, true);
        row++;

        // TPS Critical Threshold
        addValueButtons(cX + 260, contentY + spacing * row, "tps_critical_threshold",
            this.menu.getTpsCriticalThreshold(), 0.5, 5.0, 20.0, true);
    }

    private void addValueButtons(int x, int y, String key, double currentValue,
                                  double step, double min, double max, boolean isDouble) {
        // Minus button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("-"),
            btn -> {
                double newVal = Math.max(min, currentValue - step);
                updateNumericSetting(key, newVal, isDouble);
            })
            .bounds(x, y, 25, 18)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());

        // Plus button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("+"),
            btn -> {
                double newVal = Math.min(max, currentValue + step);
                updateNumericSetting(key, newVal, isDouble);
            })
            .bounds(x + 75, y, 25, 18)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }

    private void updateNumericSetting(String key, double newVal, boolean isDouble) {
        long tick = minecraft.player != null ? minecraft.player.tickCount : 0;
        String valStr = isDouble ? String.valueOf(newVal) : String.valueOf((int) newVal);
        ModNetworking.sendToServer(new UpdatePerformanceSettingPacket(key, valStr, tick));

        // Update local menu state and rebuild
        applyLocalMenuUpdate(key, newVal);
        rebuildWidgets();
    }

    private void applyLocalMenuUpdate(String key, double val) {
        switch (key) {
            case "item_merge_radius" -> this.menu.setItemMergeRadius(val);
            case "item_merge_interval" -> this.menu.setItemMergeInterval((int) val);
            case "mob_cap_multiplier" -> this.menu.setMobCapMultiplier((int) val);
            case "monster_activation_range" -> this.menu.setMonsterActivationRange((int) val);
            case "animal_activation_range" -> this.menu.setAnimalActivationRange((int) val);
            case "misc_activation_range" -> this.menu.setMiscActivationRange((int) val);
            case "villager_tick_interval" -> this.menu.setVillagerTickInterval((int) val);
            case "redstone_updates_per_tick" -> this.menu.setRedstoneUpdatesPerTick((int) val);
            case "tps_warning_threshold" -> this.menu.setTpsWarningThreshold(val);
            case "tps_critical_threshold" -> this.menu.setTpsCriticalThreshold(val);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = (int) Math.max(0, Math.min(MAX_SCROLL, scrollOffset - scrollY * SCROLL_STEP));
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.renderBg(g, partialTick, mouseX, mouseY);

        int cX = this.leftPos;
        int cY = this.topPos;

        // Header
        g.drawString(this.font, "Server Performance", cX + 15, cY + 8, 0xFFD700, true);

        // TPS indicator in header
        double tps = this.menu.getCurrentTps();
        int tpsColor = tps >= 18.0 ? 0x27AE60 : (tps >= 15.0 ? 0xE67E22 : 0xE74C3C);
        String tpsStr = String.format("TPS: %.1f", tps);
        g.drawString(this.font, tpsStr, cX + this.imageWidth - this.font.width(tpsStr) - 15, cY + 8, tpsColor, true);

        String msptStr = String.format("MSPT: %.1fms", this.menu.getAverageMspt());
        g.drawString(this.font, msptStr, cX + this.imageWidth - this.font.width(msptStr) - 15, cY + 20, 0xAAAAAA, true);

        if (this.menu.isAutoOptimizeActive()) {
            g.drawString(this.font, "AUTO-OPTIMIZE ACTIVE", cX + 15, cY + 20, 0xE67E22, true);
        } else {
            String sub = this.menu.isFeatureEnabled() ? "Manage optimization subsystems" : "Feature disabled";
            g.drawString(this.font, sub, cX + 15, cY + 20, 0xAAAAAA, true);
        }

        // Content area
        int contentY = cY + 70 - scrollOffset;
        int spacing = 30;

        if (currentPage == 0) {
            renderTogglesLabels(g, cX, contentY, spacing);
        } else if (currentPage == 1) {
            renderSettingsLabels(g, cX, contentY, spacing);
        } else if (currentPage == 2) {
            renderStatsPage(g, cX, cY + 70);
        }

        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private void renderTogglesLabels(GuiGraphics g, int cX, int contentY, int spacing) {
        int row = 0;
        g.drawString(this.font, "Server Performance:", cX + 20, contentY + spacing * row + 5, 0xFFFF55, false);
        row++;
        g.drawString(this.font, "Auto-Optimize:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "Item Merging:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "Mob Spawn Limiter:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "Entity Activation Range:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "Villager Throttle:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "Redstone Throttle:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
        row++;
        g.drawString(this.font, "TPS Monitor:", cX + 20, contentY + spacing * row + 5, 0xFFFFFF, false);
    }

    private void renderSettingsLabels(GuiGraphics g, int cX, int contentY, int spacing) {
        String[][] labels = {
            {"Item Merge Radius:", String.format("%.1f", this.menu.getItemMergeRadius())},
            {"Merge Interval (ticks):", String.valueOf(this.menu.getItemMergeInterval())},
            {"Mob Cap Multiplier %:", String.valueOf(this.menu.getMobCapMultiplier())},
            {"Monster Act. Range:", String.valueOf(this.menu.getMonsterActivationRange())},
            {"Animal Act. Range:", String.valueOf(this.menu.getAnimalActivationRange())},
            {"Misc Act. Range:", String.valueOf(this.menu.getMiscActivationRange())},
            {"Villager Tick Interval:", String.valueOf(this.menu.getVillagerTickInterval())},
            {"Redstone Upd/Tick:", String.valueOf(this.menu.getRedstoneUpdatesPerTick())},
            {"TPS Warn Threshold:", String.format("%.1f", this.menu.getTpsWarningThreshold())},
            {"TPS Critical Threshold:", String.format("%.1f", this.menu.getTpsCriticalThreshold())},
        };

        for (int i = 0; i < labels.length; i++) {
            int y = contentY + spacing * i + 4;
            g.drawString(this.font, labels[i][0], cX + 20, y, 0xFFFFFF, false);
            // Value display between - and + buttons
            String valStr = labels[i][1];
            g.drawString(this.font, valStr, cX + 260 + 28, y, 0x55FF55, false);
        }
    }

    private void renderStatsPage(GuiGraphics g, int cX, int startY) {
        int y = startY;
        int spacing = 22;

        g.drawString(this.font, "\u00a76=== Performance Statistics ===", cX + 20, y, 0xFFFFFF, false);
        y += spacing;

        double tps = this.menu.getCurrentTps();
        int tpsColor = tps >= 18.0 ? 0x27AE60 : (tps >= 15.0 ? 0xE67E22 : 0xE74C3C);
        g.drawString(this.font, String.format("Current TPS: %.1f", tps), cX + 20, y, tpsColor, false);
        y += spacing;

        g.drawString(this.font, String.format("Average MSPT: %.1fms", this.menu.getAverageMspt()), cX + 20, y, 0xFFFFFF, false);
        y += spacing;

        String autoOptStatus = this.menu.isAutoOptimizeActive() ? "\u00a7eACTIVE" : (this.menu.isTpsAutoOptimize() ? "\u00a7aStandby" : "\u00a7cOff");
        g.drawString(this.font, "Auto-Optimize: " + autoOptStatus, cX + 20, y, 0xFFFFFF, false);
        y += spacing + 5;

        g.drawString(this.font, "\u00a77--- Cumulative Stats ---", cX + 20, y, 0xFFFFFF, false);
        y += spacing;

        g.drawString(this.font, "Items Merged: " + this.menu.getTotalItemsMerged(), cX + 20, y, 0xAAAAAA, false);
        y += spacing;

        g.drawString(this.font, "Spawns Cancelled: " + this.menu.getTotalSpawnsCancelled(), cX + 20, y, 0xAAAAAA, false);
        y += spacing;

        g.drawString(this.font, "Entities Throttled: " + this.menu.getTotalEntitiesThrottled(), cX + 20, y, 0xAAAAAA, false);
        y += spacing;

        g.drawString(this.font, "Redstone Throttled: " + this.menu.getTotalRedstoneThrottled(), cX + 20, y, 0xAAAAAA, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Main background
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
            this.topPos + this.imageHeight, 0xE0101010);
        // Header bar
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
            this.topPos + 35, 0xFF1A1A2E);
        g.fill(this.leftPos, this.topPos + 35, this.leftPos + this.imageWidth,
            this.topPos + 36, 0xFF333333);

        // TPS bar under header
        double tps = this.menu.getCurrentTps();
        int barWidth = (int) (Math.min(tps / 20.0, 1.0) * (this.imageWidth - 20));
        int barColor = tps >= 18.0 ? 0xFF27AE60 : (tps >= 15.0 ? 0xFFE67E22 : 0xFFE74C3C);
        g.fill(this.leftPos + 10, this.topPos + 36, this.leftPos + 10 + barWidth, this.topPos + 39, barColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
