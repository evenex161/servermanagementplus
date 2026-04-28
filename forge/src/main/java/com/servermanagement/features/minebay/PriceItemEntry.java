package com.servermanagement.features.minebay;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.DataVersion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a single price item requirement with stack/count mode
 */
public class PriceItemEntry {
    private ItemStack itemStack;
    private boolean useStacks; // true = count in stacks, false = count individual items
    private int amount; // Number of stacks or items
    
    public PriceItemEntry(ItemStack itemStack, int amount, boolean useStacks) {
        this.itemStack = itemStack.copy();
        this.amount = amount;
        this.useStacks = useStacks;
    }
    
    public PriceItemEntry() {
        this.itemStack = ItemStack.EMPTY;
        this.amount = 1;
        this.useStacks = false;
    }
    
    public ItemStack getItemStack() {
        return itemStack.copy();
    }
    
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }
    
    public boolean isUseStacks() {
        return useStacks;
    }
    
    public void setUseStacks(boolean useStacks) {
        this.useStacks = useStacks;
    }
    
    public boolean isEmpty() {
        return itemStack.isEmpty();
    }
    
    /**
     * Get the total number of items required (accounting for stacks)
     */
    public int getTotalItemsRequired() {
        if (itemStack.isEmpty()) return 0;
        if (useStacks) {
            return amount * itemStack.getMaxStackSize();
        } else {
            return amount;
        }
    }
    
    /**
     * Get display string for this price item
     */
    public String getDisplayString() {
        if (itemStack.isEmpty()) return "";
        if (useStacks) {
            return amount + " stack" + (amount > 1 ? "s" : "") + " of " + itemStack.getHoverName().getString();
        } else {
            return amount + "x " + itemStack.getHoverName().getString();
        }
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DataVersion.CURRENT_VERSION);
        tag.put("Item", itemStack.save(new CompoundTag()));
        tag.putInt("Amount", amount);
        tag.putBoolean("UseStacks", useStacks);
        return tag;
    }
    
    public static PriceItemEntry fromNBT(CompoundTag tag) {
        // Check data version
        int dataVersion = tag.getInt("DataVersion");
        if (dataVersion > DataVersion.CURRENT_VERSION) {
            ServerManagementMod.LOGGER.warn("PriceItemEntry data version {} is newer than supported version {}",
                dataVersion, DataVersion.CURRENT_VERSION);
        }
        
        PriceItemEntry entry = new PriceItemEntry();
        entry.itemStack = ItemStack.of(tag.getCompound("Item"));
        entry.amount = tag.getInt("Amount");
        entry.useStacks = tag.getBoolean("UseStacks");
        return entry;
    }
}
