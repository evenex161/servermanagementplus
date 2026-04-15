package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Console output display widget with vertical scrolling, horizontal scrolling, and scissor clipping
 */
public class ConsoleOutput extends AbstractWidget {
    
    private final List<String> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int horizontalScroll = 0;
    private int maxVisibleLines;
    private static final int LINE_HEIGHT = 10;
    private static final int H_SCROLL_STEP = 40;
    
    public ConsoleOutput(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.maxVisibleLines = (height - 4) / LINE_HEIGHT;
    }
    
    public void addLine(String line) {
        lines.add(line);
        // Auto-scroll to bottom when new line added
        scrollOffset = Math.max(0, lines.size() - maxVisibleLines);
    }
    
    public void clear() {
        lines.clear();
        scrollOffset = 0;
        horizontalScroll = 0;
    }
    
    public void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - 1);
    }
    
    public void scrollDown() {
        scrollOffset = Math.min(Math.max(0, lines.size() - maxVisibleLines), scrollOffset + 1);
    }
    
    public void scrollLeft() {
        horizontalScroll = Math.max(0, horizontalScroll - H_SCROLL_STEP);
    }
    
    public void scrollRight() {
        horizontalScroll += H_SCROLL_STEP;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        
        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.width;
        int y1 = y0 + this.height;
        
        // Background
        guiGraphics.fill(x0, y0, x1, y1, 0xE0000000);
        
        // Border
        guiGraphics.fill(x0, y0, x1, y0 + 1, 0x80FFFFFF);
        guiGraphics.fill(x0, y1 - 1, x1, y1, 0x80FFFFFF);
        guiGraphics.fill(x0, y0, x0 + 1, y1, 0x80FFFFFF);
        guiGraphics.fill(x1 - 1, y0, x1, y1, 0x80FFFFFF);
        
        // Enable scissor to clip text within bounds
        guiGraphics.enableScissor(x0 + 2, y0 + 2, x1 - 8, y1 - 2);
        
        // Render visible lines
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = y0 + 2;
        int endIndex = Math.min(scrollOffset + maxVisibleLines, lines.size());
        
        for (int i = scrollOffset; i < endIndex; i++) {
            String line = lines.get(i);
            int color = getColorForLine(line);
            guiGraphics.drawString(font, line, x0 + 4 - horizontalScroll, textY, color, false);
            textY += LINE_HEIGHT;
        }
        
        guiGraphics.disableScissor();
        
        // Vertical scrollbar
        if (lines.size() > maxVisibleLines) {
            int scrollbarHeight = this.height - 4;
            int thumbHeight = Math.max(20, scrollbarHeight * maxVisibleLines / lines.size());
            int thumbY = (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, lines.size() - maxVisibleLines);
            
            guiGraphics.fill(x1 - 6, y0 + 2 + thumbY,
                           x1 - 2, y0 + 2 + thumbY + thumbHeight,
                           0x80FFFFFF);
        }
        
        // Horizontal scroll indicator
        if (horizontalScroll > 0) {
            guiGraphics.fill(x0 + 2, y1 - 4, x0 + 20, y1 - 2, 0x60FFFFFF);
        }
    }
    
    private int getColorForLine(String line) {
        if (line.startsWith(">")) {
            return 0x55FF55; // Green for user input
        } else if (line.contains("[ERROR]") || line.contains("ERROR")) {
            return 0xFF5555; // Red
        } else if (line.contains("[WARN]") || line.contains("WARN")) {
            return 0xFFAA00; // Yellow/Orange
        } else if (line.contains("[DEBUG]") || line.contains("DEBUG")) {
            return 0xAAAAAA; // Gray
        }
        return 0xCCCCCC; // Light gray default
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHoveredOrFocused()) {
            // Shift+scroll = horizontal scroll
            boolean shiftHeld = net.minecraft.client.Minecraft.getInstance().getWindow() != null &&
                org.lwjgl.glfw.GLFW.glfwGetKey(
                    net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(),
                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            
            if (shiftHeld) {
                if (scrollY > 0) {
                    scrollLeft();
                } else if (scrollY < 0) {
                    scrollRight();
                }
            } else {
                if (scrollY > 0) {
                    scrollUp();
                } else if (scrollY < 0) {
                    scrollDown();
                }
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
