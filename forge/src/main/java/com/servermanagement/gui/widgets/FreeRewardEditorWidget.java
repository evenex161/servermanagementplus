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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FreeRewardEditorWidget extends AbstractWidget {

    private final EditBox searchBox;
    private final EditBox quantityBox;
    private final ModernButton applyQuantityBtn;
    
    private final List<ItemStack> rewardItems = new ArrayList<>();
    private final Set<Integer> selectedIndices = new HashSet<>();
    
    private boolean isPendingSearchBoxFocus = false;
    private boolean isPendingQuantityBoxFocus = false;
    
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

    public FreeRewardEditorWidget(int x, int y, int width, int height, List<ItemStack> initialItems) {
        super(x, y, width, height, Component.empty());
        
        if (initialItems != null) {
            for (ItemStack item : initialItems) {
                if (item != null && !item.isEmpty()) {
                    this.rewardItems.add(item.copy());
                }
            }
        }
        
        this.searchBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("Search Item"));
        this.searchBox.setHint(Component.literal("Search item name or ID..."));
        this.searchBox.setMaxLength(32767);
        this.searchBox.setResponder(this::updateSuggestions);
        
        int bottomY = y + 25 + (VISIBLE_ITEMS * ITEM_HEIGHT) + 5;
        this.quantityBox = new EditBox(Minecraft.getInstance().font, x, bottomY, 50, 20, Component.literal("Quantity"));
        this.quantityBox.setHint(Component.literal("Qty"));
        this.quantityBox.setMaxLength(3);
        this.quantityBox.setFilter(s -> s.matches("\\d*")); // Numbers only
        
        this.applyQuantityBtn = new ModernButton(
            x + 55, bottomY, 80, 20,
            Component.literal("Apply Qty"),
            button -> applyQuantity(),
            ModernButton.ButtonStyle.PRIMARY
        );
    }
    
    public List<ItemStack> getRewardItems() {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack item : rewardItems) {
            copies.add(item.copy());
        }
        return copies;
    }
    
    private void applyQuantity() {
        if (quantityBox.getValue().isEmpty() || selectedIndices.isEmpty()) return;
        
        try {
            int qty = Integer.parseInt(quantityBox.getValue());
            if (qty > 0) {
                for (int idx : selectedIndices) {
                    if (idx >= 0 && idx < rewardItems.size()) {
                        rewardItems.get(idx).setCount(Math.min(qty, 99)); // NeoForge supports 99
                    }
                }
            }
        } catch (NumberFormatException ignored) {}
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
                if (autocompleteSuggestions.size() >= 5) break;
            }
        }
        
        if (!autocompleteSuggestions.isEmpty()) {
            selectedSuggestionIndex = 0;
        }
    }
    
    private void addRewardItem(Item item) {
        rewardItems.add(new ItemStack(item, 1));
        searchBox.setValue("");
        autocompleteSuggestions.clear();
        selectedSuggestionIndex = -1;
    }
    
    public void tick() {
        // Handle hold-to-delete logic
        if (holdingIndex >= 0 && holdingIndex < rewardItems.size()) {
            holdTicks++;
            if (holdTicks >= MAX_HOLD_TICKS) {
                if (selectedIndices.contains(holdingIndex)) {
                    List<ItemStack> toRemove = new ArrayList<>();
                    for (int idx : selectedIndices) {
                        if (idx >= 0 && idx < rewardItems.size()) {
                            toRemove.add(rewardItems.get(idx));
                        }
                    }
                    rewardItems.removeAll(toRemove);
                    selectedIndices.clear();
                } else {
                    rewardItems.remove(holdingIndex);
                }
                holdingIndex = -1;
                holdTicks = 0;
            }
        } else {
            holdTicks = Math.max(0, holdTicks - 4);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.searchBox.setFocused(false);
            this.quantityBox.setFocused(false);
        } else if (this.isPendingSearchBoxFocus) {
            this.searchBox.setFocused(true);
            this.quantityBox.setFocused(false);
            this.isPendingSearchBoxFocus = false;
        } else if (this.isPendingQuantityBoxFocus) {
            this.quantityBox.setFocused(true);
            this.searchBox.setFocused(false);
            this.isPendingQuantityBoxFocus = false;
        }
    }

    public boolean isSearchBoxFocused() {
        return (this.searchBox != null && this.searchBox.isFocused()) || (this.quantityBox != null && this.quantityBox.isFocused());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.searchBox.setFocused(true);
            this.quantityBox.setFocused(false);
            this.isPendingSearchBoxFocus = true;
            return true;
        }
        
        if (this.quantityBox.mouseClicked(mouseX, mouseY, button)) {
            this.quantityBox.setFocused(true);
            this.searchBox.setFocused(false);
            this.isPendingQuantityBoxFocus = true;
            return true;
        }
        
        if (this.applyQuantityBtn.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (!autocompleteSuggestions.isEmpty() && searchBox.isFocused()) {
            int suggestY = this.getY() + 22;
            for (int i = 0; i < autocompleteSuggestions.size(); i++) {
                if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
                    mouseY >= suggestY && mouseY < suggestY + 16) {
                    if (button == 0) {
                        addRewardItem(autocompleteSuggestions.get(i));
                        this.isPendingSearchBoxFocus = true;
                        return true;
                    }
                }
                suggestY += 16;
            }
        }
        
        int listY = this.getY() + 25;
        if (autocompleteSuggestions.isEmpty()) {
            for (int i = 0; i < Math.min(VISIBLE_ITEMS, rewardItems.size() - scrollOffset); i++) {
                int idx = i + scrollOffset;
                int itemY = listY + i * ITEM_HEIGHT;
                if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
                    mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    
                    if (button == 0) {
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
                        this.quantityBox.setFocused(false);
                        return true;
                    } else if (button == 1) {
                        holdingIndex = idx;
                        holdTicks = 0;
                        this.setFocused(true);
                        this.searchBox.setFocused(false);
                        this.quantityBox.setFocused(false);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) {
            holdingIndex = -1;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.setFocused(true);
        }
        if (this.quantityBox != null && this.quantityBox.isFocused()) {
            this.quantityBox.setFocused(true);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.searchBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (mouseX >= this.getX() && mouseX <= this.getX() + this.width &&
            mouseY >= this.getY() + 25 && mouseY <= this.getY() + 25 + (VISIBLE_ITEMS * ITEM_HEIGHT)) {
            if (scrollY > 0 && scrollOffset > 0) {
                scrollOffset--;
                return true;
            } else if (scrollY < 0 && scrollOffset + VISIBLE_ITEMS < rewardItems.size()) {
                scrollOffset++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
                    addRewardItem(autocompleteSuggestions.get(selectedSuggestionIndex));
                }
            } else {
                this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        
        if (this.quantityBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyQuantity();
                return true;
            }
            this.quantityBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (!selectedIndices.isEmpty()) {
                List<ItemStack> toRemove = new ArrayList<>();
                for (int idx : selectedIndices) {
                    if (idx >= 0 && idx < rewardItems.size()) {
                        toRemove.add(rewardItems.get(idx));
                    }
                }
                rewardItems.removeAll(toRemove);
                selectedIndices.clear();
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
        if (this.quantityBox.isFocused()) {
            this.quantityBox.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int listY = this.getY() + 25;
        
        guiGraphics.fill(this.getX(), listY, this.getX() + this.width, listY + (VISIBLE_ITEMS * ITEM_HEIGHT), 0xAA000000);
        
        for (int i = 0; i < Math.min(VISIBLE_ITEMS, rewardItems.size() - scrollOffset); i++) {
            int idx = i + scrollOffset;
            int itemY = listY + i * ITEM_HEIGHT;
            ItemStack stack = rewardItems.get(idx);
            
            boolean isSelected = selectedIndices.contains(idx);
            boolean isHovered = mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            
            int bgColor = isSelected ? 0x882980B9 : (isHovered ? 0x8834495E : 0x44000000);
            guiGraphics.fill(this.getX() + 2, itemY + 2, this.getX() + this.width - 2, itemY + ITEM_HEIGHT - 2, bgColor);
            
            if (holdingIndex == idx || (isSelected && selectedIndices.contains(holdingIndex))) {
                if (holdTicks > 0) {
                    float progress = (float)holdTicks / MAX_HOLD_TICKS;
                    int fillWidth = (int)((this.width - 4) * progress);
                    guiGraphics.fill(this.getX() + 2, itemY + 2, this.getX() + 2 + fillWidth, itemY + ITEM_HEIGHT - 2, 0x88E74C3C);
                }
            }
            
            guiGraphics.renderFakeItem(stack, this.getX() + 6, itemY + 4);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, this.getX() + 6, itemY + 4);
            
            String name = stack.getHoverName().getString();
            guiGraphics.drawString(Minecraft.getInstance().font, name, this.getX() + 28, itemY + 8, 0xFFFFFF, true);
            
            String qtyStr = "x" + stack.getCount();
            int qtyWidth = Minecraft.getInstance().font.width(qtyStr);
            guiGraphics.drawString(Minecraft.getInstance().font, qtyStr, this.getX() + this.width - 5 - qtyWidth, itemY + 8, 0xFFAA00, true);
        }
        
        if (rewardItems.size() > VISIBLE_ITEMS) {
            int scrollbarX = this.getX() + this.width - 4;
            int listHeight = VISIBLE_ITEMS * ITEM_HEIGHT;
            int scrollbarHeight = (int)(((float)VISIBLE_ITEMS / rewardItems.size()) * listHeight);
            int maxScrollOffset = rewardItems.size() - VISIBLE_ITEMS;
            int scrollbarY = listY + (int)(((float)scrollOffset / maxScrollOffset) * (listHeight - scrollbarHeight));
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarHeight, 0xFF888888);
        }
        
        this.quantityBox.render(guiGraphics, mouseX, mouseY, partialTick);
        this.applyQuantityBtn.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (!selectedIndices.isEmpty()) {
            guiGraphics.drawString(Minecraft.getInstance().font, selectedIndices.size() + " selected", this.getX() + 145, this.quantityBox.getY() + 6, 0x888888, true);
        }
        
        if (this.searchBox.isFocused() && !autocompleteSuggestions.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 400);
            
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
    public void setX(int x) {
        super.setX(x);
        if (this.searchBox != null) this.searchBox.setX(x);
        if (this.quantityBox != null) this.quantityBox.setX(x);
        if (this.applyQuantityBtn != null) this.applyQuantityBtn.setX(x + 55);
    }
    
    @Override
    public void setY(int y) {
        super.setY(y);
        if (this.searchBox != null) this.searchBox.setY(y);
        int bottomY = y + 25 + (VISIBLE_ITEMS * ITEM_HEIGHT) + 5;
        if (this.quantityBox != null) this.quantityBox.setY(bottomY);
        if (this.applyQuantityBtn != null) this.applyQuantityBtn.setY(bottomY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
