package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Console output display widget with scrolling
 */
public class ConsoleOutput extends AbstractWidget {
    
    private final List<String> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxVisibleLines;
    
    public ConsoleOutput(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.maxVisibleLines = (height - 4) / 10; // 10px per line with padding
    }
    
    public void addLine(String line) {
        lines.add(line);
        // Auto-scroll to bottom when new line added
        scrollOffset = Math.max(0, lines.size() - maxVisibleLines);
    }
    
    public void clear() {
        lines.clear();
        scrollOffset = 0;
    }
    
    public void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - 1);
    }
    
    public void scrollDown() {
        scrollOffset = Math.min(Math.max(0, lines.size() - maxVisibleLines), scrollOffset + 1);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        // Background
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 
                        0xE0000000);
        
        // Border
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0x80FFFFFF);
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0x80FFFFFF);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, 0x80FFFFFF);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, 0x80FFFFFF);
        
        // Render visible lines
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int y = this.getY() + 2;
        int endIndex = Math.min(scrollOffset + maxVisibleLines, lines.size());
        
        for (int i = scrollOffset; i < endIndex; i++) {
            String line = lines.get(i);
            // Color code based on log level
            int color = getColorForLine(line);
            guiGraphics.drawString(font, line, this.getX() + 4, y, color, false);
            y += 10;
        }
        
        // Scrollbar indicator if needed
        if (lines.size() > maxVisibleLines) {
            int scrollbarHeight = this.height - 4;
            int thumbHeight = Math.max(20, scrollbarHeight * maxVisibleLines / lines.size());
            int thumbY = (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, lines.size() - maxVisibleLines);
            
            guiGraphics.fill(this.getX() + this.width - 6, this.getY() + 2 + thumbY,
                           this.getX() + this.width - 2, this.getY() + 2 + thumbY + thumbHeight,
                           0x80FFFFFF);
        }
    }
    
    private int getColorForLine(String line) {
        if (line.contains("[ERROR]") || line.contains("ERROR")) {
            return 0xFF5555; // Red
        } else if (line.contains("[WARN]") || line.contains("WARN")) {
            return 0xFFAA00; // Yellow
        } else if (line.contains("[INFO]") || line.contains("INFO")) {
            return 0xFFFFFF; // White
        } else if (line.contains("[DEBUG]") || line.contains("DEBUG")) {
            return 0xAAAAAA; // Gray
        }
        return 0xCCCCCC; // Light gray default
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHoveredOrFocused()) {
            if (scrollY > 0) {
                scrollUp();
            } else if (scrollY < 0) {
                scrollDown();
            }
            return true;
        }
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, "Console Output");
    }
}
