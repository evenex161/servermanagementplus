package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.gui.WorldDetailMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.gui.widgets.ToggleSwitch;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.WMSetTimerPacket;
import com.servermanagement.network.packet.WMToggleChatIsolationPacket;
import com.servermanagement.network.packet.WMTogglePortalsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern World Detail GUI with proper state management
 */
public class WorldDetailScreen extends ScalableContainerScreen<WorldDetailMenu> {
    
    private String dimensionId;
    private ToggleSwitch netherPortalsSwitch;
    private ToggleSwitch endPortalsSwitch;
    private ToggleSwitch chatSwitch;
    private EditBox timerInput;
    private String selectedPortalType = "both"; // "nether", "end", or "both"
    private ModernButton portalTypeButton;

    // Live countdown state ÔÇö anchored at init() from the SyncWorldDetailPacket
    // snapshot, then decremented locally via the player's tickCount.
    private int syncedTimerSeconds = 0;
    private long syncedAtTick = 0L;
    private boolean refreshRequested = false;
    
    public WorldDetailScreen(WorldDetailMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 380, 280);
        this.imageHeight = 280;
        this.imageWidth = 380;
        
        // Get dimension ID from cached data
        this.dimensionId = ClientPacketHandler.getCachedDimensionId();
    }
    
    @Override
    protected void init() {
        super.init();
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int leftCol = centerX + 20;
        int rightCol = centerX + this.imageWidth - 65;
        int startY = centerY + 38;
        int spacing = 32;
        
        // Get FRESH state from cache each time
        boolean netherEnabled = ClientPacketHandler.isNetherPortalsEnabled();
        boolean endEnabled = ClientPacketHandler.isEndPortalsEnabled();
        boolean chatConnected = ClientPacketHandler.isChatConnected();
        boolean hasTimer = ClientPacketHandler.hasTimer();
        int timerSeconds = ClientPacketHandler.getTimerSeconds();

        // Anchor the live countdown to this init() pass. SyncWorldDetailPacket
        // delivered the remaining seconds; pair with player.tickCount so render()
        // can compute a smooth real-time decrement without further packets.
        this.syncedTimerSeconds = hasTimer ? timerSeconds : 0;
        this.syncedAtTick = (minecraft != null && minecraft.player != null) ? minecraft.player.tickCount : 0L;
        this.refreshRequested = false;
        
        // Determine which portal toggles to show based on dimension
        boolean showNether = !dimensionId.equals("minecraft:the_end");
        boolean showEnd = !dimensionId.equals("minecraft:the_nether");
        
        int currentY = startY;
        
        // Nether portal toggle
        if (showNether) {
            this.netherPortalsSwitch = new ToggleSwitch(
                rightCol, currentY + 3,
                Component.literal("Nether Portals"),
                netherEnabled,
                (newState) -> {
                    long clientTick = minecraft.player.tickCount;
                    ModNetworking.sendToServer(new WMTogglePortalsPacket(dimensionId, newState, "nether", clientTick));
                    ClientPacketHandler.handleWorldDetail(dimensionId, newState,
                        ClientPacketHandler.isEndPortalsEnabled(),
                        ClientPacketHandler.hasTimer(), ClientPacketHandler.getTimerSeconds(),
                        ClientPacketHandler.isChatConnected(), ClientPacketHandler.getTimerPortalType());
                }
            );
            this.addRenderableWidget(this.netherPortalsSwitch);
            currentY += spacing;
        }
        
        // End portal toggle
        if (showEnd) {
            this.endPortalsSwitch = new ToggleSwitch(
                rightCol, currentY + 3,
                Component.literal("End Portals"),
                endEnabled,
                (newState) -> {
                    long clientTick = minecraft.player.tickCount;
                    ModNetworking.sendToServer(new WMTogglePortalsPacket(dimensionId, newState, "end", clientTick));
                    ClientPacketHandler.handleWorldDetail(dimensionId,
                        ClientPacketHandler.isNetherPortalsEnabled(), newState,
                        ClientPacketHandler.hasTimer(), ClientPacketHandler.getTimerSeconds(),
                        ClientPacketHandler.isChatConnected(), ClientPacketHandler.getTimerPortalType());
                }
            );
            this.addRenderableWidget(this.endPortalsSwitch);
            currentY += spacing;
        }
        
        // Chat connection toggle
        this.chatSwitch = new ToggleSwitch(
            rightCol, currentY + 3,
            Component.literal("Chat"),
            chatConnected,
            (newState) -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMToggleChatIsolationPacket(dimensionId, newState, clientTick));
                ClientPacketHandler.handleWorldDetail(dimensionId,
                    ClientPacketHandler.isNetherPortalsEnabled(),
                    ClientPacketHandler.isEndPortalsEnabled(),
                    ClientPacketHandler.hasTimer(), ClientPacketHandler.getTimerSeconds(),
                    newState, ClientPacketHandler.getTimerPortalType());
            }
        );
        this.addRenderableWidget(this.chatSwitch);
        currentY += spacing + 10;
        
        // Portal type selector for timer (only if dimension has multiple portal types)
        if (showNether && showEnd) {
            this.selectedPortalType = "both";
            this.portalTypeButton = new ModernButton.Builder(
                Component.literal("Type: Both"),
                button -> {
                    if ("both".equals(selectedPortalType)) {
                        selectedPortalType = "nether";
                        button.setMessage(Component.literal("Type: Nether"));
                    } else if ("nether".equals(selectedPortalType)) {
                        selectedPortalType = "end";
                        button.setMessage(Component.literal("Type: End"));
                    } else {
                        selectedPortalType = "both";
                        button.setMessage(Component.literal("Type: Both"));
                    }
                })
                .bounds(leftCol, currentY, 80, 22)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build();
            this.addRenderableWidget(this.portalTypeButton);
        } else {
            this.selectedPortalType = showNether ? "nether" : "end";
        }
        
        // Timer input ÔÇö locked while a timer is running so the user isn't confused
        // by the live remaining-seconds value (the live countdown is rendered separately).
        int timerInputX = (showNether && showEnd) ? leftCol + 85 : leftCol;
        int timerInputWidth = (showNether && showEnd) ? 55 : 80;
        this.timerInput = new EditBox(this.font, timerInputX, currentY, timerInputWidth, 22, Component.literal("Seconds"));
        this.timerInput.setMaxLength(6);
        if (hasTimer) {
            this.timerInput.setValue("");
            this.timerInput.setHint(Component.literal("running"));
            this.timerInput.setEditable(false);
        } else {
            this.timerInput.setValue("60");
            this.timerInput.setEditable(true);
        }
        this.addRenderableWidget(this.timerInput);
        
        // Set Timer button ÔÇö only available when no timer is running.
        int btnX = timerInputX + timerInputWidth + 5;
        if (!hasTimer) {
            this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Set Timer"),
                button -> {
                    try {
                        int seconds = Integer.parseInt(this.timerInput.getValue());
                        if (seconds > 0) {
                            long clientTick = minecraft.player.tickCount;
                            ModNetworking.sendToServer(new WMSetTimerPacket(dimensionId, seconds, selectedPortalType, clientTick));
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input, ignore
                    }
                })
                .bounds(btnX, currentY, 70, 22)
                .style(ModernButton.ButtonStyle.SUCCESS)
                .build());
        }
        
        // Clear Timer button ÔÇö always available so admins can cancel a running timer.
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Clear"),
            button -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMSetTimerPacket(dimensionId, 0, selectedPortalType, clientTick));
            })
            .bounds(btnX + 75, currentY, 55, 22)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        currentY += 40;
        
        // Back and Close buttons - symmetrical, equal width
        int btnW = (this.imageWidth - 30) / 2;
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("ÔåÉ Back"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_LIST, "")))
            .bounds(centerX + 10, currentY, btnW, 26)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + this.imageWidth - btnW - 10, currentY, btnW, 26)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + this.imageHeight, 0xE0101010);
        
        // Header bar (opaque dark)
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + 25, 0xFF1A1A2E);
        // Separator line
        guiGraphics.fill(this.leftPos, this.topPos + 25, this.leftPos + this.imageWidth, 
                        this.topPos + 26, 0xFF333333);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        String dimName = this.dimensionId.replace("minecraft:", "");
        dimName = dimName.substring(0, 1).toUpperCase() + dimName.substring(1).replace("_", " ");
        guiGraphics.drawString(this.font, dimName, 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        int leftCol = this.leftPos + 20;
        int startY = this.topPos + 38;
        int spacing = 32;
        
        boolean showNether = !dimensionId.equals("minecraft:the_end");
        boolean showEnd = !dimensionId.equals("minecraft:the_nether");
        
        int currentY = startY;
        
        // Nether portal labels
        if (showNether) {
            guiGraphics.drawString(this.font, "Nether Portals", 
                leftCol, currentY + 6, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Enable/disable nether portal travel", 
                leftCol, currentY + 16, 0xAAAAAA, true);
            currentY += spacing;
        }
        
        // End portal labels
        if (showEnd) {
            guiGraphics.drawString(this.font, "End Portals", 
                leftCol, currentY + 6, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, "Enable/disable end portal travel", 
                leftCol, currentY + 16, 0xAAAAAA, true);
            currentY += spacing;
        }
        
        // Chat labels
        guiGraphics.drawString(this.font, "Chat", 
            leftCol, currentY + 6, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Connect to global chat", 
            leftCol, currentY + 16, 0xAAAAAA, true);
        currentY += spacing + 10;
        
        // Timer section header - drawn ABOVE the controls
        guiGraphics.drawString(this.font, "Portal Timer", 
            leftCol, currentY - 12, 0xFFFFFF, true);

        // Live countdown ÔÇö sits between the timer controls (top of currentY,
        // 22 px tall) and the Back/Close button row (currentY + 40). Updates
        // every render frame from the local tick clock; auto-requests a fresh
        // SyncWorldDetailPacket once the countdown reaches zero so the toggle
        // states reflect the new portal status.
        if (this.syncedTimerSeconds > 0) {
            int remaining = computeLiveRemaining();
            String type = ClientPacketHandler.getTimerPortalType();
            String typeLabel = "both".equals(type) ? "Both" : ("nether".equals(type) ? "Nether" : "End");
            int color = remaining <= 5  ? 0xFFFF5555
                      : remaining <= 10 ? 0xFFFFAA00
                      : remaining <= 30 ? 0xFFFFFF55
                      :                   0xFF55FF55;
            String text = String.format("Time left: %d:%02d  (%s)",
                    remaining / 60, remaining % 60, typeLabel);
            guiGraphics.drawString(this.font, text, leftCol, currentY + 26, color, true);
            if (remaining == 0 && !this.refreshRequested) {
                this.refreshRequested = true;
                ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_DETAIL, this.dimensionId));
            }
        }
        
        // Render widgets on top
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }

    /**
     * Estimates the remaining timer seconds without waiting for a server packet.
     * Anchored at init() to {@code syncedTimerSeconds} (from SyncWorldDetailPacket)
     * and the player's tickCount; decremented locally at 20 tps.
     */
    private int computeLiveRemaining() {
        if (minecraft == null || minecraft.player == null) return syncedTimerSeconds;
        long elapsed = (minecraft.player.tickCount - syncedAtTick) / 20L;
        return Math.max(0, syncedTimerSeconds - (int) elapsed);
    }
}
