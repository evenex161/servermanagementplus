package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to purchase a MineBay listing
 */
public class PurchaseListingPacket implements IPacket {
    private final String listingId;
    
    public PurchaseListingPacket(String listingId) {
        this.listingId = listingId;
    }
    
    public PurchaseListingPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.listingId, 36);
    }
    
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer buyer = ctx.getSender();
            if (buyer == null) {
                return; // No buyer - reject packet
            }
            
            // Validate listing ID
            if (this.listingId == null || this.listingId.trim().isEmpty() || this.listingId.length() > 100) {
                buyer.sendSystemMessage(Component.literal("§cInvalid listing ID!"));
                return;
            }
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            if (listing == null) {
                buyer.sendSystemMessage(Component.literal("§cListing not found!"));
                return;
            }
            
            // Check if listing is fixed price
            if (listing.getOfferType() != MineBayListing.OfferType.FIXED) {
                buyer.sendSystemMessage(Component.literal("§cThis listing is negotiable only!"));
                return;
            }
            
            // Check if buyer is not the seller
            if (listing.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(Component.literal("§cYou cannot buy your own listing!"));
                return;
            }
            
            // Validate buyer has enough money
            EconomyManager economyManager = EconomyManager.getInstance();
            BankAccount buyerAccount = economyManager.getOrCreateAccount(buyer.getUUID());
            
            if (buyerAccount.getBalance() < listing.getMoneyPrice()) {
                buyer.sendSystemMessage(Component.literal("§cInsufficient funds! Need $" + 
                    String.format("%.2f", listing.getMoneyPrice())));
                return;
            }
            
            // Validate buyer has required price items
            List<PriceItemEntry> priceItems = listing.getPriceItems();
            Map<PriceItemEntry, Integer> requiredItems = new HashMap<>();
            
            for (PriceItemEntry priceItem : priceItems) {
                int required = priceItem.isUseStacks() ? 
                    priceItem.getAmount() * 64 : priceItem.getAmount();
                requiredItems.put(priceItem, required);
                
                // Count how many buyer has
                int count = 0;
                for (ItemStack stack : buyer.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(stack, priceItem.getItemStack())) {
                        count += stack.getCount();
                    }
                }
                
                if (count < required) {
                    buyer.sendSystemMessage(Component.literal("§cInsufficient items! Need " + 
                        required + "x " + priceItem.getItemStack().getHoverName().getString()));
                    return;
                }
            }
            
            // All checks passed - execute purchase
            
            // 1. Deduct money from buyer
            buyerAccount.withdraw(listing.getMoneyPrice());
            
            // 2. Remove required items from buyer
            for (Map.Entry<PriceItemEntry, Integer> entry : requiredItems.entrySet()) {
                PriceItemEntry priceItem = entry.getKey();
                int remaining = entry.getValue();
                
                for (int i = 0; i < buyer.getInventory().items.size() && remaining > 0; i++) {
                    ItemStack stack = buyer.getInventory().items.get(i);
                    if (ItemStack.isSameItemSameComponents(stack, priceItem.getItemStack())) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        remaining -= toRemove;
                    }
                }
            }
            
            // 3. Give purchased item to buyer
            ItemStack purchasedItem = listing.getItemForSale().copy();
            if (!buyer.getInventory().add(purchasedItem)) {
                // Inventory full, drop at player's feet
                buyer.drop(purchasedItem, false);
            }
            
            // 4. Give money to seller
            BankAccount sellerAccount = economyManager.getOrCreateAccount(listing.getSellerId());
            sellerAccount.deposit(listing.getMoneyPrice());
            
            // 5. Give price items to seller (if they're online, add to inventory, otherwise store)
            ServerPlayer seller = buyer.server.getPlayerList().getPlayer(listing.getSellerId());
            for (PriceItemEntry priceItem : priceItems) {
                ItemStack itemToGive = priceItem.getItemStack().copy();
                int amount = priceItem.isUseStacks() ? 
                    priceItem.getAmount() * 64 : priceItem.getAmount();
                
                while (amount > 0) {
                    int stackSize = Math.min(amount, itemToGive.getMaxStackSize());
                    ItemStack stack = itemToGive.copy();
                    stack.setCount(stackSize);
                    
                    if (seller != null && seller.isAlive()) {
                        if (!seller.getInventory().add(stack)) {
                            seller.drop(stack, false);
                        }
                    } else {
                        // Seller offline - add to their bank inventory
                        economyManager.getBankInventory(listing.getSellerId())
                            .addItem(stack, 
                                com.servermanagement.features.economy.BankInventory.ItemSource.MINEBAY_SALE, 
                                "Sold to " + buyer.getName().getString());
                    }
                    
                    amount -= stackSize;
                }
            }
            
            // 6. Remove listing
            mineBayManager.removeListing(listingId);
            
            // 7. Notify both parties
            buyer.sendSystemMessage(Component.literal("§aPurchase successful! You bought " + 
                purchasedItem.getHoverName().getString() + " for $" + 
                String.format("%.2f", listing.getMoneyPrice())));
            
            if (seller != null && seller.isAlive()) {
                seller.sendSystemMessage(Component.literal("§aYour listing was purchased by " + 
                    buyer.getName().getString() + "! Received $" + 
                    String.format("%.2f", listing.getMoneyPrice())));
            }
            
            // 8. Sync updated listings to all players
            mineBayManager.syncListingsToAllPlayers(buyer.server);
            
            // 9. Sync updated balances
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncBankAccountPacket(
                    buyerAccount.getBalance(), 
                    buyerAccount.getRecentTransactions(10)
                ),
                buyer
            );
            
            if (seller != null && seller.isAlive()) {
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new com.servermanagement.network.packet.SyncBankAccountPacket(
                        sellerAccount.getBalance(), 
                        sellerAccount.getRecentTransactions(10)
                    ),
                    seller
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
