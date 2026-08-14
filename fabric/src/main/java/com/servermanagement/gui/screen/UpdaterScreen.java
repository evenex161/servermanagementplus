package com.servermanagement.gui.screen;

import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.UpdaterMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.CheckForUpdatesPacket;
import com.servermanagement.network.packet.StartServerUpdatePacket;
import com.servermanagement.network.packet.SyncUpdateInfoPacket;
import com.servermanagement.client.ClientUpdateManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class UpdaterScreen extends ScalableContainerScreen<UpdaterMenu> {
    private boolean hasUpdate(SyncUpdateInfoPacket packet) {
        if (packet == null || packet.getResult() == null) return false;
        com.servermanagement.updater.UpdateInfo target = packet.getResult().resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
        return target != null;
    }
    
    private com.servermanagement.updater.UpdateInfo getTarget(SyncUpdateInfoPacket packet) {
        if (packet == null || packet.getResult() == null) return null;
        return packet.getResult().resolve(com.servermanagement.updater.UpdatePreferences.getMainSource(), com.servermanagement.updater.UpdatePreferences.isCheckFallback());
    }

    private boolean confirmMode = false;
    private boolean smartStartWarningMode = false;
    private boolean overrideSelected = false;
    private SyncUpdateInfoPacket currentInfo = null;
    private boolean hasAutoChecked = false;
    private int tickCount = 0;

    public UpdaterScreen(UpdaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 300, 220);
        this.imageWidth = 300;
        this.imageHeight = 220;
        this.currentInfo = ClientUpdateManager.latestUpdateInfo;
    }

    public void onUpdateInfoReceived(SyncUpdateInfoPacket packet) {
        com.servermanagement.updater.UpdatePreferences.load();

        this.currentInfo = packet;
        this.confirmMode = false;
        this.smartStartWarningMode = false;
        this.overrideSelected = false;
        this.refreshWidgets();
    }

    @Override
    protected void init() {
        super.init();
        if (this.currentInfo == null && !this.hasAutoChecked) {
            this.hasAutoChecked = true;
            ModNetworking.sendToServer(new CheckForUpdatesPacket());
        }
        refreshWidgets();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.tickCount++;
    }

    protected void refreshWidgets() {
        this.clearWidgets();

        int cX = this.leftPos;
        int cY = this.topPos;

        // Bottom buttons
        int btnWidth = 100;
        int btnHeight = 20;

        if (confirmMode) {
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("CANCEL"),
                btn -> {
                    this.confirmMode = false;
                    this.smartStartWarningMode = false;
                    this.refreshWidgets();
                })
                .bounds(cX + this.imageWidth / 2 - 110, cY + this.imageHeight - 35, btnWidth, btnHeight)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());

            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("CONFIRM UPDATE"),
                btn -> {
                    ModNetworking.sendToServer(new StartServerUpdatePacket(currentInfo != null ? getTarget(currentInfo).downloadUrl() : "", overrideSelected));
                    this.onClose();
                })
                .bounds(cX + this.imageWidth / 2 + 10, cY + this.imageHeight - 35, btnWidth, btnHeight)
                .style(ModernButton.ButtonStyle.DANGER)
                .build());
        } else if (smartStartWarningMode) {
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Auto-override run scripts & Update"),
                btn -> {
                    this.overrideSelected = true;
                    this.smartStartWarningMode = false;
                    this.confirmMode = true;
                    this.refreshWidgets();
                })
                .bounds(cX + this.imageWidth / 2 - 120, cY + this.imageHeight - 75, 240, btnHeight)
                .style(ModernButton.ButtonStyle.PRIMARY)
                .build());
                
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("I'll do it manually & Update"),
                btn -> {
                    this.overrideSelected = false;
                    this.smartStartWarningMode = false;
                    this.confirmMode = true;
                    this.refreshWidgets();
                })
                .bounds(cX + this.imageWidth / 2 - 120, cY + this.imageHeight - 50, 240, btnHeight)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());
                
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("CANCEL"),
                btn -> {
                    this.smartStartWarningMode = false;
                    this.refreshWidgets();
                })
                .bounds(cX + this.imageWidth / 2 - 50, cY + this.imageHeight - 25, 100, btnHeight)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());
        } else {
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Check for Updates"),
                btn -> {
                    this.currentInfo = null;
                    this.hasAutoChecked = true;
                    ModNetworking.sendToServer(new CheckForUpdatesPacket());
                    this.refreshWidgets();
                })
                .bounds(cX + 10, cY + this.imageHeight - 35, 110, btnHeight)
                .style(ModernButton.ButtonStyle.PRIMARY)
                .build());

            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Path: " + (com.servermanagement.updater.UpdatePreferences.getUpdateChannel(null).equals("beta") ? "Beta" : "Release")),
                btn -> {
                    String current = com.servermanagement.updater.UpdatePreferences.getUpdateChannel(null);
                    com.servermanagement.updater.UpdatePreferences.setUpdateChannel(current.equals("beta") ? "release" : "beta");
                    com.servermanagement.updater.UpdatePreferences.save();
                    this.currentInfo = null;
                    this.hasAutoChecked = true;
                    ModNetworking.sendToServer(new CheckForUpdatesPacket());
                    this.refreshWidgets();
                })
                .bounds(cX + 125, cY + this.imageHeight - 35, 90, btnHeight)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());

            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Close"),
                btn -> this.onClose())
                .bounds(cX + this.imageWidth - 80, cY + this.imageHeight - 35, 70, btnHeight)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());

            if (currentInfo != null && hasUpdate(currentInfo)) {
                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("Update Now"),
                    btn -> {
                        if (!currentInfo.isSmartStartActive()) {
                            this.smartStartWarningMode = true;
                        } else {
                            this.confirmMode = true;
                        }
                        this.refreshWidgets();
                    })
                    .bounds(cX + this.imageWidth / 2 - 110, cY + this.imageHeight - 65, btnWidth, btnHeight)
                    .style(ModernButton.ButtonStyle.SUCCESS)
                    .build());

                this.addRenderableWidget(new ModernButton.Builder(
                    Component.literal("Skip Version"),
                    btn -> {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.connection.sendCommand("sm update skip");
                            this.onClose();
                        }
                    })
                    .bounds(cX + this.imageWidth / 2 + 10, cY + this.imageHeight - 65, btnWidth, btnHeight)
                    .style(ModernButton.ButtonStyle.SECONDARY)
                    .build());
            }
        }
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.renderBg(g, partialTick, mouseX, mouseY);

        super.renderContent(g, mouseX, mouseY, partialTick);

        int cX = this.leftPos;
        int cY = this.topPos;

        // Title
        g.drawCenteredString(this.font, "Server Updater", cX + this.imageWidth / 2, cY + 15, 0xFFD700);

        if (confirmMode) {
            g.drawCenteredString(this.font, "\u00a7cWARNING: SERVER WILL RESTART", cX + this.imageWidth / 2, cY + 60, 0xFF5555);
            g.drawCenteredString(this.font, "Are you sure you want to apply the update now?", cX + this.imageWidth / 2, cY + 80, 0xFFFFFF);
            g.drawCenteredString(this.font, "All players will be disconnected immediately.", cX + this.imageWidth / 2, cY + 100, 0xAAAAAA);
            if (!currentInfo.isSmartStartActive()) {
                g.drawCenteredString(this.font, "\u00a7eThe server will NOT restart automatically.", cX + this.imageWidth / 2, cY + 120, 0xFFFF55);
                g.drawCenteredString(this.font, "\u00a7eYou must restart it manually.", cX + this.imageWidth / 2, cY + 135, 0xFFFF55);
            }
        } else if (smartStartWarningMode) {
            g.drawCenteredString(this.font, "\u00a7eSmart Start Script not detected!", cX + this.imageWidth / 2, cY + 45, 0xFFFF55);
            g.drawCenteredString(this.font, "To automatically restart after updates,", cX + this.imageWidth / 2, cY + 65, 0xFFFFFF);
            g.drawCenteredString(this.font, "the server needs to use a smart start script.", cX + this.imageWidth / 2, cY + 80, 0xFFFFFF);
            
            g.drawCenteredString(this.font, "\u00a77(If you choose manually, you must restart the server yourself)", cX + this.imageWidth / 2, cY + 110, 0xAAAAAA);
        } else {
            if (currentInfo == null) {
                String dots = switch ((this.tickCount / 10) % 4) {
                    case 0 -> "";
                    case 1 -> ".";
                    case 2 -> "..";
                    default -> "...";
                };
                g.drawCenteredString(this.font, "Checking for updates" + dots, cX + this.imageWidth / 2, cY + 80, 0xAAAAAA);
            } else if (!hasUpdate(currentInfo)) {
                g.drawCenteredString(this.font, "\u00a7aServer is up to date!", cX + this.imageWidth / 2, cY + 80, 0x55FF55);
            } else {
                g.drawCenteredString(this.font, "\u00a7aUpdate Available!", cX + this.imageWidth / 2, cY + 40, 0x55FF55);
                g.drawString(this.font, "Version: \u00a7e" + getTarget(currentInfo).version(), cX + 20, cY + 70, 0xFFFFFF, false);
                g.drawString(this.font, "Date: \u00a77" + getTarget(currentInfo).releaseDate(), cX + 20, cY + 85, 0xFFFFFF, false);

                // Render changelog (limit to a few lines)
                g.drawString(this.font, "Changelog:", cX + 20, cY + 105, 0xAAAAAA, false);
                String[] changelogLines = getTarget(currentInfo).changelog().split("\n");
                int maxChangelogWidth = this.imageWidth - 60;
                for (int i = 0; i < Math.min(3, changelogLines.length); i++) {
                    String line = changelogLines[i];
                    boolean truncated = false;
                    while (this.font.width(line + "...") > maxChangelogWidth && line.length() > 3) {
                        line = line.substring(0, line.length() - 1);
                        truncated = true;
                    }
                    if (truncated) {
                        line += "...";
                    }
                    g.drawString(this.font, line, cX + 30, cY + 120 + (i * 12), 0xFFFFFF, false);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xE0101010);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 35, 0xFF1A1A2E);
        g.fill(this.leftPos, this.topPos + 35, this.leftPos + this.imageWidth, this.topPos + 36, 0xFF333333);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}

