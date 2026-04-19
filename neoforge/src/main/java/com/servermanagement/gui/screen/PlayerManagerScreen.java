package com.servermanagement.gui.screen;

import com.servermanagement.features.playermanager.PlayerManagerClientData;
import com.servermanagement.gui.PlayerManagerMenu;
import com.servermanagement.gui.ScreenScaler;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Reworked Player Manager Screen with tabs for Online, Banned, and Whitelist.
 */
public class PlayerManagerScreen extends AbstractContainerScreen<PlayerManagerMenu> {
    
    private enum Tab { ONLINE, BANNED, WHITELIST }
    private enum SubView { LIST, ACTIONS, KICK_CONFIRM, BAN_CONFIRM }
    
    private Tab currentTab = Tab.ONLINE;
    private SubView currentSubView = SubView.LIST;
    private String selectedPlayer = null;
    private int scrollOffset = 0;
    
    // For kick/ban dialogs
    private EditBox reasonBox;
    private boolean banWithIP = false;
    
    // For whitelist add — preserve value across rebuilds
    private EditBox whitelistNameBox;
    private String savedWhitelistInput = "";

    public PlayerManagerScreen(PlayerManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 380;
        this.imageHeight = 280;
    }

    @Override
    protected void init() {
        int[] dim = ScreenScaler.scale(380, 280, this.width, this.height);
        this.imageWidth = dim[0];
        this.imageHeight = dim[1];
        super.init();
        
        // Request player lists from server
        ModNetworking.sendToServer(new PMRequestPlayerListsPacket());
        
        rebuildUI();
    }
    
    /**
     * Calculate the maximum number of entry rows that fit in the content area.
     */
    private int getMaxRows() {
        int contentH = this.imageHeight - 100; // y0+58 to y0+imageHeight-42
        int overhead = 22; // scroll button area
        if (currentTab == Tab.WHITELIST) {
            overhead += 46; // whitelist toggle (22) + add input (20) + gaps (4)
        }
        return Math.max(1, (contentH - overhead) / 24);
    }
    
    /**
     * Called from PMSyncPlayerListsPacket handler to refresh the UI when data arrives.
     */
    public void refreshFromSync() {
        // Save EditBox value before rebuild
        if (whitelistNameBox != null) {
            savedWhitelistInput = whitelistNameBox.getValue();
        }
        rebuildUI();
    }
    
    private void rebuildUI() {
        // Save EditBox value before clearing widgets
        if (whitelistNameBox != null) {
            savedWhitelistInput = whitelistNameBox.getValue();
        }
        
        this.clearWidgets();
        
        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;
        
        // Tab buttons
        int tabW = (this.imageWidth - 30) / 3;
        int tabY = y0 + 35;
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Online"),
            btn -> { currentTab = Tab.ONLINE; currentSubView = SubView.LIST; selectedPlayer = null; scrollOffset = 0; rebuildUI(); })
            .bounds(x0 + 10, tabY, tabW, 18)
            .style(currentTab == Tab.ONLINE ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Banned"),
            btn -> { currentTab = Tab.BANNED; currentSubView = SubView.LIST; selectedPlayer = null; scrollOffset = 0; rebuildUI(); })
            .bounds(x0 + 15 + tabW, tabY, tabW, 18)
            .style(currentTab == Tab.BANNED ? ModernButton.ButtonStyle.DANGER : ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Whitelist"),
            btn -> { currentTab = Tab.WHITELIST; currentSubView = SubView.LIST; selectedPlayer = null; scrollOffset = 0; rebuildUI(); })
            .bounds(x0 + 20 + tabW * 2, tabY, tabW, 18)
            .style(currentTab == Tab.WHITELIST ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        // Content area starts at y0 + 58
        int contentY = y0 + 58;
        int btnWidth = this.imageWidth - 30;
        
        switch (currentSubView) {
            case LIST -> buildListView(x0, contentY, btnWidth);
            case ACTIONS -> buildActionsView(x0, contentY, btnWidth);
            case KICK_CONFIRM -> buildKickConfirmView(x0, contentY, btnWidth);
            case BAN_CONFIRM -> buildBanConfirmView(x0, contentY, btnWidth);
        }
        
        // Bottom buttons
        int bottomY = y0 + this.imageHeight - 35;
        int halfW = (this.imageWidth - 30) / 2;
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Dashboard"),
            btn -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, "")))
            .bounds(x0 + 10, bottomY, halfW, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            btn -> this.onClose())
            .bounds(x0 + this.imageWidth - halfW - 10, bottomY, halfW, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    private void buildListView(int x0, int contentY, int btnWidth) {
        List<String> entries = getListEntries();
        int maxRows = getMaxRows();
        int maxScroll = Math.max(0, entries.size() - maxRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        
        int y = contentY;
        
        // Whitelist tab: toggle + add input at TOP (fixed position)
        if (currentTab == Tab.WHITELIST) {
            // Whitelist enforcement toggle
            boolean wlEnabled = PlayerManagerClientData.isWhitelistEnabled();
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal(wlEnabled ? "Whitelist Enforcement: ON" : "Whitelist Enforcement: OFF"),
                btn -> {
                    ModNetworking.sendToServer(new PMWhitelistTogglePacket(!PlayerManagerClientData.isWhitelistEnabled()));
                })
                .bounds(x0 + 15, y, btnWidth, 20)
                .style(wlEnabled ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY)
                .build());
            y += 24;
            
            // Add player input
            int inputW = btnWidth - 70;
            whitelistNameBox = new EditBox(this.font, x0 + 15, y, inputW, 18, Component.literal("Player name"));
            whitelistNameBox.setMaxLength(16);
            whitelistNameBox.setHint(Component.literal("Enter player name..."));
            if (!savedWhitelistInput.isEmpty()) {
                whitelistNameBox.setValue(savedWhitelistInput);
            }
            this.addRenderableWidget(whitelistNameBox);
            
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Add"),
                btn -> {
                    if (whitelistNameBox != null && !whitelistNameBox.getValue().isEmpty()) {
                        ModNetworking.sendToServer(new PMWhitelistPacket(whitelistNameBox.getValue(), true));
                        whitelistNameBox.setValue("");
                        savedWhitelistInput = "";
                    }
                })
                .bounds(x0 + 20 + inputW, y, 60, 18)
                .style(ModernButton.ButtonStyle.SUCCESS)
                .build());
            y += 22;
        }
        
        // Entry rows
        for (int i = scrollOffset; i < Math.min(scrollOffset + maxRows, entries.size()); i++) {
            final String name = entries.get(i);
            
            if (currentTab == Tab.ONLINE) {
                // Online player - click to show actions
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal(name),
                    btn -> { selectedPlayer = name; currentSubView = SubView.ACTIONS; rebuildUI(); })
                    .bounds(x0 + 15, y, btnWidth, 20)
                    .style(ModernButton.ButtonStyle.PRIMARY)
                    .build());
            } else if (currentTab == Tab.BANNED) {
                // Banned player - show with unban button
                int nameW = btnWidth - 70;
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal(name),
                    btn -> {})
                    .bounds(x0 + 15, y, nameW, 20)
                    .style(ModernButton.ButtonStyle.SECONDARY)
                    .build());
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("Unban"),
                    btn -> { ModNetworking.sendToServer(new PMUnbanPlayerPacket(name)); })
                    .bounds(x0 + 20 + nameW, y, 60, 20)
                    .style(ModernButton.ButtonStyle.SUCCESS)
                    .build());
            } else {
                // Whitelisted player - show with remove button
                int nameW = btnWidth - 70;
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal(name),
                    btn -> {})
                    .bounds(x0 + 15, y, nameW, 20)
                    .style(ModernButton.ButtonStyle.SECONDARY)
                    .build());
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("Remove"),
                    btn -> { ModNetworking.sendToServer(new PMWhitelistPacket(name, false)); })
                    .bounds(x0 + 20 + nameW, y, 60, 20)
                    .style(ModernButton.ButtonStyle.DANGER)
                    .build());
            }
            y += 24;
        }
        
        // Scroll buttons if needed
        if (entries.size() > maxRows) {
            int scrollBtnY = contentY + this.imageHeight - 100 - 22;
            if (scrollOffset > 0) {
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("▲ Up"),
                    btn -> { scrollOffset = Math.max(0, scrollOffset - maxRows); rebuildUI(); })
                    .bounds(x0 + 15, scrollBtnY, 60, 18)
                    .style(ModernButton.ButtonStyle.SECONDARY)
                    .build());
            }
            if (scrollOffset + maxRows < entries.size()) {
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("▼ Down"),
                    btn -> { scrollOffset = Math.min(maxScroll, scrollOffset + maxRows); rebuildUI(); })
                    .bounds(x0 + this.imageWidth - 75, scrollBtnY, 60, 18)
                    .style(ModernButton.ButtonStyle.SECONDARY)
                    .build());
            }
        }
    }
    
    private void buildActionsView(int x0, int contentY, int btnWidth) {
        int y = contentY + 4;
        
        // Spectate
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("👁 Spectate Player"),
            btn -> {
                ModNetworking.sendToServer(new PMSpectatePlayerPacket(selectedPlayer));
                this.onClose();
            })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.SUCCESS)
            .build());
        
        // View Inventory
        y += 24;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("📦 View Inventory"),
            btn -> {
                ModNetworking.sendToServer(new PMViewInventoryPacket(selectedPlayer));
                this.onClose();
            })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.PRIMARY)
            .build());
        
        // Kick
        y += 24;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("🚪 Kick Player"),
            btn -> { currentSubView = SubView.KICK_CONFIRM; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.WARNING)
            .build());
        
        // Ban
        y += 24;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("🔨 Ban Player"),
            btn -> { banWithIP = false; currentSubView = SubView.BAN_CONFIRM; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        // Back to list
        y += 30;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Back to List"),
            btn -> { currentSubView = SubView.LIST; selectedPlayer = null; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    private void buildKickConfirmView(int x0, int contentY, int btnWidth) {
        int y = contentY + 4;
        
        // Reason input
        reasonBox = new EditBox(this.font, x0 + 15, y + 12, btnWidth, 18, Component.literal("Reason"));
        reasonBox.setMaxLength(256);
        reasonBox.setHint(Component.literal("Enter kick reason (optional)..."));
        this.addRenderableWidget(reasonBox);
        
        // Confirm kick
        y += 40;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Confirm Kick"),
            btn -> {
                String reason = reasonBox != null ? reasonBox.getValue() : "";
                ModNetworking.sendToServer(new PMKickPlayerPacket(selectedPlayer, reason));
                currentSubView = SubView.LIST;
                selectedPlayer = null;
                rebuildUI();
            })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        // Cancel
        y += 24;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Cancel"),
            btn -> { currentSubView = SubView.ACTIONS; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    private void buildBanConfirmView(int x0, int contentY, int btnWidth) {
        int y = contentY + 4;
        
        // Reason input
        reasonBox = new EditBox(this.font, x0 + 15, y + 12, btnWidth, 18, Component.literal("Reason"));
        reasonBox.setMaxLength(256);
        reasonBox.setHint(Component.literal("Enter ban reason (optional)..."));
        this.addRenderableWidget(reasonBox);
        
        // Ban IP toggle button
        y += 38;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal(banWithIP ? "✓ Also Ban IP" : "☐ Also Ban IP"),
            btn -> { banWithIP = !banWithIP; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(banWithIP ? ModernButton.ButtonStyle.WARNING : ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        // Confirm ban
        y += 26;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Confirm Ban"),
            btn -> {
                String reason = reasonBox != null ? reasonBox.getValue() : "";
                ModNetworking.sendToServer(new PMBanPlayerPacket(selectedPlayer, reason, banWithIP));
                currentSubView = SubView.LIST;
                selectedPlayer = null;
                rebuildUI();
            })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        // Cancel
        y += 24;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Cancel"),
            btn -> { currentSubView = SubView.ACTIONS; rebuildUI(); })
            .bounds(x0 + 15, y, btnWidth, 20)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    private List<String> getListEntries() {
        return switch (currentTab) {
            case ONLINE -> {
                List<String> names = new ArrayList<>();
                Minecraft mc = Minecraft.getInstance();
                ClientPacketListener connection = mc.getConnection();
                if (connection != null) {
                    String selfName = mc.player != null ? mc.player.getName().getString() : "";
                    for (PlayerInfo info : connection.getOnlinePlayers()) {
                        String name = info.getProfile().getName();
                        // Filter out the admin's own player
                        if (!name.equals(selfName)) {
                            names.add(name);
                        }
                    }
                }
                yield names;
            }
            case BANNED -> new ArrayList<>(PlayerManagerClientData.getBannedPlayers());
            case WHITELIST -> new ArrayList<>(PlayerManagerClientData.getWhitelistedPlayers());
        };
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        
        // Border
        guiGraphics.fill(x0 - 1, y0 - 1, x0 + this.imageWidth + 1, y0 + this.imageHeight + 1, 0xFF000000);
        // Main dark background
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + this.imageHeight, 0xE0101010);
        
        // Header bar
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + 32, 0xFF1A1A2E);
        guiGraphics.fill(x0, y0 + 31, x0 + this.imageWidth, y0 + 32, 0xFF333355);
        guiGraphics.fill(x0, y0 + 32, x0 + this.imageWidth, y0 + 33, 0xFF222222);
        
        // Tab underline for active tab
        int tabW = (this.imageWidth - 30) / 3;
        int tabY = y0 + 53;
        int tabIdx = currentTab.ordinal();
        int tabX = x0 + 10 + tabIdx * (tabW + 5);
        int underlineColor = switch (currentTab) {
            case ONLINE -> 0xFF4488FF;
            case BANNED -> 0xFFFF4444;
            case WHITELIST -> 0xFF44FF44;
        };
        guiGraphics.fill(tabX, tabY, tabX + tabW, tabY + 2, underlineColor);
        
        // Content area bg
        guiGraphics.fill(x0 + 8, y0 + 56, x0 + this.imageWidth - 8, y0 + this.imageHeight - 42, 0x0AFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        int x0 = this.leftPos;
        int y0 = this.topPos;
        
        // Title
        guiGraphics.drawString(this.font, "Player Manager", x0 + 15, y0 + 8, 0xFFD700, true);
        
        // Subtitle — context-dependent
        String subtitle = switch (currentSubView) {
            case LIST -> {
                List<String> entries = getListEntries();
                yield switch (currentTab) {
                    case ONLINE -> entries.size() + " player(s) online";
                    case BANNED -> entries.size() + " banned player(s)";
                    case WHITELIST -> entries.size() + " whitelisted player(s)";
                };
            }
            case ACTIONS -> "Actions for " + selectedPlayer;
            case KICK_CONFIRM -> "Kick " + selectedPlayer;
            case BAN_CONFIRM -> "Ban " + selectedPlayer;
        };
        guiGraphics.drawString(this.font, subtitle, x0 + 15, y0 + 20, 0xAAAAAA, true);
        
        // Sub-view specific labels
        int contentY = y0 + 58;
        if (currentSubView == SubView.KICK_CONFIRM) {
            guiGraphics.drawString(this.font, "Reason:", x0 + 15, contentY + 6, 0xCCCCCC, true);
        } else if (currentSubView == SubView.BAN_CONFIRM) {
            guiGraphics.drawString(this.font, "Reason:", x0 + 15, contentY + 6, 0xCCCCCC, true);
        } else if (currentSubView == SubView.LIST && getListEntries().isEmpty()) {
            int emptyY = contentY + (currentTab == Tab.WHITELIST ? 60 : 40);
            String emptyMsg = switch (currentTab) {
                case ONLINE -> "No other players online";
                case BANNED -> "No banned players";
                case WHITELIST -> "Whitelist is empty";
            };
            guiGraphics.drawCenteredString(this.font, emptyMsg, x0 + this.imageWidth / 2, emptyY, 0x666666);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentSubView == SubView.LIST) {
            int maxRows = getMaxRows();
            List<String> entries = getListEntries();
            int maxScroll = Math.max(0, entries.size() - maxRows);
            if (scrollY > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                rebuildUI();
                return true;
            } else if (scrollY < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                rebuildUI();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Prevent inventory key (E) from closing the screen when an EditBox is focused
        if (keyCode != 256) { // 256 = Escape — always allow closing
            if ((reasonBox != null && reasonBox.isFocused()) ||
                (whitelistNameBox != null && whitelistNameBox.isFocused())) {
                return reasonBox != null && reasonBox.isFocused()
                    ? reasonBox.keyPressed(keyCode, scanCode, modifiers)
                    : whitelistNameBox.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
