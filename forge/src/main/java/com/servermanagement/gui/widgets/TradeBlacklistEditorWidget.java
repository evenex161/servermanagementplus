package com.servermanagement.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.gui.economy.ConfirmDeleteScreen;
import com.servermanagement.client.ClientConfig;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TradeBlacklistEditorWidget extends AbstractWidget {

    private final EditBox searchBox;
    private final List<String> blacklistedItems = new ArrayList<>();
    private final Set<Integer> selectedIndices = new HashSet<>();
    
    private boolean isPendingSearchBoxFocus = false;
    
    // Autocomplete state
    private final List<Item> autocompleteSuggestions = new ArrayList<>();
    private int selectedSuggestionIndex = -1;
    
    // Hold-to-delete state
    private int holdingIndex = -1;
    private int holdTicks = 0;
    private final int MAX_HOLD_TICKS = 40; // 2 seconds at 20 TPS

    // Scroll state
    private int scrollOffset = 0;
    private static final int VISIBLE_ITEMS = 4;
    private static final int ITEM_HEIGHT = 24;

    public TradeBlacklistEditorWidget(int x, int y, int width, int height, String initialBlacklist) {
        super(x, y, width, height, Component.empty());
        
        if (initialBlacklist != null && !initialBlacklist.trim().isEmpty()) {
            String[] items = initialBlacklist.split(",");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    blacklistedItems.add(trimmed);
                }
            }
        }
        
        this.searchBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("Search Item"));
        this.searchBox.setHint(Component.literal("Search item name or ID..."));
        this.searchBox.setMaxLength(32767);
        this.searchBox.setResponder(this::updateSuggestions);
    }
    
    public String getBlacklistString() {
        return String.join(",", blacklistedItems);
    }
    
    private void updateSuggestions(String text) {
        autocompleteSuggestions.clear();
        selectedSuggestionIndex = -1;
        if (text.isEmpty()) return;
        
        String lowerText = text.toLowerCase();
        
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key == null) continue;
            
            String id = key.toString().toLowerCase();
            String name = Component.translatable(item.getDescriptionId()).getString().toLowerCase();
            
            if (id.contains(lowerText) || name.contains(lowerText)) {
                autocompleteSuggestions.add(item);
                if (autocompleteSuggestions.size() >= 5) break; // Limit suggestions
            }
        }
        
        if (!autocompleteSuggestions.isEmpty()) {
            selectedSuggestionIndex = 0;
        }
    }
    
    private void addBlacklistedItem(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key != null) {
            String id = key.toString();
            if (!blacklistedItems.contains(id)) {
                blacklistedItems.add(id);
            }
        }
        searchBox.setValue("");
        autocompleteSuggestions.clear();
        selectedSuggestionIndex = -1;
    }
    
    public void tick() {
        // Handle hold-to-delete logic
        if (holdingIndex >= 0 && holdingIndex < blacklistedItems.size()) {
            holdTicks++;
            if (holdTicks >= MAX_HOLD_TICKS) {
                // Delete selected items
                if (selectedIndices.contains(holdingIndex)) {
                    List<String> toRemove = new ArrayList<>();
                    for (int idx : selectedIndices) {
                        if (idx >= 0 && idx < blacklistedItems.size()) {
                            toRemove.add(blacklistedItems.get(idx));
                        }
                    }
                    blacklistedItems.removeAll(toRemove);
                    selectedIndices.clear();
                } else {
                    blacklistedItems.remove(holdingIndex);
                }
                holdingIndex = -1;
                holdTicks = 0;
            }
        } else {
            holdTicks = Math.max(0, holdTicks - 4); // Fast decay if let go
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.searchBox.setFocused(false);
        } else if (this.isPendingSearchBoxFocus) {
            this.searchBox.setFocused(true);
            this.isPendingSearchBoxFocus = false;
        }
    }

    public boolean isSearchBoxFocused() {
        return this.searchBox != null && this.searchBox.isFocused();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.isPendingSearchBoxFocus = true;
            return true;
        }
        
        // Clicked autocomplete suggestion?
        if (!autocompleteSuggestions.isEmpty() && searchBox.isFocused()) {
            int suggestY = this.getY() + 22;
            for (int i = 0; i < autocompleteSuggestions.size(); i++) {
                if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
                    mouseY >= suggestY && mouseY < suggestY + 16) {
                    if (button == 0) { // Left click
                        addBlacklistedItem(autocompleteSuggestions.get(i));
                        this.isPendingSearchBoxFocus = true;
                        return true;
                    }
                }
                suggestY += 16;
            }
        }
        
        // Clicked blacklisted item list?
        int listY = this.getY() + 25;
        if (autocompleteSuggestions.isEmpty()) {
            for (int i = 0; i < Math.min(VISIBLE_ITEMS, blacklistedItems.size() - scrollOffset); i++) {
                int idx = i + scrollOffset;
                int itemY = listY + i * ITEM_HEIGHT;
                if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
                    mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    
                    if (button == 0) { // Left click
                        boolean ctrlDown = net.minecraft.client.gui.screens.Screen.hasControlDown();
                        if (ctrlDown) {
                            if (selectedIndices.contains(idx)) {
                                selectedIndices.remove(idx);
                            } else {
                                selectedIndices.add(idx);
                            }
                        } else {
                            selectedIndices.clear();
                            selectedIndices.add(idx);
                        }
                        this.setFocused(true);
                        this.searchBox.setFocused(false);
                        return true;
                    } else if (button == 1) { // Right click hold
                        holdingIndex = idx;
                        holdTicks = 0;
                        this.setFocused(true);
                        this.searchBox.setFocused(false);
                        return true;
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) {
            holdingIndex = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    // 1.20.1: mouseScrolled has 3 params (no scrollX)
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
            mouseY >= this.getY() + 25 && mouseY <= this.getY() + this.height) {
            if (scrollY > 0 && scrollOffset > 0) {
                scrollOffset--;
                return true;
            } else if (scrollY < 0 && scrollOffset + VISIBLE_ITEMS < blacklistedItems.size()) {
                scrollOffset++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                if (!autocompleteSuggestions.isEmpty()) {
                    selectedSuggestionIndex = (selectedSuggestionIndex - 1 + autocompleteSuggestions.size()) % autocompleteSuggestions.size();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                if (!autocompleteSuggestions.isEmpty()) {
                    selectedSuggestionIndex = (selectedSuggestionIndex + 1) % autocompleteSuggestions.size();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < autocompleteSuggestions.size()) {
                    addBlacklistedItem(autocompleteSuggestions.get(selectedSuggestionIndex));
                }
            } else {
                this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        
        // Handle DEL key
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (!selectedIndices.isEmpty()) {
                if (ClientConfig.shouldSkipBlacklistWarning()) {
                    List<String> toRemove = new ArrayList<>();
                    for (int idx : selectedIndices) {
                        if (idx >= 0 && idx < blacklistedItems.size()) {
                            toRemove.add(blacklistedItems.get(idx));
                        }
                    }
                    blacklistedItems.removeAll(toRemove);
                    selectedIndices.clear();
                } else {
                    Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(
                        Minecraft.getInstance().screen,
                        selectedIndices.size(),
                        () -> {
                            List<String> toRemove = new ArrayList<>();
                            for (int idx : selectedIndices) {
                                if (idx >= 0 && idx < blacklistedItems.size()) {
                                    toRemove.add(blacklistedItems.get(idx));
                                }
                            }
                            blacklistedItems.removeAll(toRemove);
                            selectedIndices.clear();
                        }
                    ));
                }
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            this.searchBox.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int listY = this.getY() + 25;
        
        // Render background for list
        guiGraphics.fill(this.getX(), listY, this.getX() + this.width, this.getY() + this.height, 0xAA000000);
        
        for (int i = 0; i < Math.min(VISIBLE_ITEMS, blacklistedItems.size() - scrollOffset); i++) {
            int idx = i + scrollOffset;
            int itemY = listY + i * ITEM_HEIGHT;
            String id = blacklistedItems.get(idx);
            
            boolean isSelected = selectedIndices.contains(idx);
            boolean isHovered = mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            
            // Background
            int bgColor = isSelected ? 0x882980B9 : (isHovered ? 0x8834495E : 0x44000000);
            guiGraphics.fill(this.getX() + 2, itemY + 2, this.getX() + this.width - 2, itemY + ITEM_HEIGHT - 2, bgColor);
            
            // Hold-to-delete animation fill (Red overlay)
            if (holdingIndex == idx || (isSelected && selectedIndices.contains(holdingIndex))) {
                if (holdTicks > 0) {
                    float progress = (float)holdTicks / MAX_HOLD_TICKS;
                    int fillWidth = (int)((this.width - 4) * progress);
                    guiGraphics.fill(this.getX() + 2, itemY + 2, this.getX() + 2 + fillWidth, itemY + ITEM_HEIGHT - 2, 0x88E74C3C);
                }
            }
            
            // Draw Item Icon if possible
            ResourceLocation res = ResourceLocation.tryParse(id);
            if (res != null) {
                Item item = BuiltInRegistries.ITEM.get(res);
                if (item != Items.AIR) {
                    guiGraphics.renderFakeItem(new ItemStack(item), this.getX() + 6, itemY + 4);
                }
            }
            
            // Draw ID Text
            guiGraphics.drawString(Minecraft.getInstance().font, id, this.getX() + 28, itemY + 8, 0xFFFFFF, true);
        }
        
        // Scrollbar indicator if needed
        if (blacklistedItems.size() > VISIBLE_ITEMS) {
            int scrollbarX = this.getX() + this.width - 4;
            int scrollbarHeight = (int)(((float)VISIBLE_ITEMS / blacklistedItems.size()) * (this.height - 25));
            int maxScrollOffset = blacklistedItems.size() - VISIBLE_ITEMS;
            int scrollbarY = listY + (int)(((float)scrollOffset / maxScrollOffset) * (this.height - 25 - scrollbarHeight));
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarHeight, 0xFF888888);
        }
        
        // Render autocomplete dropdown on top of everything
        if (this.searchBox.isFocused() && !autocompleteSuggestions.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 400); // Push forward in 3D space
            
            int suggestY = this.getY() + 22;
            guiGraphics.fill(this.getX(), suggestY, this.getX() + this.width, suggestY + (autocompleteSuggestions.size() * 16), 0xFF222222);
            guiGraphics.renderOutline(this.getX(), suggestY, this.width, autocompleteSuggestions.size() * 16, 0xFF555555);
            
            for (int i = 0; i < autocompleteSuggestions.size(); i++) {
                Item item = autocompleteSuggestions.get(i);
                int itemY = suggestY + (i * 16);
                
                if (i == selectedSuggestionIndex) {
                    guiGraphics.fill(this.getX() + 1, itemY, this.getX() + this.width - 1, itemY + 16, 0xFF444444);
                }
                
                guiGraphics.renderFakeItem(new ItemStack(item), this.getX() + 2, itemY);
                String name = Component.translatable(item.getDescriptionId()).getString();
                guiGraphics.drawString(Minecraft.getInstance().font, name, this.getX() + 22, itemY + 4, 0xFFFFFF, true);
            }
            
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // No-op
    }
}
