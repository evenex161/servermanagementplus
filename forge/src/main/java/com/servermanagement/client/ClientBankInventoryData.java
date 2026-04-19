package com.servermanagement.client;

import com.servermanagement.features.economy.BankInventory;

/**
 * Client-side holder for bank inventory data
 */
public class ClientBankInventoryData {
    private static BankInventory bankInventory = null;
    
    public static void setBankInventory(BankInventory inventory) {
        bankInventory = inventory;
    }
    
    public static BankInventory getBankInventory() {
        return bankInventory;
    }
    
    public static boolean hasBankInventory() {
        return bankInventory != null;
    }
    
    public static void clear() {
        bankInventory = null;
    }
}
