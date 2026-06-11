package com.servermanagement.features.playermanager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

public class PlayerInventoryMenuProvider implements MenuProvider {
    
    private final ServerPlayer target;
    
    public PlayerInventoryMenuProvider(ServerPlayer target) {
        this.target = target;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal(target.getName().getString() + "'s Inventory");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Create a live-updating container that wraps the target player's inventory
        LiveInventoryContainer container = new LiveInventoryContainer(target);
        
        // Create a 4-row chest menu (36 slots for hotbar + 3x9 inventory)
        // Use the constructor directly since there's no fourRows(int, Inventory, Container) static method
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x4, containerId, playerInventory, container, 4);
    }
    
    /**
     * A container that directly references the target player's main inventory (36 slots).
     * This provides real-time updates when items are added/removed from the target's inventory.
     */
    private static class LiveInventoryContainer implements Container {
        private final ServerPlayer target;
        
        public LiveInventoryContainer(ServerPlayer target) {
            this.target = target;
        }
        
        @Override
        public int getContainerSize() {
            return 36; // Hotbar (9) + Main inventory (27)
        }
        
        @Override
        public boolean isEmpty() {
            return target.getInventory().isEmpty();
        }
        
        @Override
        public ItemStack getItem(int slot) {
            if (slot < 0 || slot >= 36) {
                return ItemStack.EMPTY;
            }
            return target.getInventory().getItem(slot);
        }
        
        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= 36) {
                return ItemStack.EMPTY;
            }
            return target.getInventory().removeItem(slot, amount);
        }
        
        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= 36) {
                return ItemStack.EMPTY;
            }
            return target.getInventory().removeItemNoUpdate(slot);
        }
        
        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < 36) {
                target.getInventory().setItem(slot, stack);
            }
        }
        
        @Override
        public void setChanged() {
            target.getInventory().setChanged();
        }
        
        @Override
        public boolean stillValid(Player player) {
            return target.isAlive() && !target.hasDisconnected();
        }
        
        @Override
        public void clearContent() {
            for (int i = 0; i < 36; i++) {
                target.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        
    }
}

