package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.gui.PortalTimerMenu;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.WMSetTimerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PortalTimerScreen extends ScalableContainerScreen<PortalTimerMenu> {
    
    private String dimensionId;
    private EditBox hoursInput;
    private EditBox minutesInput;
    private EditBox secondsInput;
    private Button startButton;
    private Button stopButton;
    private Button preset5min;
    private Button preset10min;
    private Button preset30min;
    private Button preset1hour;
    private Button portalTypeButton;
    private String selectedPortalType = "both";
    
    public PortalTimerScreen(PortalTimerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 300, 240);
        this.imageHeight = 240;
        this.imageWidth = 300;

        // See WorldDetailScreen: SyncWorldDetailPacket may not be processed by
        // the time the screen is constructed under Fabric. Default to empty
        // and refresh lazily in render() to ensure correct portal toggles.
        String cached = ClientPacketHandler.getCachedDimensionId();
        this.dimensionId = cached == null ? "" : cached;
    }
    
    @Override
    protected void init() {
        super.init();
        // Refresh dimensionId from the cache on every init() in case the
        // SyncWorldDetailPacket arrived after the screen ctor but before the
        // first render — keeps portal-toggle visibility correct.
        String cachedDim = ClientPacketHandler.getCachedDimensionId();
        if (cachedDim != null && !cachedDim.isEmpty()) {
            this.dimensionId = cachedDim;
        }
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        int startY = centerY + 40;
        
        // Portal type selector
        boolean showNether = !dimensionId.equals("minecraft:the_end");
        boolean showEnd = !dimensionId.equals("minecraft:the_nether");
        
        if (showNether && showEnd) {
            this.selectedPortalType = "both";
            this.portalTypeButton = Button.builder(
                Component.literal("§bPortal Type: Both"),
                button -> {
                    if ("both".equals(selectedPortalType)) {
                        selectedPortalType = "nether";
                        button.setMessage(Component.literal("§bPortal Type: Nether"));
                    } else if ("nether".equals(selectedPortalType)) {
                        selectedPortalType = "end";
                        button.setMessage(Component.literal("§bPortal Type: End"));
                    } else {
                        selectedPortalType = "both";
                        button.setMessage(Component.literal("§bPortal Type: Both"));
                    }
                })
                .bounds(centerX + 30, startY - 10, 240, 20)
                .build();
            this.addRenderableWidget(this.portalTypeButton);
            startY += 15;
        } else {
            this.selectedPortalType = showNether ? "nether" : "end";
        }
        
        // Timer inputs
        int inputY = startY + 10;
        
        // Hours input
        this.hoursInput = new EditBox(this.font, centerX + 30, inputY, 50, 18, Component.literal("Hours"));
        this.hoursInput.setValue("0");
        this.hoursInput.setMaxLength(3);
        this.hoursInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(this.hoursInput);
        
        // Minutes input
        this.minutesInput = new EditBox(this.font, centerX + 105, inputY, 50, 18, Component.literal("Minutes"));
        this.minutesInput.setValue("0");
        this.minutesInput.setMaxLength(2);
        this.minutesInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(this.minutesInput);
        
        // Seconds input
        this.secondsInput = new EditBox(this.font, centerX + 180, inputY, 50, 18, Component.literal("Seconds"));
        this.secondsInput.setValue("60");
        this.secondsInput.setMaxLength(2);
        this.secondsInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(this.secondsInput);
        
        startY += 40;
        
        // Start button
        this.startButton = Button.builder(
            Component.literal("§aStart Timer"),
            button -> {
                try {
                    int hours = Integer.parseInt(this.hoursInput.getValue());
                    int minutes = Integer.parseInt(this.minutesInput.getValue());
                    int seconds = Integer.parseInt(this.secondsInput.getValue());
                    int totalSeconds = (hours * 3600) + (minutes * 60) + seconds;
                    
                    if (totalSeconds > 0) {
                        long clientTick = minecraft.player.tickCount;
                        ModNetworking.sendToServer(new WMSetTimerPacket(dimensionId, totalSeconds, selectedPortalType, clientTick));
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
            })
            .bounds(centerX + 30, startY, 100, 20)
            .build();
        this.addRenderableWidget(this.startButton);
        
        // Stop/Cancel button
        this.stopButton = Button.builder(
            Component.literal("§cCancel"),
            button -> {
                long clientTick = minecraft.player.tickCount;
                ModNetworking.sendToServer(new WMSetTimerPacket(dimensionId, 0, selectedPortalType, clientTick));
            })
            .bounds(centerX + 140, startY, 100, 20)
            .build();
        this.addRenderableWidget(this.stopButton);
        
        startY += 35;
        
        // Preset buttons
        this.preset5min = Button.builder(
            Component.literal("5 min"),
            button -> setPresetTime(0, 5, 0))
            .bounds(centerX + 30, startY, 55, 20)
            .build();
        this.addRenderableWidget(this.preset5min);
        
        this.preset10min = Button.builder(
            Component.literal("10 min"),
            button -> setPresetTime(0, 10, 0))
            .bounds(centerX + 90, startY, 55, 20)
            .build();
        this.addRenderableWidget(this.preset10min);
        
        this.preset30min = Button.builder(
            Component.literal("30 min"),
            button -> setPresetTime(0, 30, 0))
            .bounds(centerX + 150, startY, 55, 20)
            .build();
        this.addRenderableWidget(this.preset30min);
        
        this.preset1hour = Button.builder(
            Component.literal("1 hour"),
            button -> setPresetTime(1, 0, 0))
            .bounds(centerX + 210, startY, 55, 20)
            .build();
        this.addRenderableWidget(this.preset1hour);
        
        startY += 40;
        
        // Close button
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX + 100, startY, 100, 20)
            .build());
    }
    
    private void setPresetTime(int hours, int minutes, int seconds) {
        this.hoursInput.setValue(String.valueOf(hours));
        this.minutesInput.setValue(String.valueOf(minutes));
        this.secondsInput.setValue(String.valueOf(seconds));
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, 
                        this.topPos + this.imageHeight, 0xC0101010);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Late-arrival sync recovery: if the dimension id wasn't available at
        // construction time, pick it up now and rebuild widgets so the portal
        // type selector reflects the actual dimension.
        if (this.dimensionId == null || this.dimensionId.isEmpty()) {
            String cached = ClientPacketHandler.getCachedDimensionId();
            if (cached != null && !cached.isEmpty()) {
                this.dimensionId = cached;
                this.rebuildWidgets();
            }
        }

        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Title
        String title = "Portal Timer Configuration";
        guiGraphics.drawString(this.font, title, 
            this.leftPos + (this.imageWidth - this.font.width(title)) / 2, 
            this.topPos + 10, 0xFFFFFF, false);
        
        // Labels
        int labelX = this.leftPos + 30;
        int labelY = this.topPos + 53;
        
        guiGraphics.drawString(this.font, "H", labelX + 20, labelY, 0xAAAAAA, true);
        guiGraphics.drawString(this.font, "M", labelX + 95, labelY, 0xAAAAAA, true);
        guiGraphics.drawString(this.font, "S", labelX + 170, labelY, 0xAAAAAA, true);
        
        // Current timer status
        boolean hasTimer = ClientPacketHandler.hasTimer();
        int timerSeconds = ClientPacketHandler.getTimerSeconds();
        
        if (hasTimer && timerSeconds > 0) {
            int hours = timerSeconds / 3600;
            int minutes = (timerSeconds % 3600) / 60;
            int seconds = timerSeconds % 60;
            
            String statusLabel = "Current Timer:";
            String timeDisplay = String.format("§e%d:%02d:%02d", hours, minutes, seconds);
            
            int statusY = this.topPos + 165;
            guiGraphics.drawString(this.font, statusLabel, 
                this.leftPos + 30, statusY, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, timeDisplay, 
                this.leftPos + 130, statusY, 0xFFFFFF, false);
        } else {
            String noTimer = "No active timer";
            int statusY = this.topPos + 165;
            guiGraphics.drawString(this.font, noTimer, 
                this.leftPos + (this.imageWidth - this.font.width(noTimer)) / 2, 
                statusY, 0x888888, false);
        }
        
        // Presets label
        guiGraphics.drawString(this.font, "Quick Presets:", 
            this.leftPos + 30, this.topPos + 115, 0xFFFFFF, false);
        
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
