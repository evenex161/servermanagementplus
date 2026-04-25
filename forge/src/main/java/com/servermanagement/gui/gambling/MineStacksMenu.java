package com.servermanagement.gui.gambling;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for MineStacks gambling interface
 */
public class MineStacksMenu extends AbstractContainerMenu {
    
    // Container for item betting
    private final SimpleContainer bettingContainer;
    private GamblingSlot bettingSlot;
    private final ContainerData data;
    private final java.util.List<GamblingSlot> inventorySlots = new java.util.ArrayList<>();
    
    // Server-side constructor
    public MineStacksMenu(int containerId, Inventory playerInventory, Player player) {
        this(containerId, playerInventory, new SimpleContainerData(2));
        
        // Sync player balance using two 16-bit ContainerData slots.
        // Vanilla ContainerData transmits each slot as a short (16-bit),
        // so a single slot can only hold values up to 32,767.
        // We split the 32-bit cent value across slot 0 (low) and slot 1 (high).
        if (player != null && !player.level().isClientSide) {
            double balance = EconomyManager.getInstance().getBalance(player.getUUID());
            setBalanceData(balance);
        }
    }
    
    // Client-side constructor
    public MineStacksMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(2));
    }
    
    private MineStacksMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.MINESTACKS_MENU.get(), containerId);
        
        this.data = data;
        this.addDataSlots(data);
        
        // Calculate scaled panel dimensions for slot positioning. Both axes use the
        // same uniform factor so slots stay inside the (uniformly scaled) panel rectangle.
        int panelWidth = getScaledPanelWidth(playerInventory);
        float vScale = getScaledVerticalFactor(playerInventory);
        
        // Initialize betting container (1 slot for item bets)
        this.bettingContainer = new SimpleContainer(1);
        
        // Add betting slot centered horizontally in panel
        int betSlotX = (panelWidth - 18) / 2;
        int betSlotY = Math.round(30 * vScale);
        bettingSlot = new GamblingSlot(bettingContainer, 0, betSlotX, betSlotY);
        // Start disabled - will be enabled when switching to item betting mode
        bettingSlot.setEnabled(false);
        this.addSlot(bettingSlot);
        
        // Add player inventory centered in panel (Y scaled to match panel height)
        int inventoryX = (panelWidth - 162) / 2; // Center 9-column inventory (9*18=162px)
        int inventoryY = Math.round(140 * vScale);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GamblingSlot slot = new GamblingSlot(playerInventory, col + row * 9 + 9, 
                    inventoryX + col * 18, inventoryY + row * 18);
                slot.setEnabled(false); // Start disabled
                this.addSlot(slot);
                inventorySlots.add(slot);
            }
        }
        
        for (int col = 0; col < 9; col++) {
            GamblingSlot slot = new GamblingSlot(playerInventory, col, 
                inventoryX + col * 18, inventoryY + 58);
            slot.setEnabled(false); // Start disabled
            this.addSlot(slot);
            inventorySlots.add(slot);
        }
    }
    
    /**
     * Get the scaled panel width, using ScreenScaler on client or defaulting to 400 on server
     */
    private static int getScaledPanelWidth(Inventory playerInventory) {
        if (playerInventory.player.level().isClientSide()) {
            return getClientPanelWidth();
        }
        return 400;
    }
    
    private static int getClientPanelWidth() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int guiW = mc.getWindow().getGuiScaledWidth();
            int guiH = mc.getWindow().getGuiScaledHeight();
            return com.servermanagement.gui.ScreenScaler.scale(400, 220, guiW, guiH)[0];
        } catch (Throwable t) {
            return 400;
        }
    }
    
    /**
     * Get the uniform scale factor used by the matching screen (400x220 design),
     * so menu slot Y positions shrink in lockstep with the panel rectangle.
     */
    private static float getScaledVerticalFactor(Inventory playerInventory) {
        if (!playerInventory.player.level().isClientSide()) {
            return 1.0f;
        }
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int guiW = mc.getWindow().getGuiScaledWidth();
            int guiH = mc.getWindow().getGuiScaledHeight();
            return com.servermanagement.gui.ScreenScaler.scaleFactor(400, 220, guiW, guiH);
        } catch (Throwable t) {
            return 1.0f;
        }
    }
    
    /**
     * Get the item in the betting slot
     */
    public ItemStack getBettingItem() {
        return bettingContainer.getItem(0);
    }
    
    /**
     * Clear the betting slot
     */
    public void clearBettingSlot() {
        bettingContainer.setItem(0, ItemStack.EMPTY);
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
        
        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            return itemstack;
        }
        
        itemstack = slotStack.copy();
        
        // If clicking betting slot (index 0), return to inventory
        if (index == 0) {
            if (!this.moveItemStackTo(slotStack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        // If clicking inventory, try to move to betting slot
        else {
            // Only move if betting slot is active, empty, and allows this item
            if (bettingSlot != null && bettingSlot.isActive() && 
                bettingContainer.isEmpty() && bettingSlot.mayPlace(slotStack)) {
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
        
        // Return betting item to player if menu is closed with an item in the slot
        if (!player.level().isClientSide) {
            ItemStack bettingItem = bettingContainer.getItem(0);
            if (!bettingItem.isEmpty()) {
                player.getInventory().placeItemBackInInventory(bettingItem);
                bettingContainer.setItem(0, ItemStack.EMPTY);
            }
        }
    }
    
    /**
     * Get the player's balance synced from server.
     * Reconstructs the full 32-bit cent value from two 16-bit ContainerData slots.
     */
    public double getPlayerBalance() {
        int low = this.data.get(0) & 0xFFFF;
        int high = this.data.get(1) & 0xFFFF;
        int balanceCents = (high << 16) | low;
        return balanceCents / 100.0;
    }
    
    /**
     * Updates the balance shown in the menu (called after gambling transactions).
     * Splits the cent value across two 16-bit ContainerData slots.
     */
    public void updateBalance(double newBalance) {
        setBalanceData(newBalance);
    }
    
    private void setBalanceData(double balance) {
        int cents = (int) Math.round(balance * 100);
        this.data.set(0, cents & 0xFFFF);        // low 16 bits
        this.data.set(1, (cents >> 16) & 0xFFFF); // high 16 bits
    }
    
    public Slot getBettingSlot() {
        return bettingSlot;
    }
    
    /**
     * Set whether the betting slot and inventory are active (visible and interactive)
     * This should be called on both client and server to keep them in sync
     */
    public void setBettingSlotActive(boolean active) {
        if (bettingSlot != null) {
            bettingSlot.setEnabled(active);
        }
        // Also update inventory slots
        for (GamblingSlot slot : inventorySlots) {
            slot.setEnabled(active);
        }
    }
}
