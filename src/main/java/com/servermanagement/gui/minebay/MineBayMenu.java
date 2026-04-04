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
    
    public MineBayMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.MINEBAY_MENU.get(), containerId);
        
        // Initialize offering container (1 slot for item to sell)
        this.offeringContainer = new SimpleContainer(1);
        
        // Add offering slot (centered, visible only in CREATE_STEP2)
        int offeringX = 300; // Center of 600px width
        int offeringY = 85; // Below title
        offeringSlot = new ToggleableSlot(offeringContainer, 0, offeringX - 9, offeringY);
        offeringSlot.setVisible(false); // Hidden by default
        this.addSlot(offeringSlot);
        
        // Position inventory at bottom of 600x400 GUI
        int inventoryX = 221; // Centered
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
            if (!this.moveItemStackTo(slotStack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotStack, itemstack);
        }
        // If clicking inventory, try to move to offering slot
        else {
            if (offeringSlot != null && offeringContainer.isEmpty() && offeringSlot.isActive()) {
                ItemStack singleItem = slotStack.copy();
                singleItem.setCount(1);
                
                if (this.moveItemStackTo(singleItem, 0, 1, false)) {
                    slotStack.shrink(1);
                } else {
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
        
        // Return offering item to player if menu is closed AND item should be returned
        if (!player.level().isClientSide && shouldReturnItem) {
            ItemStack offeringItem = offeringContainer.getItem(0);
            if (!offeringItem.isEmpty()) {
                // Play item pickup sound
                player.level().playSound(null, player.blockPosition(), 
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 
                    net.minecraft.sounds.SoundSource.PLAYERS, 
                    0.2F, ((player.level().random.nextFloat() - player.level().random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                
                player.getInventory().placeItemBackInInventory(offeringItem);
                offeringContainer.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}
