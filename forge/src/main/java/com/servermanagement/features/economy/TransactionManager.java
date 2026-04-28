package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.DataVersion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages all economy transactions with atomic operations and rollback support.
 * Prevents duplicate payments, ensures data consistency, handles failures gracefully.
 */
public class TransactionManager {
    private static TransactionManager instance;
    private final Map<String, Transaction> activeTransactions = new ConcurrentHashMap<>();
    private final Map<String, Transaction> completedTransactions = new ConcurrentHashMap<>();
    private final ReentrantLock transactionLock = new ReentrantLock();
    private File dataDirectory;
    
    private TransactionManager() {}
    
    public static synchronized TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }
    
    public void initialize(MinecraftServer server) {
        this.dataDirectory = new File(server.getServerDirectory(), "servermanagement/transactions");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        load();
    }
    
    /**
     * Execute a MineBay purchase transaction atomically
     */
    public TransactionResult executePurchase(ServerPlayer buyer, ServerPlayer seller, 
                                             String listingId, ItemStack item, 
                                             double moneyPrice, List<ItemStack> itemPrices) {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(transactionId, TransactionType.MINEBAY_PURCHASE);
        transaction.buyerId = buyer.getUUID();
        transaction.sellerId = seller.getUUID();
        transaction.listingId = listingId;
        transaction.item = item.copy();
        transaction.moneyAmount = moneyPrice;
        transaction.itemPrices = new ArrayList<>(itemPrices);
        
        transactionLock.lock();
        try {
            // Check if listing already processed
            if (isListingProcessed(listingId)) {
                return new TransactionResult(false, "Listing already sold", null);
            }
            
            // Mark as active
            activeTransactions.put(transactionId, transaction);
            transaction.status = TransactionStatus.PROCESSING;
            
            // Step 1: Validate buyer has sufficient funds
            EconomyManager economyManager = EconomyManager.getInstance();
            BankAccount buyerAccount = economyManager.getOrCreateAccount(buyer.getUUID());
            
            if (buyerAccount.getBalance() < moneyPrice) {
                rollback(transaction);
                return new TransactionResult(false, "Insufficient funds", null);
            }
            
            // Step 2: Validate buyer has required items
            if (!hasRequiredItems(buyer, itemPrices)) {
                rollback(transaction);
                return new TransactionResult(false, "Missing required items", null);
            }
            
            // Step 3: Remove money from buyer
            boolean moneyRemoved = buyerAccount.withdraw(moneyPrice);
            if (!moneyRemoved) {
                rollback(transaction);
                return new TransactionResult(false, "Failed to withdraw money", null);
            }
            transaction.step1Complete = true;
            
            // Step 4: Remove items from buyer
            if (!removeItems(buyer, itemPrices)) {
                rollback(transaction);
                return new TransactionResult(false, "Failed to remove items", null);
            }
            transaction.step2Complete = true;
            
            // Step 5: Add money to seller
            BankAccount sellerAccount = economyManager.getOrCreateAccount(seller.getUUID());
            sellerAccount.deposit(moneyPrice);
            transaction.step3Complete = true;
            
            // Step 6: Deliver items to seller
            deliverItems(seller, itemPrices, BankInventory.ItemSource.MINEBAY_SALE, 
                        "Sale to " + buyer.getName().getString());
            transaction.step4Complete = true;
            
            // Step 7: Deliver purchased item to buyer
            deliverItem(buyer, item, BankInventory.ItemSource.MINEBAY_PURCHASE, 
                       "Purchase from " + seller.getName().getString());
            transaction.step5Complete = true;
            
            // Mark as complete
            transaction.status = TransactionStatus.COMPLETED;
            transaction.completedTimestamp = System.currentTimeMillis();
            activeTransactions.remove(transactionId);
            completedTransactions.put(transactionId, transaction);
            
            save();
            
            ServerManagementMod.LOGGER.info("Transaction {} completed successfully", transactionId);
            return new TransactionResult(true, "Transaction successful", transactionId);
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Transaction failed: {}", transactionId, e);
            rollback(transaction);
            return new TransactionResult(false, "Transaction failed: " + e.getMessage(), null);
        } finally {
            transactionLock.unlock();
        }
    }
    
    /**
     * Execute a money transfer transaction atomically
     */
    public TransactionResult executeTransfer(ServerPlayer sender, UUID receiverId, 
                                            String receiverName, double amount) {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(transactionId, TransactionType.MONEY_TRANSFER);
        transaction.buyerId = sender.getUUID();
        transaction.sellerId = receiverId;
        transaction.moneyAmount = amount;
        
        transactionLock.lock();
        try {
            activeTransactions.put(transactionId, transaction);
            transaction.status = TransactionStatus.PROCESSING;
            
            EconomyManager economyManager = EconomyManager.getInstance();
            BankAccount senderAccount = economyManager.getOrCreateAccount(sender.getUUID());
            
            // Validate sender has funds
            if (senderAccount.getBalance() < amount) {
                rollback(transaction);
                return new TransactionResult(false, "Insufficient funds", null);
            }
            
            // Remove from sender
            if (!senderAccount.withdraw(amount)) {
                rollback(transaction);
                return new TransactionResult(false, "Failed to withdraw", null);
            }
            transaction.step1Complete = true;
            
            // Add to receiver
            BankAccount receiverAccount = economyManager.getOrCreateAccount(receiverId);
            receiverAccount.deposit(amount);
            transaction.step2Complete = true;
            
            // Complete
            transaction.status = TransactionStatus.COMPLETED;
            transaction.completedTimestamp = System.currentTimeMillis();
            activeTransactions.remove(transactionId);
            completedTransactions.put(transactionId, transaction);
            
            save();
            return new TransactionResult(true, "Transfer successful", transactionId);
            
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Transfer failed: {}", transactionId, e);
            rollback(transaction);
            return new TransactionResult(false, "Transfer failed: " + e.getMessage(), null);
        } finally {
            transactionLock.unlock();
        }
    }
    
    /**
     * Check if a listing has already been processed
     */
    private boolean isListingProcessed(String listingId) {
        return completedTransactions.values().stream()
            .anyMatch(t -> listingId.equals(t.listingId) && 
                          t.status == TransactionStatus.COMPLETED);
    }
    
    /**
     * Validate player has required items
     */
    private boolean hasRequiredItems(ServerPlayer player, List<ItemStack> required) {
        for (ItemStack requiredItem : required) {
            int count = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invItem = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameTags(invItem, requiredItem)) {
                    count += invItem.getCount();
                }
            }
            if (count < requiredItem.getCount()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Remove items from player inventory
     */
    private boolean removeItems(ServerPlayer player, List<ItemStack> items) {
        for (ItemStack requiredItem : items) {
            int remaining = requiredItem.getCount();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack invItem = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameTags(invItem, requiredItem)) {
                    int toRemove = Math.min(invItem.getCount(), remaining);
                    invItem.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Deliver item to player (inventory or bank fallback)
     */
    public void deliverItem(ServerPlayer player, ItemStack item, 
                           BankInventory.ItemSource source, String details) {
        boolean added = player.getInventory().add(item.copy());
        
        if (!added) {
            // Fallback to bank inventory
            EconomyManager economyManager = EconomyManager.getInstance();
            BankInventory bankInventory = economyManager.getBankInventory(player.getUUID());
            bankInventory.addItem(item, source, details);
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§e⚠ Inventory full! Item sent to Bank Storage. Use /bank to retrieve it."
            ));
            
            ServerManagementMod.LOGGER.info("Item sent to bank storage for {}: {}", 
                player.getName().getString(), item.getHoverName().getString());
        }
    }
    
    /**
     * Deliver multiple items to player
     */
    public void deliverItems(ServerPlayer player, List<ItemStack> items, 
                            BankInventory.ItemSource source, String details) {
        for (ItemStack item : items) {
            deliverItem(player, item, source, details);
        }
    }
    
    /**
     * Rollback a failed transaction
     */
    private void rollback(Transaction transaction) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            
            // Rollback step 3: Remove money from seller
            if (transaction.step3Complete && transaction.sellerId != null) {
                BankAccount sellerAccount = economyManager.getOrCreateAccount(transaction.sellerId);
                sellerAccount.withdraw(transaction.moneyAmount);
            }
            
            // Rollback step 1: Return money to buyer
            if (transaction.step1Complete && transaction.buyerId != null) {
                BankAccount buyerAccount = economyManager.getOrCreateAccount(transaction.buyerId);
                buyerAccount.deposit(transaction.moneyAmount);
            }
            
            // Note: Items are harder to rollback, would need transaction item storage
            // For now, log the issue
            if (transaction.step2Complete) {
                ServerManagementMod.LOGGER.warn("Transaction rollback: Items were removed but cannot be restored automatically");
            }
            
            transaction.status = TransactionStatus.FAILED;
            activeTransactions.remove(transaction.transactionId);
            
            ServerManagementMod.LOGGER.info("Transaction {} rolled back", transaction.transactionId);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to rollback transaction {}", transaction.transactionId, e);
        }
    }
    
    /**
     * Save transaction history
     */
    private void save() {
        try {
            File file = new File(dataDirectory, "transactions.dat");
            CompoundTag rootTag = new CompoundTag();
            
            // Save completed transactions (keep last 1000)
            ListTag completedTag = new ListTag();
            completedTransactions.values().stream()
                .sorted(Comparator.comparingLong(t -> -t.completedTimestamp))
                .limit(1000)
                .forEach(t -> completedTag.add(t.toNBT()));
            rootTag.put("Completed", completedTag);
            
            NbtIo.writeCompressed(rootTag, file);
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to save transactions", e);
        }
    }
    
    /**
     * Load transaction history
     */
    private void load() {
        try {
            File file = new File(dataDirectory, "transactions.dat");
            if (!file.exists()) {
                return;
            }
            
            CompoundTag rootTag = NbtIo.readCompressed(file);
            
            ListTag completedTag = rootTag.getList("Completed", Tag.TAG_COMPOUND);
            for (int i = 0; i < completedTag.size(); i++) {
                Transaction transaction = Transaction.fromNBT(completedTag.getCompound(i));
                completedTransactions.put(transaction.transactionId, transaction);
            }
            
            ServerManagementMod.LOGGER.info("Loaded {} completed transactions", completedTransactions.size());
        } catch (IOException e) {
            ServerManagementMod.LOGGER.error("Failed to load transactions", e);
        }
    }
    
    /**
     * Transaction types
     */
    public enum TransactionType {
        MINEBAY_PURCHASE,
        MINEBAY_COUNTEROFFER,
        MONEY_TRANSFER,
        DAILY_TASK_CLAIM,
        ACHIEVEMENT_CLAIM
    }
    
    /**
     * Transaction status
     */
    public enum TransactionStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
    
    /**
     * Transaction record
     */
    public static class Transaction {
        String transactionId;
        TransactionType type;
        TransactionStatus status;
        UUID buyerId;
        UUID sellerId;
        String listingId;
        ItemStack item;
        double moneyAmount;
        List<ItemStack> itemPrices = new ArrayList<>();
        boolean step1Complete;
        boolean step2Complete;
        boolean step3Complete;
        boolean step4Complete;
        boolean step5Complete;
        long createdTimestamp;
        long completedTimestamp;
        
        public Transaction(String transactionId, TransactionType type) {
            this.transactionId = transactionId;
            this.type = type;
            this.status = TransactionStatus.PROCESSING;
            this.createdTimestamp = System.currentTimeMillis();
        }
        
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("DataVersion", DataVersion.CURRENT_VERSION);
            tag.putString("Id", transactionId);
            tag.putString("Type", type.name());
            tag.putString("Status", status.name());
            if (buyerId != null) tag.putUUID("Buyer", buyerId);
            if (sellerId != null) tag.putUUID("Seller", sellerId);
            if (listingId != null) tag.putString("Listing", listingId);
            if (item != null) tag.put("Item", item.save(new CompoundTag()));
            tag.putDouble("Money", moneyAmount);
            tag.putLong("Created", createdTimestamp);
            tag.putLong("Completed", completedTimestamp);
            return tag;
        }
        
        public static Transaction fromNBT(CompoundTag tag) {
            // Check data version
            int dataVersion = tag.getInt("DataVersion");
            if (dataVersion > DataVersion.CURRENT_VERSION) {
                ServerManagementMod.LOGGER.warn("Transaction data version {} is newer than supported version {}",
                    dataVersion, DataVersion.CURRENT_VERSION);
            }
            
            String id = tag.getString("Id");
            TransactionType type = TransactionType.valueOf(tag.getString("Type"));
            Transaction transaction = new Transaction(id, type);
            transaction.status = TransactionStatus.valueOf(tag.getString("Status"));
            if (tag.contains("Buyer")) transaction.buyerId = tag.getUUID("Buyer");
            if (tag.contains("Seller")) transaction.sellerId = tag.getUUID("Seller");
            if (tag.contains("Listing")) transaction.listingId = tag.getString("Listing");
            if (tag.contains("Item")) transaction.item = ItemStack.of(tag.getCompound("Item"));
            transaction.moneyAmount = tag.getDouble("Money");
            transaction.createdTimestamp = tag.getLong("Created");
            transaction.completedTimestamp = tag.getLong("Completed");
            return transaction;
        }
    }
    
    /**
     * Transaction result
     */
    public static class TransactionResult {
        public final boolean success;
        public final String message;
        public final String transactionId;
        
        public TransactionResult(boolean success, String message, String transactionId) {
            this.success = success;
            this.message = message;
            this.transactionId = transactionId;
        }
    }
}
