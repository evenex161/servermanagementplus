package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import com.servermanagement.network.packet.IPacket;
import com.servermanagement.network.packet.SyncBankAccountPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from client to server when a seller accepts an offer.
 * Items and money are already escrowed at offer creation time via CreateOfferPacket.
 */
public class AcceptOfferPacket implements IPacket {
    private final String listingId;
    private final String offerId;
    
    public AcceptOfferPacket(String listingId, String offerId) {
        this.listingId = listingId;
        this.offerId = offerId;
    }
    
    public AcceptOfferPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
        this.offerId = buf.readUtf(36);
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeUtf(offerId, 36);
    }
    
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer seller = ctx.getSender();
            if (seller == null) return;
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            EconomyManager economyManager = EconomyManager.getInstance();
            
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            // Validation
            if (listing == null) {
                seller.sendSystemMessage(Component.literal("§cListing not found!"));
                return;
            }
            
            if (!listing.getSellerId().equals(seller.getUUID())) {
                seller.sendSystemMessage(Component.literal("§cYou can only accept offers on your own listings!"));
                return;
            }
            
            if (listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
                seller.sendSystemMessage(Component.literal("§cThis listing is no longer active!"));
                return;
            }
            
            // Find the offer
            MineBayOffer acceptedOffer = null;
            for (MineBayOffer offer : listing.getCounteroffers()) {
                if (offer.getOfferId().equals(offerId) && offer.getStatus() == MineBayOffer.OfferStatus.PENDING) {
                    acceptedOffer = offer;
                    break;
                }
            }
            
            if (acceptedOffer == null) {
                seller.sendSystemMessage(Component.literal("§cOffer not found or already processed!"));
                return;
            }
            
            // Execute the transaction — items and money are already escrowed
            
            // SECURITY: Mark listing as COMPLETED immediately to prevent concurrent accept operations
            listing.setStatus(MineBayListing.ListingStatus.COMPLETED);
            
            BankAccount sellerAccount = economyManager.getOrCreateAccount(seller.getUUID());
            
            // 1. Pay seller the escrowed money
            if (acceptedOffer.getMoneyOffer() > 0) {
                sellerAccount.deposit(acceptedOffer.getMoneyOffer());
                sellerAccount.addTransaction(new Transaction(
                    TransactionType.MINEBAY_SALE, acceptedOffer.getMoneyOffer(),
                    "Sold " + listing.getItemForSale().getHoverName().getString(),
                    acceptedOffer.getBuyerId()));
            }
            
            // 2. Give escrowed items to seller
            for (ItemStack offeredStack : acceptedOffer.getItemOffers()) {
                if (offeredStack.isEmpty()) continue;
                ItemStack stack = offeredStack.copy();
                if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(seller, stack)) {
                    // Overflow: add to seller's overflow inventory
                    com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                        .addItem(seller.getUUID(), stack);
                    seller.sendSystemMessage(Component.literal("§6[MineBay] §eInventory full — item stored in overflow. Use §f/overflow §eto claim."));
                }
            }
            
            // 3. Give purchased item to buyer (may be offline)
            ItemStack purchasedItem = listing.getItemForSale().copy();
            String purchasedItemName = purchasedItem.getHoverName().getString();
            ServerPlayer buyer = seller.server.getPlayerList().getPlayer(acceptedOffer.getBuyerId());
            
            // Record buyer transaction
            BankAccount buyerAccount = economyManager.getOrCreateAccount(acceptedOffer.getBuyerId());
            if (acceptedOffer.getMoneyOffer() > 0) {
                buyerAccount.addTransaction(new Transaction(
                    TransactionType.MINEBAY_PURCHASE, acceptedOffer.getMoneyOffer(),
                    "Bought " + purchasedItemName + " from " + seller.getName().getString(),
                    seller.getUUID()));
            }
            
            if (buyer != null) {
                if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(buyer, purchasedItem)) {
                    com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                        .addItem(buyer.getUUID(), purchasedItem);
                    buyer.sendSystemMessage(Component.literal("§6[MineBay] §eInventory full — item stored in overflow. Use §f/overflow §eto claim."));
                }
            } else {
                // Buyer is offline — store in overflow
                com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                    .addItem(acceptedOffer.getBuyerId(), purchasedItem);
            }
            
            // 4. Reject all other pending offers on this listing (return their escrowed items/money)
            for (MineBayOffer other : listing.getCounteroffers()) {
                if (other != acceptedOffer && other.getStatus() == MineBayOffer.OfferStatus.PENDING) {
                    returnEscrowedOffer(other, seller.server);
                    other.setStatus(MineBayOffer.OfferStatus.REJECTED);
                }
            }
            
            // 5. Mark offer as accepted
            acceptedOffer.setStatus(MineBayOffer.OfferStatus.ACCEPTED);
            
            // 6. Complete the listing (status already set to COMPLETED above)
            mineBayManager.saveListing(listing);
            mineBayManager.removeListing(listingId);
            
            // 7. Sync listings to all players
            mineBayManager.syncListingsToAllPlayers(seller.server);
            
            // 8. Sync bank accounts
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new SyncBankAccountPacket(sellerAccount.getBalance(), sellerAccount.getRecentTransactions(10)),
                seller
            );
            if (buyer != null) {
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncBankAccountPacket(buyerAccount.getBalance(), buyerAccount.getRecentTransactions(10)),
                    buyer
                );
            }
            
            // Notify both parties (action bar)
            String moneyStr = acceptedOffer.getMoneyOffer() > 0 ? 
                " for §6$" + String.format("%.2f", acceptedOffer.getMoneyOffer()) : "";
            seller.displayClientMessage(Component.literal(
                "§a§l✓ §r§aOffer accepted! Sold §f" + purchasedItemName + moneyStr), true);
            
            if (buyer != null) {
                buyer.displayClientMessage(Component.literal(
                    "§a§l✓ §r§aYour offer was accepted! Purchased §f" + purchasedItemName + 
                    " §afrom §6" + seller.getName().getString()), true);
            }
        });
        ctx.setPacketHandled(true);
    }
    
    /**
     * Return escrowed items and money from a rejected/displaced offer back to the buyer
     */
    private void returnEscrowedOffer(MineBayOffer offer, net.minecraft.server.MinecraftServer server) {
        EconomyManager economyManager = EconomyManager.getInstance();
        
        // Return money
        if (offer.getMoneyOffer() > 0) {
            BankAccount buyerAccount = economyManager.getOrCreateAccount(offer.getBuyerId());
            buyerAccount.deposit(offer.getMoneyOffer());
            buyerAccount.addTransaction(new Transaction(
                TransactionType.MINEBAY_ESCROW_RETURN, offer.getMoneyOffer(),
                "Offer auto-rejected (listing sold)"));
        }
        
        // Return items
        ServerPlayer buyer = server.getPlayerList().getPlayer(offer.getBuyerId());
        for (ItemStack offeredStack : offer.getItemOffers()) {
            if (offeredStack.isEmpty()) continue;
            ItemStack stack = offeredStack.copy();
            if (buyer != null) {
                if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(buyer, stack)) {
                    com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                        .addItem(buyer.getUUID(), stack);
                    buyer.sendSystemMessage(Component.literal("§6[MineBay] §eInventory full — item stored in overflow. Use §f/overflow §eto claim."));
                }
            } else {
                com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                    .addItem(offer.getBuyerId(), stack);
            }
        }
        
        // Notify buyer if online (action bar)
        if (buyer != null) {
            buyer.displayClientMessage(Component.literal("§e[MineBay] §cYour offer was auto-rejected §7(listing sold) — escrowed items/money returned"), true);
        }
    }
}
