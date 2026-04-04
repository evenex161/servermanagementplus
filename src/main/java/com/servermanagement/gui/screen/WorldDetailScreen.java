package com.servermanagement.gui.screen;

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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Modern World Detail GUI with proper state management
 */
public class WorldDetailScreen extends AbstractContainerScreen<WorldDetailMenu> {
    
    private String dimensionId;
    private ToggleSwitch netherPortalsSwitch;
    private ToggleSwitch endPortalsSwitch;
    private ToggleSwitch chatSwitch;
    private EditBox timerInput;
    private String selectedPortalType = "both"; // "nether", "end", or "both"
    private ModernButton portalTypeButton;
    
    public WorldDetailScreen(WorldDetailMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 230;
        this.imageWidth = 320;
        
        // Get dimension ID from cached data
        this.dimensionId = ClientPacketHandler.getCachedDimensionId();
    }
    
    @Override
    protected void init() {
        super.init();
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int leftCol = centerX + 20;
        int rightCol = centerX + 260;
        int startY = centerY + 45;
        int spacing = 28;
        
        // Get FRESH state from cache each time
        boolean netherEnabled = ClientPacketHandler.isNetherPortalsEnabled();
        boolean endEnabled = ClientPacketHandler.isEndPortalsEnabled();
        boolean chatConnected = ClientPacketHandler.isChatConnected();
        boolean hasTimer = ClientPacketHandler.hasTimer();
        int timerSeconds = ClientPacketHandler.getTimerSeconds();
        
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
                ModNetworking.sendToServer(new WMToggleChatIsolationPacket(newState, clientTick));
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
                    // Cycle through portal types
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
                .bounds(leftCol, currentY, 80, 20)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build();
            this.addRenderableWidget(this.portalTypeButton);
        } else {
            // Single portal type for this dimension
            this.selectedPortalType = showNether ? "nether" : "end";
        }
        
        // Timer input
        int timerInputX = (showNether && showEnd) ? leftCol + 85 : leftCol;
        int timerInputWidth = (showNether && showEnd) ? 65 : 80;
        this.timerInput = new EditBox(this.font, timerInputX, currentY, timerInputWidth, 20, Component.literal("Seconds"));
        this.timerInput.setValue(hasTimer ? String.valueOf(timerSeconds) : "60");
        this.timerInput.setMaxLength(6);
        this.addRenderableWidget(this.timerInput);
        
        // Set Timer button
        int btnX = timerInputX + timerInputWidth + 5;
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
            .bounds(btnX, currentY, 70, 20)
            .style(ModernButton.ButtonStyle.SUCCESS)
            .build());
        
        // Clear Timer button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Clear"),
            button -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMSetTimerPacket(dimensionId, 0, selectedPortalType, clientTick));
                this.timerInput.setValue("60");
            })
            .bounds(btnX + 75, currentY, 55, 20)
            .style(ModernButton.ButtonStyle.DANGER)
            .build());
        
        currentY += 35;
        
        // Back to World List button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("← Back"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.WORLD_LIST, "")))
            .bounds(centerX + 30, currentY, 90, 24)
            .style(ModernButton.ButtonStyle.SECONDARY)
            .build());
        
        // Close button
        this.addRenderableWidget(new ModernButton.Builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + 190, currentY, 100, 24)
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        String dimName = this.dimensionId.replace("minecraft:", "");
        dimName = dimName.substring(0, 1).toUpperCase() + dimName.substring(1).replace("_", " ");
        guiGraphics.drawString(this.font, dimName, 
            this.leftPos + 15, this.topPos + 8, 0xFFD700, true);
        
        int leftCol = this.leftPos + 20;
        int startY = this.topPos + 45;
        int spacing = 28;
        
        boolean showNether = !dimensionId.equals("minecraft:the_end");
        boolean showEnd = !dimensionId.equals("minecraft:the_nether");
        
        int currentY = startY;
        
        // Nether portal labels
        if (showNether) {
            guiGraphics.drawString(this.font, "Nether Portals", 
                leftCol, currentY + 6, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "Enable/disable nether portal travel", 
                leftCol, currentY + 16, 0x808080, false);
            currentY += spacing;
        }
        
        // End portal labels
        if (showEnd) {
            guiGraphics.drawString(this.font, "End Portals", 
                leftCol, currentY + 6, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "Enable/disable end portal travel", 
                leftCol, currentY + 16, 0x808080, false);
            currentY += spacing;
        }
        
        // Chat labels
        guiGraphics.drawString(this.font, "Chat", 
            leftCol, currentY + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "Connect to global chat", 
            leftCol, currentY + 16, 0x808080, false);
        currentY += spacing + 10;
        
        // Timer label
        guiGraphics.drawString(this.font, "Portal Timer", 
            leftCol, currentY + 24, 0xFFFFFF, false);
        
        // Render widgets on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
