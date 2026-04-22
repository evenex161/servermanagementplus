package com.servermanagement.gui.economy;

import com.servermanagement.features.economy.Transaction;
import com.servermanagement.gui.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for the Bank GUI - now includes transaction history
 */
public class BankMenu extends AbstractContainerMenu {
    private double balance;
    private List<Transaction> recentTransactions;

    public BankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public BankMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.BANK_MENU.get(), containerId);
        
        // Balance and transactions will be synced from server via SyncBankAccountPacket        reloadFromClientCache();
    }

    /** Re-pull balance + transaction history from the client cache. Called from
     *  the screen's init() so that a late SyncBankAccountPacket triggering
     *  refreshOpenScreen() picks up the freshly synced values instead of the
     *  snapshot latched in the constructor. */
    public void reloadFromClientCache() {        this.balance = com.servermanagement.client.ClientBankData.getBalance();
        this.recentTransactions = new ArrayList<>(com.servermanagement.client.ClientBankData.getTransactions());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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
}
