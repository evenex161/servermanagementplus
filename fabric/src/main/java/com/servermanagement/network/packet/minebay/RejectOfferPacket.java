package com.servermanagement.network.packet.minebay;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/**
 * Packet sent from client to server when a seller rejects an offer.
 * Returns escrowed items and money to the buyer.
 */
public record RejectOfferPacket(String listingId, String offerId) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "reject_offer_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public RejectOfferPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), buf.readUtf(36));
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeUtf(offerId, 36);
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ServerPlayer seller = player;
            if (seller == null) return;
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            // Validation
            if (listing == null) {
                seller.sendSystemMessage(Component.literal("┬ºcListing not found!"));
                return;
            }
            
            if (!listing.getSellerId().equals(seller.getUUID())) {
                seller.sendSystemMessage(Component.literal("┬ºcYou can only reject offers on your own listings!"));
                return;
            }
            
            // Find the offer
            MineBayOffer rejectedOffer = null;
            for (MineBayOffer offer : listing.getCounteroffers()) {
                if (offer.getOfferId().equals(offerId) && offer.getStatus() == MineBayOffer.OfferStatus.PENDING) {
                    rejectedOffer = offer;
                    break;
                }
            }
            
            if (rejectedOffer == null) {
                seller.sendSystemMessage(Component.literal("┬ºcOffer not found or already processed!"));
                return;
            }
            
            // Return escrowed money to buyer
            if (rejectedOffer.getMoneyOffer() > 0) {
                BankAccount buyerAccount = EconomyManager.getInstance()
                    .getOrCreateAccount(rejectedOffer.getBuyerId());
                buyerAccount.deposit(rejectedOffer.getMoneyOffer());
                buyerAccount.addTransaction(new Transaction(
                    TransactionType.MINEBAY_ESCROW_RETURN, rejectedOffer.getMoneyOffer(),
                    "Offer rejected on " + listing.getItemForSale().getHoverName().getString(),
                    seller.getUUID()));
            }
            
            // Return escrowed items to buyer
            ServerPlayer buyer = seller.server.getPlayerList().getPlayer(rejectedOffer.getBuyerId());
            for (ItemStack offeredStack : rejectedOffer.getItemOffers()) {
                if (offeredStack.isEmpty()) continue;
                ItemStack stack = offeredStack.copy();
                if (buyer != null) {
                    if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(buyer, stack)) {
                        com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                            .addItem(buyer.getUUID(), stack);
                        buyer.sendSystemMessage(Component.literal("┬º6[MineBay] ┬ºeInventory full ÔÇö item stored in overflow. Use ┬ºf/overflow ┬ºeto claim."));
                    }
                } else {
                    // Buyer is offline ÔÇö store in overflow
                    com.servermanagement.features.economy.OverflowInventoryManager.getInstance()
                        .addItem(rejectedOffer.getBuyerId(), stack);
                }
            }
            
            // Mark offer as rejected
            rejectedOffer.setStatus(MineBayOffer.OfferStatus.REJECTED);
            mineBayManager.saveListing(listing);
            
            // Notify seller (action bar)
            seller.displayClientMessage(Component.literal(
                "┬ºe[MineBay] ┬ºcOffer rejected ┬º7ÔÇö escrowed items/money returned to buyer"), true);
            
            // Sync bank account if buyer is online
            if (buyer != null) {
                BankAccount buyerAccount = EconomyManager.getInstance()
                    .getOrCreateAccount(rejectedOffer.getBuyerId());
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new com.servermanagement.network.packet.SyncBankAccountPacket(
                        buyerAccount.getBalance(), buyerAccount.getTransactions()),
                    buyer
                );
                buyer.displayClientMessage(Component.literal(
                    "┬ºe[MineBay] ┬ºc" + seller.getName().getString() + " rejected your offer on ┬ºf" + 
                    listing.getItemForSale().getHoverName().getString() + " ┬º7ÔÇö escrowed items/money returned"), true);
            }

}
}
