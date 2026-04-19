package com.servermanagement.gui;

import com.servermanagement.features.economy.Transaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for the Bank GUI - handles balance and transaction data
 */
public class BankMenu extends AbstractContainerMenu {
    
    private double balance;
    private List<Transaction> recentTransactions;
    
    public BankMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.BANK_MENU, windowId);
        this.balance = 0.0;
        this.recentTransactions = new ArrayList<>();
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }
    
    public void setRecentTransactions(List<Transaction> transactions) {
        this.recentTransactions = new ArrayList<>(transactions);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
