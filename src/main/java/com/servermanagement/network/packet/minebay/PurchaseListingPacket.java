package com.servermanagement.network.packet.minebay;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packet sent from client to server to purchase a MineBay listing
 */
public class PurchaseListingPacket implements IPacket {
    public static final CustomPacketPayload.Type<PurchaseListingPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "purchase_listing"));

    public static final StreamCodec<FriendlyByteBuf, PurchaseListingPacket> STREAM_CODEC =
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), PurchaseListingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer buyer = (ServerPlayer) ctx.player();
            if (buyer == null) {
                return; // No buyer - reject packet
            }
            
            // Validate listing ID
            if (this.listingId == null || this.listingId.trim().isEmpty() || this.listingId.length() > 100) {
                buyer.sendSystemMessage(Component.literal("┬ºcInvalid listing ID!"));
                return;
            }
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            if (listing == null) {
                buyer.sendSystemMessage(Component.literal("┬ºcListing not found!"));
                return;
            }
            
            // Check if listing is still active (prevents double-purchase)
            if (listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
                buyer.sendSystemMessage(Component.literal("§cThis listing is no longer available!"));
                return;
            }
            
            // Check if listing is fixed price
            if (listing.getOfferType() != MineBayListing.OfferType.FIXED) {
                buyer.sendSystemMessage(Component.literal("┬ºcThis listing is negotiable only!"));
                return;
            }
            
            // Check if buyer is not the seller
            if (listing.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(Component.literal("┬ºcYou cannot buy your own listing!"));
                return;
            }
            
            // === SMART PAYMENT SYSTEM ===
            // Calculate total money cost
            EconomyManager economyManager = EconomyManager.getInstance();
            BankAccount buyerAccount = economyManager.getOrCreateAccount(buyer.getUUID());
            double totalMoneyPrice = listing.getMoneyPrice();
            
            // Get the market pricing engine for item valuation
            com.servermanagement.features.economy.MarketPricingEngine pricingEngine = 
                com.servermanagement.features.economy.MarketPricingEngine.getInstance();
            pricingEngine.ensureFresh(buyer.server);
            
            // Build a list of inventory items with their market values (cheapest first)
            List<int[]> valuedSlots = new ArrayList<>(); // [slotIndex, unused]
            Map<Integer, Double> slotValues = new HashMap<>();
            
            for (int i = 0; i < buyer.getInventory().items.size(); i++) {
                ItemStack stack = buyer.getInventory().items.get(i);
                if (!stack.isEmpty()) {
                    double perItemValue = pricingEngine.getBasePrice(stack);
                    if (perItemValue > 0) {
                        slotValues.put(i, perItemValue);
                        valuedSlots.add(new int[]{i, 0});
                    }
                }
            }
            
            // Sort by per-item value ascending (cheapest first)
            valuedSlots.sort(Comparator.comparingDouble(a -> slotValues.getOrDefault(a[0], 0.0)));
            
            // Validate buyer has required price items (existing item-for-item requirements)
            List<PriceItemEntry> priceItems = listing.getPriceItems();
            Map<PriceItemEntry, Integer> requiredItems = new HashMap<>();
            
            for (PriceItemEntry priceItem : priceItems) {
                int required = priceItem.isUseStacks() ? 
                    priceItem.getAmount() * 64 : priceItem.getAmount();
                requiredItems.put(priceItem, required);
                
                int count = 0;
                for (ItemStack stack : buyer.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(stack, priceItem.getItemStack())) {
                        count += stack.getCount();
                    }
                }
                
                if (count < required) {
                    buyer.sendSystemMessage(Component.literal("┬ºcInsufficient items! Need " + 
                        required + "x " + priceItem.getItemStack().getHoverName().getString()));
                    return;
                }
            }
            
            // Smart payment: auto-select items from inventory (cheapest first) to cover the price
            double remainingCost = totalMoneyPrice;
            Map<Integer, Integer> itemsToConsume = new java.util.LinkedHashMap<>(); // slot -> count to remove
            double totalItemValue = 0.0;
            
            for (int[] slotInfo : valuedSlots) {
                if (remainingCost <= 0) break;
                
                int slot = slotInfo[0];
                ItemStack stack = buyer.getInventory().items.get(slot);
                if (stack.isEmpty()) continue;
                
                // Skip items that are needed for price item requirements
                boolean isRequiredItem = false;
                for (PriceItemEntry priceItem : priceItems) {
                    if (ItemStack.isSameItemSameComponents(stack, priceItem.getItemStack())) {
                        isRequiredItem = true;
                        break;
                    }
                }
                if (isRequiredItem) continue;
                
                double perItemValue = slotValues.getOrDefault(slot, 0.0);
                int maxNeeded = (int) Math.ceil(remainingCost / perItemValue);
                int toConsume = Math.min(maxNeeded, stack.getCount());
                double consumeValue = toConsume * perItemValue;
                
                itemsToConsume.put(slot, toConsume);
                totalItemValue += consumeValue;
                remainingCost -= consumeValue;
            }
            
            // After item selection, check if remaining cost can be covered by bank balance
            double bankPayment = Math.max(0, remainingCost);
            
            if (bankPayment > buyerAccount.getBalance()) {
                // Not enough combined resources
                double totalAvailable = totalItemValue + buyerAccount.getBalance();
                buyer.sendSystemMessage(Component.literal("┬ºcInsufficient funds! Need $" + 
                    String.format("%.2f", totalMoneyPrice) + " but you only have $" +
                    String.format("%.2f", totalAvailable) + " in items + bank balance."));
                return;
            }
            
            // === All checks passed - execute purchase ===
            
            // SECURITY: Mark listing as sold BEFORE processing payment
            // to prevent a second packet from passing the ACTIVE check above.
            listing.setStatus(MineBayListing.ListingStatus.COMPLETED);
            
            // 1. Remove auto-selected items from buyer inventory
            for (Map.Entry<Integer, Integer> entry : itemsToConsume.entrySet()) {
                ItemStack stack = buyer.getInventory().items.get(entry.getKey());
                stack.shrink(entry.getValue());
            }
            
            // 2. Deduct remaining money from buyer's bank
            if (bankPayment > 0) {
                buyerAccount.withdraw(bankPayment);
            }
            
            // 3. Remove required price items from buyer
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
            
            // 4. Give purchased item to buyer
            ItemStack purchasedItem = listing.getItemForSale().copy();
            if (!buyer.getInventory().add(purchasedItem)) {
                buyer.drop(purchasedItem, false);
            }
            
            // 5. Give money to seller (full listing price)
            BankAccount sellerAccount = economyManager.getOrCreateAccount(listing.getSellerId());
            sellerAccount.deposit(listing.getMoneyPrice());
            
            // 6. Give price items to seller
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
                        economyManager.getBankInventory(listing.getSellerId())
                            .addItem(stack, 
                                com.servermanagement.features.economy.BankInventory.ItemSource.MINEBAY_SALE, 
                                "Sold to " + buyer.getName().getString());
                    }
                    
                    amount -= stackSize;
                }
            }
            
            // 7. Remove listing
            mineBayManager.removeListing(listingId);
            
            // 8. Build payment breakdown message
            StringBuilder paymentMsg = new StringBuilder();
            paymentMsg.append("┬ºaPurchase successful! You bought ");
            paymentMsg.append(purchasedItem.getHoverName().getString());
            if (totalItemValue > 0 && bankPayment > 0) {
                paymentMsg.append(" ÔÇö Paid $").append(String.format("%.2f", totalItemValue));
                paymentMsg.append(" in items + $").append(String.format("%.2f", bankPayment));
                paymentMsg.append(" from bank");
            } else if (totalItemValue > 0) {
                paymentMsg.append(" ÔÇö Paid $").append(String.format("%.2f", totalItemValue));
                paymentMsg.append(" in items");
            } else {
                paymentMsg.append(" for $").append(String.format("%.2f", bankPayment));
            }
            buyer.sendSystemMessage(Component.literal(paymentMsg.toString()));
            
            if (seller != null && seller.isAlive()) {
                seller.sendSystemMessage(Component.literal("┬ºaYour listing was purchased by " + 
                    buyer.getName().getString() + "! Received $" + 
                    String.format("%.2f", listing.getMoneyPrice())));
            }
            
            // 9. Sync updated listings to all players
            mineBayManager.syncListingsToAllPlayers(buyer.server);
            
            // 10. Sync updated balances
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
    }
}
