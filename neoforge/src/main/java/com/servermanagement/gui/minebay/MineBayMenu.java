package com.servermanagement.gui.minebay;

import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for MineBay marketplace
 */
public class MineBayMenu extends AbstractContainerMenu {
    
    private boolean inventoryVisible = true;
    private java.util.List<ToggleableSlot> inventorySlots = new java.util.ArrayList<>();
    
    // Container for the item being sold
    private final SimpleContainer offeringContainer;
    private ToggleableSlot offeringSlot;
    private volatile boolean shouldReturnItem = true; // Track if item should be returned on close
    
    // Container for offer items (3 slots for items buyer offers to seller)
    private final SimpleContainer offerContainer;
    private final java.util.List<ToggleableSlot> offerSlots = new java.util.ArrayList<>();
    private volatile boolean shouldReturnOfferItems = true; // Track if offer items should be returned on close
    
    public MineBayMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.MINEBAY_MENU.get(), containerId);
        
        // All slot positions are in design space (panel width 600).
        // The matching ScalableContainerScreen renders everything through a uniform
        // pose-matrix scale, so slots are visually scaled with the panel even when
        // the workspace is smaller than the design size.
        int panelWidth = 600;
        
        // Initialize offering container (1 slot for item to sell)
        this.offeringContainer = new SimpleContainer(1);
        
        // Add offering slot (centered, visible only in CREATE_STEP1)
        int offeringX = panelWidth / 2 - 8;
        int offeringY = 85;
        offeringSlot = new ToggleableSlot(offeringContainer, 0, offeringX, offeringY);
        offeringSlot.setVisible(false); // Hidden by default
        this.addSlot(offeringSlot);
        
        // Initialize offer container (3 slots for items buyer offers)
        this.offerContainer = new SimpleContainer(3);
        
        // Add 3 offer slots (evenly spaced, visible only in MAKE_OFFER)
        int offerSlotSpacing = 50;
        int offerStartX = (panelWidth - (3 * 18 + 2 * (offerSlotSpacing - 18))) / 2;
        int offerSlotY = 148;
        for (int i = 0; i < 3; i++) {
            ToggleableSlot slot = new ToggleableSlot(offerContainer, i, 
                offerStartX + (i * offerSlotSpacing), offerSlotY);
            slot.setVisible(false); // Hidden by default
            this.addSlot(slot);
            offerSlots.add(slot);
        }
        
        // Position inventory centered at bottom of panel (design-space Y)
        int inventoryX = (panelWidth - 162) / 2; // Center 9-column inventory (9*18=162px)
        int inventoryY = 230;
        
        // Add player inventory slots (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ToggleableSlot slot = new ToggleableSlot(playerInventory, col + row * 9 + 9, inventoryX + col * 18, inventoryY + row * 18);
                this.addSlot(slot);
                inventorySlots.add(slot);
            }
        }
        
        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            ToggleableSlot slot = new ToggleableSlot(playerInventory, col, inventoryX + col * 18, inventoryY + 58);
            this.addSlot(slot);
            inventorySlots.add(slot);
        }
    }
    public int getOfferingSlotX() {
        return offeringSlot.x;
    }
    
    public int getOfferingSlotY() {
        return offeringSlot.y;
    }
    
    /**
     * Set whether inventory should be visible and interactive
     */
    public void setInventoryVisible(boolean visible) {
        this.inventoryVisible = visible;
        for (ToggleableSlot slot : inventorySlots) {
            slot.setVisible(visible);
        }
    }
    
    /**
     * Set whether offering slot should be visible
     */
    public void setOfferingSlotVisible(boolean visible) {
        if (offeringSlot != null) {
            offeringSlot.setVisible(visible);
        }
    }
    
    /**
     * Get the item in the offering slot
     */
    public ItemStack getOfferingItem() {
        return offeringContainer.getItem(0);
    }
    
    /**
     * Clear the offering slot (used when listing is created)
     */
    public void clearOfferingItem() {
        shouldReturnItem = false; // Don't return the item - it's been listed
        offeringContainer.setItem(0, ItemStack.EMPTY);
    }
    
    /**
     * Set the item in the offering slot
     */
    public void setOfferingItem(ItemStack stack) {
        offeringContainer.setItem(0, stack);
    }
    
    /**
     * Clear the offering slot
     */
    public void clearOfferingSlot() {
        offeringContainer.setItem(0, ItemStack.EMPTY);
    }
    
    /**
     * Set whether offer slots should be visible (MAKE_OFFER state)
     */
    public void setOfferSlotsVisible(boolean visible) {
        for (ToggleableSlot slot : offerSlots) {
            slot.setVisible(visible);
        }
    }
    
    /**
     * Get items from offer slots (non-empty only)
     */
    public java.util.List<ItemStack> getOfferItems() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < offerContainer.getContainerSize(); i++) {
            ItemStack stack = offerContainer.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        return items;
    }
    
    /**
     * Clear all offer slots without returning items (items sent to server)
     */
    public void clearOfferItems() {
        shouldReturnOfferItems = false;
        for (int i = 0; i < offerContainer.getContainerSize(); i++) {
            offerContainer.setItem(i, ItemStack.EMPTY);
        }
    }
    
    /**
     * Get the offer slot positions for rendering
     */
    public int getOfferSlotX(int index) {
        if (index >= 0 && index < offerSlots.size()) {
            return offerSlots.get(index).x;
        }
        return 0;
    }
    
    public int getOfferSlotY(int index) {
        if (index >= 0 && index < offerSlots.size()) {
            return offerSlots.get(index).y;
        }
        return 0;
    }
    
    public boolean isInventoryVisible() {
        return inventoryVisible;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        
        // Bounds check
        if (index < 0 || index >= this.slots.size()) {
            return itemstack;
        }
        
        Slot slot = this.slots.get(index);
        
        if (slot == null || !slot.hasItem()) {
            return itemstack;
        }
        
        // Don't allow shift-clicking hidden slots
        if (slot instanceof ToggleableSlot toggleable && !toggleable.isActive()) {
            return itemstack;
        }
        
        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            return itemstack;
        }
        
        itemstack = slotStack.copy();
        
        // If clicking offering slot (index 0), return item to inventory
        if (index == 0) {
            if (!this.moveItemStackTo(slotStack, 4, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotStack, itemstack);
        }
        // If clicking an offer slot (index 1-3), return item to inventory
        else if (index >= 1 && index <= 3) {
            if (!this.moveItemStackTo(slotStack, 4, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        // If clicking inventory, try to move to offering slot or offer slots
        else {
            // Try offering slot first if visible and empty
            if (offeringSlot != null && offeringContainer.isEmpty() && offeringSlot.isActive()) {
                ItemStack singleItem = slotStack.copy();
                singleItem.setCount(1);
                
                if (this.moveItemStackTo(singleItem, 0, 1, false)) {
                    slotStack.shrink(1);
                } else {
                    return ItemStack.EMPTY;
                }
            }
            // Try offer slots if visible
            else if (!offerSlots.isEmpty() && offerSlots.get(0).isActive()) {
                if (!this.moveItemStackTo(slotStack, 1, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }
        
        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        
        if (slotStack.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
        }
        
        slot.onTake(player, slotStack);
        
        return itemstack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        if (!player.level().isClientSide) {
            // Return offering item to player if menu is closed AND item should be returned
            if (shouldReturnItem) {
                ItemStack offeringItem = offeringContainer.getItem(0);
                if (!offeringItem.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(offeringItem);
                    offeringContainer.setItem(0, ItemStack.EMPTY);
                }
            }
            
            // Return offer items to player if menu is closed AND items should be returned
            if (shouldReturnOfferItems) {
                for (int i = 0; i < offerContainer.getContainerSize(); i++) {
                    ItemStack offerItem = offerContainer.getItem(i);
                    if (!offerItem.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(offerItem);
                        offerContainer.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }
}
