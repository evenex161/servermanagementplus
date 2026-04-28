package com.servermanagement.gui.minebay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.servermanagement.gui.ScreenScaler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A searchable item picker GUI that shows all obtainable items
 * Similar to creative mode search, but read-only selection
 */
public class ItemPickerScreen extends Screen {
    
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 11;
    private static final int SLOT_SIZE = 18;
    private static final int GUI_WIDTH = 195;
    private static final int GUI_HEIGHT = 280;
    
    private int guiW = GUI_WIDTH;
    private int guiH = GUI_HEIGHT;
    
    private final Screen parent;
    private final Consumer<ItemStack> onItemSelected;
    private EditBox searchBox;
    private List<ItemStack> allItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private ItemStack highlightItem = null; // Item to scroll to and highlight
    
    public ItemPickerScreen(Screen parent, Consumer<ItemStack> onItemSelected) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.onItemSelected = onItemSelected;
        buildItemList();
    }
    
    /**
     * Set an item to highlight and scroll to when opened
     */
    public void setHighlightItem(ItemStack item) {
        this.highlightItem = item.isEmpty() ? null : item;
    }
    
    private void buildItemList() {
        allItems.clear();
        
        // Add all registered items
        for (Item item : BuiltInRegistries.ITEM) {
            // Skip air and other non-obtainable items
            if (item == Items.AIR) continue;
            
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                allItems.add(stack);
            }
        }
        
        // Sort alphabetically by item name
        allItems.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
        
        filteredItems = new ArrayList<>(allItems);
        updateMaxScroll();
    }
    
    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        this.guiW = Math.min(GUI_WIDTH, this.width - 20);
        this.guiH = Math.min(GUI_HEIGHT, this.height - 20);
        
        int centerX = (this.width - guiW) / 2;
        int centerY = (this.height - guiH) / 2;
        
        // Search box at top
        this.searchBox = new EditBox(this.font, centerX + 5, centerY + 10, guiW - 10, 20, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search items..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
        
        // Scroll to highlighted item if set
        if (highlightItem != null) {
            scrollToItem(highlightItem);
        }
    }
    
    private void onSearchChanged(String search) {
        filteredItems.clear();
        
        if (search.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String lowerSearch = search.toLowerCase();
            for (ItemStack stack : allItems) {
                if (stack.getHoverName().getString().toLowerCase().contains(lowerSearch)) {
                    filteredItems.add(stack);
                }
            }
        }
        
        scrollOffset = 0;
        updateMaxScroll();
    }
    
    private void updateMaxScroll() {
        int totalRows = (int) Math.ceil(filteredItems.size() / (double) GRID_COLS);
        maxScroll = Math.max(0, totalRows - GRID_ROWS);
    }
    
    private void scrollToItem(ItemStack item) {
        if (item == null || item.isEmpty()) return;
        
        for (int i = 0; i < filteredItems.size(); i++) {
            ItemStack stack = filteredItems.get(i);
            if (ItemStack.isSameItemSameComponents(stack, item)) {
                int row = i / GRID_COLS;
                // Center the row in view
                scrollOffset = Math.max(0, Math.min(maxScroll, row - GRID_ROWS / 2));
                return;
            }
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
        
        int centerX = (this.width - guiW) / 2;
        int centerY = (this.height - guiH) / 2;
        
        // Main GUI background
        guiGraphics.fill(centerX, centerY, centerX + guiW, centerY + guiH, 0xE0202020);
        
        // Title
        Component title = Component.literal("Select Price Item");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, centerX + (guiW - titleWidth) / 2, centerY - 15, 0xFFD700, true);
        
        // Search box
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Item grid
        int gridStartX = centerX + 5;
        int gridStartY = centerY + 35;
        
        // Render item slots
        int startIndex = scrollOffset * GRID_COLS;
        int endIndex = Math.min(filteredItems.size(), startIndex + (GRID_COLS * GRID_ROWS));
        
        ItemStack hoveredItem = null;
        boolean isHighlighted = false;
        
        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / GRID_COLS;
            int col = relativeIndex % GRID_COLS;
            
            int slotX = gridStartX + col * SLOT_SIZE;
            int slotY = gridStartY + row * SLOT_SIZE;
            
            ItemStack stack = filteredItems.get(i);
            
            // Check if this is the highlighted item
            isHighlighted = highlightItem != null && ItemStack.isSameItemSameComponents(stack, highlightItem);
            
            // Slot background
            int slotColor = 0xFF8B8B8B;
            if (isHighlighted) {
                slotColor = 0xFFFFD700; // Gold highlight
            } else if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && 
                       mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                slotColor = 0xFFFFFFFF; // White on hover
                hoveredItem = stack;
            }
            
            // Draw slot border
            guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, slotColor); // Top
            guiGraphics.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, slotColor); // Bottom
            guiGraphics.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, slotColor); // Left
            guiGraphics.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, slotColor); // Right
            
            // Render item
            guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
        }
        
        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = centerX + guiW - 10;
            int scrollbarY = gridStartY;
            int scrollbarHeight = GRID_ROWS * SLOT_SIZE;
            
            // Scrollbar background
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF3A3A3A);
            
            // Scrollbar thumb
            int thumbHeight = Math.max(10, scrollbarHeight / (maxScroll + GRID_ROWS));
            int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * (scrollOffset / (float) maxScroll));
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF8B8B8B);
        }
        
        // Item tooltip
        if (hoveredItem != null) {
            guiGraphics.renderTooltip(this.font, hoveredItem.getHoverName(), mouseX, mouseY);
        }
        
        // Instructions at bottom
        int instructY = centerY + guiH - 12;
        guiGraphics.drawString(this.font, 
            Component.literal("Click an item to select ÔÇó ESC to cancel"),
            centerX + 5, instructY, 0xAAAAAA, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check search box first
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        int centerX = (this.width - guiW) / 2;
        int centerY = (this.height - guiH) / 2;
        
        int gridStartX = centerX + 5;
        int gridStartY = centerY + 35;
        
        // Check item grid clicks
        int startIndex = scrollOffset * GRID_COLS;
        int endIndex = Math.min(filteredItems.size(), startIndex + (GRID_COLS * GRID_ROWS));
        
        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / GRID_COLS;
            int col = relativeIndex % GRID_COLS;
            
            int slotX = gridStartX + col * SLOT_SIZE;
            int slotY = gridStartY + row * SLOT_SIZE;
            
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && 
                mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                // Item clicked - select it
                ItemStack selected = filteredItems.get(i).copy();
                onItemSelected.accept(selected);
                this.minecraft.setScreen(parent);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (scrollY < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // ESC to close
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            this.minecraft.setScreen(parent);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
