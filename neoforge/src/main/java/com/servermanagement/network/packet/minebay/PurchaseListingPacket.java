package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packet sent from client to server to purchase a MineBay listing.
 * Supports two payment modes: BALANCE (0) deducts from bank,
 * ITEMS (1) removes selected inventory items as payment.
 */
public record PurchaseListingPacket(String listingId, int paymentMode, int[] selectedSlots) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PurchaseListingPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "purchase_listing"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PurchaseListingPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), PurchaseListingPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
// 0=BALANCE, 1=ITEMS// inventory slot indices for ITEMS mode

    
    public PurchaseListingPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), buf.readByte(), decodeSelectedSlots(buf));
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.listingId, 36);
        buf.writeByte(this.paymentMode);
        buf.writeVarInt(this.selectedSlots.length);
        for (int slot : this.selectedSlots) {
            buf.writeVarInt(slot);
        }
    }
    
        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer buyer = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
            
            // Check if listing is still active (prevents double-purchase)
            if (listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
                buyer.sendSystemMessage(Component.literal("§cThis listing is no longer available!"));
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
            
            // === PAYMENT VALIDATION ===
            EconomyManager economyManager = EconomyManager.getInstance();
            BankAccount buyerAccount = economyManager.getOrCreateAccount(buyer.getUUID());
            double totalMoneyPrice = listing.getMoneyPrice();
            
            // Validate payment mode
            if (this.paymentMode != 0 && this.paymentMode != 1) {
                buyer.sendSystemMessage(Component.literal("§cInvalid payment mode!"));
                return;
            }
            
            // Validate buyer has required price items (item-for-item requirements)
            List<PriceItemEntry> priceItems = listing.getPriceItems();
            Map<PriceItemEntry, Integer> requiredItems = new HashMap<>();
            
            for (PriceItemEntry priceItem : priceItems) {
                int required = priceItem.isUseStacks() ? 
                    priceItem.getAmount() * 64 : priceItem.getAmount();
                requiredItems.put(priceItem, required);
                
                int count = 0;
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = buyer.getInventory().getItem(i);
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
            
            // Payment mode specific validation
            com.servermanagement.features.economy.MarketPricingEngine pricingEngine = 
                com.servermanagement.features.economy.MarketPricingEngine.getInstance();
            double itemPaymentTotal = 0.0;
            
            if (this.paymentMode == 0) {
                // BALANCE mode - check bank balance
                if (totalMoneyPrice > 0 && buyerAccount.getBalance() < totalMoneyPrice) {
                    buyer.sendSystemMessage(Component.literal("§cInsufficient funds! Need $" + 
                        String.format("%.2f", totalMoneyPrice) + " but you only have $" +
                        String.format("%.2f", buyerAccount.getBalance()) + " in your bank."));
                    return;
                }
            } else {
                // ITEMS mode - validate selected slots and calculate total value
                if (totalMoneyPrice > 0) {
                    java.util.Set<Integer> validatedSlots = new java.util.HashSet<>();
                    for (int slot : this.selectedSlots) {
                        if (slot < 0 || slot >= 36) continue;
                        if (validatedSlots.contains(slot)) continue; // ignore duplicates
                        validatedSlots.add(slot);
                        
                        ItemStack stack = buyer.getInventory().getItem(slot);
                        if (!stack.isEmpty()) {
                            itemPaymentTotal += pricingEngine.getStackPrice(stack);
                        }
                    }
                    
                    if (itemPaymentTotal < totalMoneyPrice) {
                        buyer.sendSystemMessage(Component.literal("§cSelected items are worth $" + 
                            String.format("%.2f", itemPaymentTotal) + " but you need $" +
                            String.format("%.2f", totalMoneyPrice) + "!"));
                        return;
                    }
                }
            }
            
            // === All checks passed - execute purchase ===
            
            // SECURITY: Mark listing as sold BEFORE processing payment
            // to prevent a second packet from passing the ACTIVE check above.
            listing.setStatus(MineBayListing.ListingStatus.COMPLETED);
            
            // 1. Process money payment
            if (totalMoneyPrice > 0) {
                if (this.paymentMode == 0) {
                    // BALANCE mode: deduct from bank
                    buyerAccount.withdraw(totalMoneyPrice);
                    buyerAccount.addTransaction(new Transaction(
                        TransactionType.MINEBAY_PURCHASE, totalMoneyPrice,
                        "Bought " + listing.getItemForSale().getHoverName().getString(),
                        listing.getSellerId()));
                } else {
                    // ITEMS mode: remove selected items, refund excess to balance
                    java.util.Set<Integer> processedSlots = new java.util.HashSet<>();
                    for (int slot : this.selectedSlots) {
                        if (slot < 0 || slot >= 36) continue;
                        if (processedSlots.contains(slot)) continue;
                        processedSlots.add(slot);
                        buyer.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    
                    buyerAccount.addTransaction(new Transaction(
                        TransactionType.MINEBAY_PURCHASE, totalMoneyPrice,
                        "Bought " + listing.getItemForSale().getHoverName().getString() + " (items)",
                        listing.getSellerId()));
                    
                    // Deposit refund to buyer's balance
                    double refund = itemPaymentTotal - totalMoneyPrice;
                    if (refund > 0.01) {
                        buyerAccount.deposit(refund);
                        buyerAccount.addTransaction(new Transaction(
                            TransactionType.MINEBAY_REFUND, refund,
                            "Item payment refund"));
                    }
                }
            }
            
            // 2. Remove required price items from buyer
            for (Map.Entry<PriceItemEntry, Integer> entry : requiredItems.entrySet()) {
                PriceItemEntry priceItem = entry.getKey();
                int remaining = entry.getValue();
                
                for (int i = 0; i < 36 && remaining > 0; i++) {
                    ItemStack stack = buyer.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameComponents(stack, priceItem.getItemStack())) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        remaining -= toRemove;
                    }
                }
            }
            
            // 3. Give purchased item to buyer
            ItemStack purchasedItem = listing.getItemForSale().copy();
            String purchasedItemName = purchasedItem.getHoverName().getString();
            if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(buyer, purchasedItem)) {
                // Use overflow inventory instead of dropping on ground
                com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                    .addItem(buyer.getUUID(), purchasedItem);
                buyer.sendSystemMessage(Component.literal("§6[MineBay] §eInventory full — item stored in overflow. Use §f/overflow §eto claim."));
            }
            
            // 4. Give money to seller (full listing price)
            BankAccount sellerAccount = economyManager.getOrCreateAccount(listing.getSellerId());
            if (listing.getMoneyPrice() > 0) {
                sellerAccount.deposit(listing.getMoneyPrice());
                sellerAccount.addTransaction(new Transaction(
                    TransactionType.MINEBAY_SALE, listing.getMoneyPrice(),
                    "Sold " + purchasedItemName + " to " + buyer.getName().getString(),
                    buyer.getUUID()));
            }
            
            // 5. Give price items to seller
            ServerPlayer seller = buyer.level().getServer().getPlayerList().getPlayer(listing.getSellerId());
            for (PriceItemEntry priceItem : priceItems) {
                ItemStack itemToGive = priceItem.getItemStack().copy();
                int amount = priceItem.isUseStacks() ? 
                    priceItem.getAmount() * 64 : priceItem.getAmount();
                
                while (amount > 0) {
                    int stackSize = Math.min(amount, itemToGive.getMaxStackSize());
                    ItemStack stack = itemToGive.copy();
                    stack.setCount(stackSize);
                    
                    if (seller != null && seller.isAlive()) {
                        if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(seller, stack)) {
                            com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                                .addItem(listing.getSellerId(), stack);
                            seller.sendSystemMessage(Component.literal("§6[MineBay] §eInventory full — item stored in overflow. Use §f/overflow §eto claim."));
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
            
            // 6. Remove listing
            mineBayManager.removeListing(listingId);
            
            // 7. Action bar success message (buyer)
            if (totalMoneyPrice > 0) {
                if (this.paymentMode == 0) {
                    buyer.displayClientMessage(Component.literal(
                        "§a§l✓ §r§aPurchased §f" + purchasedItemName + " §afor §6$" + String.format("%.2f", totalMoneyPrice)), true);
                } else {
                    double refund = itemPaymentTotal - totalMoneyPrice;
                    String refundText = refund > 0.01 ? " §7(§a+$" + String.format("%.2f", refund) + " refund§7)" : "";
                    buyer.displayClientMessage(Component.literal(
                        "§a§l✓ §r§aPurchased §f" + purchasedItemName + " §awith items" + refundText), true);
                }
            } else {
                buyer.displayClientMessage(Component.literal(
                    "§a§l✓ §r§aPurchased §f" + purchasedItemName), true);
            }
            
            // 8. Notification to seller (action bar if online)
            if (seller != null && seller.isAlive()) {
                seller.displayClientMessage(Component.literal(
                    "§6§l$ §r§6" + buyer.getName().getString() + " §abought your §f" + purchasedItemName +
                    (listing.getMoneyPrice() > 0 ? " §afor §6$" + String.format("%.2f", listing.getMoneyPrice()) : "")), true);
            }
            
            // 9. Sync updated listings to all players
            mineBayManager.syncListingsToAllPlayers(buyer.level().getServer());
            
            // 10. Sync updated balances
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncBankAccountPacket(
                    buyerAccount.getBalance(), 
                    buyerAccount.getTransactions()
                ),
                buyer
            );
            
            if (seller != null && seller.isAlive()) {
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new com.servermanagement.network.packet.SyncBankAccountPacket(
                        sellerAccount.getBalance(), 
                        sellerAccount.getTransactions()
                    ),
                    seller
                );
            }
        });
        // packet handled
    }

    private static int[] decodeSelectedSlots(FriendlyByteBuf buf) {
        int slotCount = buf.readVarInt();
        if (slotCount < 0 || slotCount > 36) {
            return new int[0];
        }
        int[] slots = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = buf.readVarInt();
        }
        return slots;
    }
}
