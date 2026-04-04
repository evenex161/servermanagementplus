package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
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

import java.util.function.Supplier;

/**
 * Packet sent from client to server when a seller accepts an offer
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
            
            ServerPlayer buyer = seller.server.getPlayerList().getPlayer(acceptedOffer.getBuyerId());
            
            // Validate buyer is online (for now, we'll require both parties online)
            if (buyer == null) {
                seller.sendSystemMessage(Component.literal("§cBuyer is not online! Transaction requires both parties online."));
                return;
            }
            
            // Validate buyer still has the money
            BankAccount buyerAccount = economyManager.getOrCreateAccount(acceptedOffer.getBuyerId());
            if (buyerAccount.getBalance() < acceptedOffer.getMoneyOffer()) {
                seller.sendSystemMessage(Component.literal("§cBuyer no longer has enough money!"));
                buyer.sendSystemMessage(Component.literal("§cYour offer was accepted but you don't have enough money!"));
                return;
            }
            
            // Validate buyer still has the items
            for (ItemStack offeredStack : acceptedOffer.getItemOffers()) {
                if (offeredStack.isEmpty()) continue;
                
                int found = 0;
                for (ItemStack invStack : buyer.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(invStack, offeredStack)) {
                        found += invStack.getCount();
                    }
                }
                
                if (found < offeredStack.getCount()) {
                    seller.sendSystemMessage(Component.literal("§cBuyer no longer has all the offered items!"));
                    buyer.sendSystemMessage(Component.literal("§cYour offer was accepted but you no longer have all the items!"));
                    return;
                }
            }
            
            // Execute the transaction
            BankAccount sellerAccount = economyManager.getOrCreateAccount(seller.getUUID());
            
            // 1. Deduct money from buyer
            if (acceptedOffer.getMoneyOffer() > 0) {
                buyerAccount.withdraw(acceptedOffer.getMoneyOffer());
            }
            
            // 2. Remove items from buyer's inventory
            for (ItemStack offeredStack : acceptedOffer.getItemOffers()) {
                if (offeredStack.isEmpty()) continue;
                
                int remaining = offeredStack.getCount();
                for (ItemStack invStack : buyer.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(invStack, offeredStack) && remaining > 0) {
                        int toRemove = Math.min(remaining, invStack.getCount());
                        invStack.shrink(toRemove);
                        remaining -= toRemove;
                    }
                }
            }
            
            // 3. Give purchased item to buyer
            ItemStack purchasedItem = listing.getItemForSale().copy();
            if (!buyer.getInventory().add(purchasedItem)) {
                buyer.drop(purchasedItem, false);
            }
            
            // 4. Pay seller
            if (acceptedOffer.getMoneyOffer() > 0) {
                sellerAccount.deposit(acceptedOffer.getMoneyOffer());
            }
            
            // 5. Give offered items to seller
            for (ItemStack offeredStack : acceptedOffer.getItemOffers()) {
                if (offeredStack.isEmpty()) continue;
                ItemStack stack = offeredStack.copy();
                if (!seller.getInventory().add(stack)) {
                    seller.drop(stack, false);
                }
            }
            
            // 6. Mark offer as accepted
            acceptedOffer.setStatus(MineBayOffer.OfferStatus.ACCEPTED);
            
            // 7. Complete the listing
            listing.setStatus(MineBayListing.ListingStatus.COMPLETED);
            mineBayManager.saveListing(listing);
            mineBayManager.removeListing(listingId);
            
            // 8. Sync listings to all players
            mineBayManager.syncListingsToAllPlayers(seller.server);
            
            // 9. Sync bank accounts
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new SyncBankAccountPacket(buyerAccount.getBalance(), buyerAccount.getRecentTransactions(10)),
                buyer
            );
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new SyncBankAccountPacket(sellerAccount.getBalance(), sellerAccount.getRecentTransactions(10)),
                seller
            );
            
            // Notify both parties
            seller.sendSystemMessage(Component.literal("§a✓ Offer accepted! Transaction complete."));
            seller.sendSystemMessage(Component.literal("§7Sold " + listing.getItemForSale().getDisplayName().getString() + 
                " to " + buyer.getName().getString()));
            
            buyer.sendSystemMessage(Component.literal("§a✓ Your offer was accepted!"));
            buyer.sendSystemMessage(Component.literal("§7Purchased " + listing.getItemForSale().getDisplayName().getString() + 
                " from " + seller.getName().getString()));
        });
        ctx.setPacketHandled(true);
    }
}
