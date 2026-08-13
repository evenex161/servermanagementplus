package com.servermanagement.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A reusable dropdown selector widget for Minecraft GUIs.
 * Renders as a bordered box with the current selection and a down-arrow indicator.
 * On click, expands an overlay list with all options.
 * Supports hover highlighting and keyboard navigation.
 */
public class DropdownWidget extends AbstractWidget {

    private final List<String> options = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean expanded = false;
    private int hoveredOptionIndex = -1;
    private Consumer<Integer> onSelectionChanged;

    // Styling constants matching the mod's GUI design language
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int HOVER_COLOR = 0xFF2A2A4E;
    private static final int SELECTED_COLOR = 0xFF3A3A5E;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int GOLD_COLOR = 0xFFFFD700;
    private static final int ARROW_COLOR = 0xFFAAAAAA;
    private static final int DROPDOWN_BG = 0xFF222240;
    private static final int DROPDOWN_BORDER = 0xFF444444;

    private static final int OPTION_HEIGHT = 16;
    private static final int PADDING = 4;

    public DropdownWidget(int x, int y, int width, int height, Component label) {
        super(x, y, width, height, label);
    }

    public void setOptions(List<String> options) {
        this.options.clear();
        this.options.addAll(options);
        if (selectedIndex >= this.options.size()) {
            selectedIndex = 0;
        }
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedValue() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return "";
    }

    public void setOnSelectionChanged(Consumer<Integer> callback) {
        this.onSelectionChanged = callback;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Main dropdown button background
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), BG_COLOR);

        // Border
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, BORDER_COLOR);
        guiGraphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), BORDER_COLOR);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), BORDER_COLOR);
        guiGraphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), BORDER_COLOR);

        // Hover highlight for main button
        if (isHoveredOrFocused() && !expanded) {
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, HOVER_COLOR);
        }

        // Selected text
        String displayText = getSelectedValue();
        if (displayText.isEmpty()) {
            displayText = getMessage().getString();
        }

        // Truncate if too long
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int maxTextWidth = getWidth() - 20; // Leave room for arrow
        if (font.width(displayText) > maxTextWidth) {
            while (font.width(displayText + "...") > maxTextWidth && displayText.length() > 0) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += "...";
        }

        guiGraphics.drawString(font, displayText,
            getX() + PADDING + 2, getY() + (getHeight() - 8) / 2,
            expanded ? GOLD_COLOR : TEXT_COLOR, false);

        // Down arrow indicator (▼)
        String arrow = expanded ? "\u25B2" : "\u25BC";
        guiGraphics.drawString(font, arrow,
            getX() + getWidth() - 12, getY() + (getHeight() - 8) / 2,
            ARROW_COLOR, false);

        // Render expanded dropdown overlay
        if (expanded && !options.isEmpty()) {
            renderDropdown(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int dropdownHeight = options.size() * OPTION_HEIGHT + 2;
        int dropdownY = getY() + getHeight();

        // Push pose for z-ordering above other elements
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);

        // Dropdown background
        guiGraphics.fill(getX(), dropdownY, getX() + getWidth(), dropdownY + dropdownHeight, DROPDOWN_BG);

        // Dropdown border
        guiGraphics.fill(getX(), dropdownY, getX() + getWidth(), dropdownY + 1, DROPDOWN_BORDER);
        guiGraphics.fill(getX(), dropdownY + dropdownHeight - 1, getX() + getWidth(), dropdownY + dropdownHeight, DROPDOWN_BORDER);
        guiGraphics.fill(getX(), dropdownY, getX() + 1, dropdownY + dropdownHeight, DROPDOWN_BORDER);
        guiGraphics.fill(getX() + getWidth() - 1, dropdownY, getX() + getWidth(), dropdownY + dropdownHeight, DROPDOWN_BORDER);

        // Render each option
        hoveredOptionIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            int optionY = dropdownY + 1 + i * OPTION_HEIGHT;

            // Check if hovered
            boolean isHovered = mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            boolean isSelected = i == selectedIndex;

            if (isHovered) {
                hoveredOptionIndex = i;
                guiGraphics.fill(getX() + 1, optionY, getX() + getWidth() - 1, optionY + OPTION_HEIGHT, HOVER_COLOR);
            } else if (isSelected) {
                guiGraphics.fill(getX() + 1, optionY, getX() + getWidth() - 1, optionY + OPTION_HEIGHT, SELECTED_COLOR);
            }

            // Option text
            String text = options.get(i);
            int maxTextWidth = getWidth() - 10;
            if (font.width(text) > maxTextWidth) {
                while (font.width(text + "...") > maxTextWidth && text.length() > 0) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }

            int textColor = isSelected ? GOLD_COLOR : (isHovered ? TEXT_COLOR : 0xFFCCCCCC);
            guiGraphics.drawString(font, text,
                getX() + PADDING + 2, optionY + (OPTION_HEIGHT - 8) / 2,
                textColor, false);

            // Gold indicator for selected item
            if (isSelected) {
                guiGraphics.drawString(font, "\u2713",
                    getX() + getWidth() - 14, optionY + (OPTION_HEIGHT - 8) / 2,
                    GOLD_COLOR, false);
            }
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) return false;

        if (expanded) {
            // Check if clicked on an option
            if (hoveredOptionIndex >= 0 && hoveredOptionIndex < options.size()) {
                int oldIndex = selectedIndex;
                selectedIndex = hoveredOptionIndex;
                expanded = false;
                if (oldIndex != selectedIndex && onSelectionChanged != null) {
                    onSelectionChanged.accept(selectedIndex);
                }
                return true;
            }

            // Check if clicked on main button (toggle close)
            if (isMouseOver(mouseX, mouseY)) {
                expanded = false;
                return true;
            }

            // Clicked outside — close
            expanded = false;
            return false;
        } else {
            // Open dropdown
            if (isMouseOver(mouseX, mouseY)) {
                expanded = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Check if mouse is over the dropdown overlay area (not just the button).
     * Used by parent screens to determine if clicks should be intercepted.
     */
    public boolean isMouseOverDropdown(double mouseX, double mouseY) {
        if (!expanded) return false;
        int dropdownHeight = options.size() * OPTION_HEIGHT + 2;
        int dropdownY = getY() + getHeight();
        return mouseX >= getX() && mouseX < getX() + getWidth()
            && mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (expanded) {
            // Include dropdown area in mouse-over check
            int dropdownHeight = options.size() * OPTION_HEIGHT + 2;
            return (mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight() + dropdownHeight);
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!expanded || options.isEmpty()) return false;

        // Arrow keys for navigation
        if (keyCode == 264) { // DOWN
            selectedIndex = Math.min(selectedIndex + 1, options.size() - 1);
            if (onSelectionChanged != null) onSelectionChanged.accept(selectedIndex);
            return true;
        } else if (keyCode == 265) { // UP
            selectedIndex = Math.max(selectedIndex - 1, 0);
            if (onSelectionChanged != null) onSelectionChanged.accept(selectedIndex);
            return true;
        } else if (keyCode == 257 || keyCode == 335) { // ENTER / NUMPAD_ENTER
            expanded = false;
            return true;
        } else if (keyCode == 256) { // ESCAPE
            expanded = false;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    /**
     * Close the dropdown if it's open. Call this from parent screens
     * when focus shifts to another widget.
     */
    public void closeDropdown() {
        expanded = false;
    }
}
