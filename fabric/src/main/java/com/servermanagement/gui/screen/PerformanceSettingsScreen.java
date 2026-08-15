package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.menu.PerformanceSettingsMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.UpdatePerformanceSettingPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PerformanceSettingsScreen extends ScalableContainerScreen<PerformanceSettingsMenu> {

    private int scrollOffset = 0;
    private static final int SCROLL_STEP = 16;

    // Layout constants
    private static final int HEADER_HEIGHT = 78;
    private static final int FOOTER_HEIGHT = 35;
    private static final int ROW_SPACING = 30;
    private static final int TOGGLE_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 18;

    private int currentPage = 0;
    private boolean gcPatchConfirmMode = false;

    public PerformanceSettingsScreen(PerformanceSettingsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 430, 420);
        this.imageWidth = 430;
        this.imageHeight = 420;
    }

    private int getContentTop() {
        return this.topPos + HEADER_HEIGHT;
    }

    private int getContentBottom() {
        return this.topPos + this.imageHeight - FOOTER_HEIGHT;
    }

    private int getContentHeight() {
        return getContentBottom() - getContentTop();
    }

    private int getMaxScroll() {
        int rows;
        if (currentPage == 0) rows = 8;
        else if (currentPage == 1) rows = 10;
        else if (currentPage == 2) {
            // Stats page: ~9 lines at 22px + padding
            int statsHeight = 17 * 22 + 10;
            return Math.max(0, statsHeight - getContentHeight());
        }
        else return 0;
        return Math.max(0, rows * ROW_SPACING - getContentHeight());
    }

    private boolean isRowVisible(int y, int height) {
        return (y + height > getContentTop()) && (y + height <= getContentBottom());
    }

    @Override
    protected void init() {
        super.init();
        // Fabric: re-pull settings from the client cache on every init() so that
        // a late SyncPerformanceSettingsPacket triggering refreshOpenScreen()
        // updates the toggles/tunables on the second init() pass instead of
        // showing the stale snapshot latched in the menu constructor.
        this.menu.reloadFromClientCache();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        int cX = this.leftPos;
        int cY = this.topPos;

        // --- Tab buttons (fixed position, never scrolled) ---
        int tabGap = 5;
        int tabTotalWidth = this.imageWidth - 20; // 10px margin each side
        int tabWidth = (tabTotalWidth - tabGap * 2) / 3;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Toggles"),
            btn -> { com.servermanagement.gui.debug.DebugLogger.logTabChange("PerformanceSettingsScreen", String.valueOf(currentPage), "0/Toggles"); currentPage = 0; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 10, cY + 42, tabWidth, 20)
            .style(currentPage == 0 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Settings"),
            btn -> { com.servermanagement.gui.debug.DebugLogger.logTabChange("PerformanceSettingsScreen", String.valueOf(currentPage), "1/Settings"); currentPage = 1; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 10 + tabWidth + tabGap, cY + 42, tabWidth, 20)
            .style(currentPage == 1 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Stats"),
            btn -> { com.servermanagement.gui.debug.DebugLogger.logTabChange("PerformanceSettingsScreen", String.valueOf(currentPage), "2/Stats"); currentPage = 2; scrollOffset = 0; rebuildWidgets(); })
            .bounds(cX + 10 + (tabWidth + tabGap) * 2, cY + 42, tabWidth, 20)
            .style(currentPage == 2 ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());

        // --- Content widgets (only add if within visible content area) ---
        int contentY = getContentTop() - scrollOffset;
        int rightCol = cX + this.imageWidth - 90;

        if (currentPage == 0) {
            buildTogglesPage(cX, contentY, rightCol);
        } else if (currentPage == 1) {
            buildSettingsPage(cX, contentY);
        } else if (currentPage == 2) {
            buildStatsPageWidgets(cX, contentY);
        }

        // --- Bottom buttons (fixed position, symmetrical) ---
        int bottomBtnW = (this.imageWidth - 30) / 2;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("\u2190 Dashboard"),
            btn -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(cX + 10, cY + this.imageHeight - 30, bottomBtnW, 22)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            btn -> this.onClose())
            .bounds(cX + this.imageWidth - bottomBtnW - 10, cY + this.imageHeight - 30, bottomBtnW, 22)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }

    private void buildTogglesPage(int cX, int contentY, int rightCol) {
        int row = 0;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "feature_enabled",
            this.menu.isFeatureEnabled(), v -> this.menu.setFeatureEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "auto_optimize",
            this.menu.isTpsAutoOptimize(), v -> this.menu.setTpsAutoOptimize(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "item_merging",
            this.menu.isItemMergingEnabled(), v -> this.menu.setItemMergingEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "mob_spawn_limiter",
            this.menu.isMobSpawnLimiterEnabled(), v -> this.menu.setMobSpawnLimiterEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "entity_activation_range",
            this.menu.isEntityActivationRangeEnabled(), v -> this.menu.setEntityActivationRangeEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "villager_throttle",
            this.menu.isVillagerThrottleEnabled(), v -> this.menu.setVillagerThrottleEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "redstone_throttle",
            this.menu.isRedstoneThrottleEnabled(), v -> this.menu.setRedstoneThrottleEnabled(v));
        row++;
        addToggleIfVisible(rightCol, contentY + ROW_SPACING * row, "tps_monitor",
            this.menu.isTpsMonitorEnabled(), v -> this.menu.setTpsMonitorEnabled(v));
    }

    private void addToggleIfVisible(int x, int y, String key, boolean initialState, java.util.function.Consumer<Boolean> setter) {
        if (!isRowVisible(y, TOGGLE_HEIGHT)) return;
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

    private void buildSettingsPage(int cX, int contentY) {
        int valBtnX = cX + this.imageWidth - 120;
        int row = 0;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "item_merge_radius",
            this.menu.getItemMergeRadius(), 0.5, 1.0, 10.0, true);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "item_merge_interval",
            this.menu.getItemMergeInterval(), 10, 10, 200, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "mob_cap_multiplier",
            this.menu.getMobCapMultiplier(), 5, 10, 100, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "monster_activation_range",
            this.menu.getMonsterActivationRange(), 4, 8, 128, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "animal_activation_range",
            this.menu.getAnimalActivationRange(), 4, 8, 128, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "misc_activation_range",
            this.menu.getMiscActivationRange(), 2, 4, 64, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "villager_tick_interval",
            this.menu.getVillagerTickInterval(), 1, 1, 10, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "redstone_updates_per_tick",
            this.menu.getRedstoneUpdatesPerTick(), 100, 100, 100000, false);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "tps_warning_threshold",
            this.menu.getTpsWarningThreshold(), 0.5, 5.0, 20.0, true);
        row++;
        addValueButtonsIfVisible(valBtnX, contentY + ROW_SPACING * row, "tps_critical_threshold",
            this.menu.getTpsCriticalThreshold(), 0.5, 5.0, 20.0, true);
    }

    private void addValueButtonsIfVisible(int x, int y, String key, double currentValue,
                                           double step, double min, double max, boolean isDouble) {
        if (!isRowVisible(y, BUTTON_HEIGHT)) return;

        int btnSpacing = 55;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("-"),
            btn -> {
                double newVal = Math.max(min, currentValue - step);
                updateNumericSetting(key, newVal, isDouble);
            })
            .bounds(x, y, 25, BUTTON_HEIGHT)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());

        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("+"),
            btn -> {
                double newVal = Math.min(max, currentValue + step);
                updateNumericSetting(key, newVal, isDouble);
            })
            .bounds(x + btnSpacing, y, 25, BUTTON_HEIGHT)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }

    private void updateNumericSetting(String key, double newVal, boolean isDouble) {
        long tick = minecraft.player != null ? minecraft.player.tickCount : 0;
        String valStr = isDouble ? String.valueOf(newVal) : String.valueOf((int) newVal);
        ModNetworking.sendToServer(new UpdatePerformanceSettingPacket(key, valStr, tick));
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
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return true;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * SCROLL_STEP));
        rebuildWidgets();
        return true;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.renderBg(g, partialTick, mouseX, mouseY);

        int cX = this.leftPos;
        int cY = this.topPos;
        int contentTop = getContentTop();
        int contentBottom = getContentBottom();

        // 1) Render widgets (super.render includes all addRenderableWidget items)
        super.renderContent(g, mouseX, mouseY, partialTick);

        // 2) Repaint header zone to cover any widget bleed from scrolling
        g.fill(cX, cY, cX + this.imageWidth, cY + 35, 0xFF1A1A2E);
        g.fill(cX, cY + 35, cX + this.imageWidth, cY + 36, 0xFF333333);
        // TPS bar
        double tps = this.menu.getCurrentTps();
        int barWidth = (int) (Math.min(tps / 20.0, 1.0) * (this.imageWidth - 20));
        int barColor = tps >= 18.0 ? 0xFF27AE60 : (tps >= 15.0 ? 0xFFE67E22 : 0xFFE74C3C);
        g.fill(cX + 10, cY + 36, cX + 10 + barWidth, cY + 39, barColor);

        // 3) Repaint footer zone to cover any widget bleed from scrolling
        g.fill(cX, contentBottom, cX + this.imageWidth, cY + this.imageHeight - 32, 0xE0101010);

        // 4) Draw header text (on top of repainted header)
        g.drawString(this.font, "Server Performance", cX + 15, cY + 8, 0xFFD700, true);
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

        // 5) Render content text labels with scissor clipping
        g.enableScissor(cX, contentTop, cX + this.imageWidth, contentBottom);

        int contentY = contentTop - scrollOffset;

        if (currentPage == 0) {
            renderTogglesLabels(g, cX, contentY);
        } else if (currentPage == 1) {
            renderSettingsLabels(g, cX, contentY);
        } else if (currentPage == 2) {
            renderStatsPage(g, cX, contentTop - scrollOffset);
        }

        g.disableScissor();

        this.renderTooltip(g, mouseX, mouseY);
    }

    private void renderTogglesLabels(GuiGraphics g, int cX, int contentY) {
        String[] labels = {
            "Server Performance:", "Auto-Optimize:", "Item Merging:",
            "Mob Spawn Limiter:", "Entity Activation Range:", "Villager Throttle:",
            "Redstone Throttle:", "TPS Monitor:"
        };
        for (int i = 0; i < labels.length; i++) {
            int y = contentY + ROW_SPACING * i + 5;
            int color = i == 0 ? 0xFFFF55 : 0xFFFFFF;
            g.drawString(this.font, labels[i], cX + 20, y, color, true);
        }
    }

    private void renderSettingsLabels(GuiGraphics g, int cX, int contentY) {
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
            int y = contentY + ROW_SPACING * i + 4;
            g.drawString(this.font, labels[i][0], cX + 20, y, 0xFFFFFF, true);
            // Value centered between - and + buttons
            String valStr = labels[i][1];
            int valWidth = this.font.width(valStr);
            int valCenter = cX + this.imageWidth - 120 + 40;
            g.drawString(this.font, valStr, valCenter - valWidth / 2, y, 0x55FF55, true);
        }
    }

    private void buildStatsPageWidgets(int cX, int contentY) {
        var gcUrgency = com.servermanagement.features.serverperformance.GCAdvisor.getUrgency();
        boolean showOptimize = gcUrgency != com.servermanagement.features.serverperformance.GCAdvisor.UrgencyLevel.OK
            && !com.servermanagement.features.serverperformance.GCAdvisor.isScriptPatched();

        if (gcPatchConfirmMode) {
            int btnY = getContentTop() + 100;
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Confirm Patch"),
                btn -> {
                    com.servermanagement.network.ModNetworking.sendToServer(
                        new com.servermanagement.network.packet.PatchJvmFlagsPacket(true));
                    com.servermanagement.features.serverperformance.GCAdvisor.setScriptPatched(true);
                    gcPatchConfirmMode = false;
                    rebuildWidgets();
                })
                .bounds(cX + this.imageWidth / 2 - 120, btnY, 110, 20)
                .style(ModernButton.ButtonStyle.SUCCESS)
                .build());

            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Cancel"),
                btn -> {
                    gcPatchConfirmMode = false;
                    rebuildWidgets();
                })
                .bounds(cX + this.imageWidth / 2 + 10, btnY, 110, 20)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());
        } else if (showOptimize) {
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Optimize JVM"),
                btn -> {
                    gcPatchConfirmMode = true;
                    rebuildWidgets();
                })
                .bounds(cX + this.imageWidth - 120, getContentTop() - scrollOffset - 2, 100, 16)
                .style(ModernButton.ButtonStyle.DANGER)
                .build());
        }
    }

    private void renderStatsPage(GuiGraphics g, int cX, int startY) {
        int y = startY;
        int spacing = 22;

        if (gcPatchConfirmMode) {
            g.drawCenteredString(this.font, "\u00a7ePatch run scripts with ZGC flags?", cX + this.imageWidth / 2, y + 10, 0xFFFF55);
            y += spacing;
            var gcType = com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC();
            g.drawCenteredString(this.font, "Current: " + gcType.getDisplayName(), cX + this.imageWidth / 2, y + 10, 0xAAAAAA);
            y += spacing;
            g.drawCenteredString(this.font, "Recommended: " + com.servermanagement.features.serverperformance.GCAdvisor.getRecommendedFlags(), cX + this.imageWidth / 2, y + 10, 0x55FF55);
            y += spacing;
            g.drawCenteredString(this.font, "A .bak backup of your script will be created.", cX + this.imageWidth / 2, y + 10, 0x777777);
            return;
        }

        var gcType = com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC();
        var gcUrgency = com.servermanagement.features.serverperformance.GCAdvisor.getUrgency();

        if (com.servermanagement.features.serverperformance.GCAdvisor.isScriptPatched()) {
            g.fill(cX + 10, y - 2, cX + this.imageWidth - 10, y + 12, 0x8027AE60);
            g.drawString(this.font, "GC: ZGC (Optimized!) - Restart to activate", cX + 15, y, 0xFF55FF55, true);
            y += spacing;
        } else if (gcUrgency == com.servermanagement.features.serverperformance.GCAdvisor.UrgencyLevel.CRITICAL) {
            g.fill(cX + 10, y - 2, cX + this.imageWidth - 10, y + 12, 0x80E74C3C);
            g.drawString(this.font, gcType.getDisplayName() + " + Distant Horizons - Switch to ZGC!", cX + 15, y, 0xFFFF5555, true);
            y += spacing;
        } else if (gcUrgency == com.servermanagement.features.serverperformance.GCAdvisor.UrgencyLevel.WARNING) {
            g.fill(cX + 10, y - 2, cX + this.imageWidth - 10, y + 12, 0x80E67E22);
            g.drawString(this.font, gcType.getDisplayName() + " Detected - ZGC recommended", cX + 15, y, 0xFFFFAA00, true);
            y += spacing;
        } else {
            g.drawString(this.font, "GC: " + gcType.getDisplayName() + " (Optimal)", cX + 20, y, 0x55FF55, true);
            y += spacing;
        }

        boolean dhAvailable = com.servermanagement.integration.dh.DistantHorizonsHook.isAvailable();
        String dhStatus = dhAvailable
            ? "Distant Horizons: Active (v" + com.servermanagement.integration.dh.DistantHorizonsHook.getVersion() + ")"
            : "Distant Horizons: Not Installed";
        int dhColor = dhAvailable ? 0x55FFFF : 0x777777;
        g.drawString(this.font, dhStatus, cX + 20, y, dhColor, true);
        y += spacing;

        g.drawString(this.font, String.format("Heap: %dMB / %dMB",
            Runtime.getRuntime().totalMemory() / (1024 * 1024),
            Runtime.getRuntime().maxMemory() / (1024 * 1024)), cX + 20, y, 0xAAAAAA, true);
        y += spacing;

        double allocRate = com.servermanagement.features.serverperformance.AllocationTracker.getAllocationRateMBps();
        if (com.servermanagement.features.serverperformance.AllocationTracker.isSupported()) {
            int allocColor = allocRate > 500 ? 0xE74C3C : (allocRate > 200 ? 0xE67E22 : 0xAAAAAA);
            g.drawString(this.font, String.format("Alloc Rate: %.0f MB/s", allocRate), cX + 20, y, allocColor, true);
        } else {
            g.drawString(this.font, "Alloc Rate: N/A", cX + 20, y, 0x777777, true);
        }
        y += spacing;

        long gcPauseMs = com.servermanagement.features.serverperformance.GCAdvisor.getTotalGCPauseMs();
        long gcCount = com.servermanagement.features.serverperformance.GCAdvisor.getTotalGCCount();
        g.drawString(this.font, String.format("GC Pauses: %d (%dms total)", gcCount, gcPauseMs), cX + 20, y, 0xAAAAAA, true);
        y += spacing + 5;

        g.drawString(this.font, "\u00a76=== Server Performance ===", cX + 20, y, 0xFFFFFF, true);
        y += spacing;

        double tps = this.menu.getCurrentTps();
        int tpsColor = tps >= 18.0 ? 0x27AE60 : (tps >= 15.0 ? 0xE67E22 : 0xE74C3C);
        g.drawString(this.font, String.format("Current TPS: %.1f", tps), cX + 20, y, tpsColor, true);
        y += spacing;

        g.drawString(this.font, String.format("Average MSPT: %.1fms", this.menu.getAverageMspt()), cX + 20, y, 0xFFFFFF, true);
        y += spacing;

        String autoOptStatus = this.menu.isAutoOptimizeActive() ? "\u00a7eACTIVE" : (this.menu.isTpsAutoOptimize() ? "\u00a7aStandby" : "\u00a7cOff");
        g.drawString(this.font, "Auto-Optimize: " + autoOptStatus, cX + 20, y, 0xFFFFFF, true);
        y += spacing + 5;

        g.drawString(this.font, "\u00a77--- Cumulative Stats ---", cX + 20, y, 0xFFFFFF, true);
        y += spacing;

        g.drawString(this.font, "Items Merged: " + this.menu.getTotalItemsMerged(), cX + 20, y, 0xAAAAAA, true);
        y += spacing;

        g.drawString(this.font, "Spawns Cancelled: " + this.menu.getTotalSpawnsCancelled(), cX + 20, y, 0xAAAAAA, true);
        y += spacing;

        g.drawString(this.font, "Entities Throttled: " + this.menu.getTotalEntitiesThrottled(), cX + 20, y, 0xAAAAAA, true);
        y += spacing;

        g.drawString(this.font, "Redstone Throttled: " + this.menu.getTotalRedstoneThrottled(), cX + 20, y, 0xAAAAAA, true);
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
