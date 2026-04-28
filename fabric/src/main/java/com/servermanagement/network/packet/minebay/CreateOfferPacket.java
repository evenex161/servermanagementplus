package com.servermanagement.network.packet.minebay;

import java.util.ArrayList;
import java.util.List;

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
 * Packet sent from client to server when a player makes an offer on a NEGOTIABLE listing
 */
public record CreateOfferPacket(String listingId, double moneyOffer, List<ItemStack> itemOffers) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "create_offer_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public CreateOfferPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), Math.max(0.0, buf.readDouble()), decodeItemOffers(buf));
    }

    private static List<ItemStack> decodeItemOffers(FriendlyByteBuf buf) {
        int itemCount = buf.readInt();
        if (itemCount < 0 || itemCount > 27) itemCount = 0;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(buf.readItem());
        }
        return items;
    }
    
        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeDouble(moneyOffer);
        buf.writeInt(itemOffers.size());
        for (ItemStack stack : itemOffers) {
            buf.writeItem(stack);
        }
    }
    
        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ServerPlayer buyer = player;
            if (buyer == null) return;
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            EconomyManager economyManager = EconomyManager.getInstance();
            
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            // Validation
            if (listing == null) {
                buyer.sendSystemMessage(Component.literal("┬ºcListing not found!"));
                return;
            }
            
            if (listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
                buyer.sendSystemMessage(Component.literal("┬ºcThis listing is no longer active!"));
                return;
            }
            
            if (listing.getOfferType() != MineBayListing.OfferType.NEGOTIABLE) {
                buyer.sendSystemMessage(Component.literal("┬ºcThis listing doesn't accept offers!"));
                return;
            }
            
            if (listing.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(Component.literal("┬ºcYou cannot make an offer on your own listing!"));
                return;
            }
            
            // Check if buyer has sufficient money
            if (moneyOffer > 0) {
                double buyerBalance = economyManager.getOrCreateAccount(buyer.getUUID()).getBalance();
                if (buyerBalance < moneyOffer) {
                    buyer.sendSystemMessage(Component.literal("┬ºcYou don't have enough money! Need $" + 
                        String.format("%.2f", moneyOffer) + " but only have $" + 
                        String.format("%.2f", buyerBalance)));
                    return;
                }
            }
            
            // Get offer items from the server-side menu container (NOT from the packet)
            // Items placed in the offer slots are already removed from player inventory by the menu system
            List<ItemStack> serverOfferItems = new ArrayList<>();
            if (buyer.containerMenu instanceof com.servermanagement.gui.minebay.MineBayMenu mineBayMenu) {
                serverOfferItems = mineBayMenu.getOfferItems();
            }
            
            // Validate that an offer was actually made
            if (moneyOffer <= 0 && serverOfferItems.isEmpty()) {
                buyer.sendSystemMessage(Component.literal("┬ºcYou must offer money or items!"));
                return;
            }
            
            // Escrow money from buyer's account
            boolean moneyEscrowed = false;
            if (moneyOffer > 0) {
                com.servermanagement.features.economy.BankAccount buyerAccount = 
                    economyManager.getOrCreateAccount(buyer.getUUID());
                if (!buyerAccount.tryWithdraw(moneyOffer)) {
                    buyer.sendSystemMessage(Component.literal("┬ºcFailed to escrow money!"));
                    return;
                }
                moneyEscrowed = true;
                buyerAccount.addTransaction(new Transaction(
                    TransactionType.MINEBAY_ESCROW, moneyOffer,
                    "Offer on " + listing.getItemForSale().getHoverName().getString(),
                    listing.getSellerId()));
            }
            
            // Clear offer items from the menu container (escrow them)
            // Items are already out of inventory ÔÇö just clear the container slots
            // Wrapped in try-catch to rollback money escrow on failure
            try {
                if (buyer.containerMenu instanceof com.servermanagement.gui.minebay.MineBayMenu mineBayMenu2) {
                    mineBayMenu2.clearOfferItems();
                }
                
                // Create the offer using server-validated items
                MineBayOffer offer = new MineBayOffer(
                    listingId,
                    buyer.getUUID(),
                    buyer.getName().getString(),
                    moneyOffer,
                    serverOfferItems
                );
                
                // Add offer to listing
                listing.addCounteroffer(offer);
                mineBayManager.saveListing(listing);
            } catch (Exception e) {
                // Rollback money escrow on failure
                if (moneyEscrowed) {
                    com.servermanagement.features.economy.BankAccount buyerAccount = 
                        economyManager.getOrCreateAccount(buyer.getUUID());
                    buyerAccount.deposit(moneyOffer);
                    buyerAccount.addTransaction(new Transaction(
                        TransactionType.MINEBAY_ESCROW_RETURN, moneyOffer,
                        "Offer failed ÔÇö money returned"));
                }
                buyer.sendSystemMessage(Component.literal("┬ºcFailed to create offer. Your money/items have been returned."));
                com.servermanagement.ServerManagementMod.LOGGER.error("Failed to create offer for listing {}", listingId, e);
                return;
            }
            
            // Notify buyer (action bar)
            String offerSummary = "";
            if (moneyOffer > 0) {
                offerSummary += "┬º6$" + String.format("%.2f", moneyOffer);
            }
            if (!serverOfferItems.isEmpty()) {
                offerSummary += (offerSummary.isEmpty() ? "" : " + ") + "┬ºf" + serverOfferItems.size() + " item(s)";
            }
            buyer.displayClientMessage(Component.literal(
                "┬ºa┬ºlÔ£ô ┬ºr┬ºaOffer submitted: " + offerSummary + " ┬ºa(escrowed)"), true);
            
            // Sync buyer's bank account after escrow
            com.servermanagement.features.economy.BankAccount buyerAccountSync = economyManager.getOrCreateAccount(buyer.getUUID());
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncBankAccountPacket(
                    buyerAccountSync.getBalance(), buyerAccountSync.getTransactions()),
                buyer
            );
            
            // Notify seller if online (action bar)
            ServerPlayer seller = buyer.server.getPlayerList().getPlayer(listing.getSellerId());
            if (seller != null) {
                seller.displayClientMessage(Component.literal(
                    "┬ºe[MineBay] ┬º6" + buyer.getName().getString() + " ┬ºemade an offer on your ┬ºf" + 
                    listing.getItemForSale().getHoverName().getString()), true);
            }

}
}
