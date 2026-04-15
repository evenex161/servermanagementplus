package com.servermanagement.network.packet.minebay;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.MineBayOffer;
import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when a player makes an offer on a NEGOTIABLE listing
 */
public class CreateOfferPacket implements IPacket {
    public static final CustomPacketPayload.Type<CreateOfferPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "create_offer_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CreateOfferPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), CreateOfferPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String listingId;
    private final double moneyOffer;
    private final List<ItemStack> itemOffers;
    
    public CreateOfferPacket(String listingId, double moneyOffer, List<ItemStack> itemOffers) {
        this.listingId = listingId;
        this.moneyOffer = moneyOffer;
        this.itemOffers = itemOffers;
    }
    
    public CreateOfferPacket(FriendlyByteBuf buf) {
        this.listingId = buf.readUtf(36);
        this.moneyOffer = buf.readDouble();
        int itemCount = buf.readInt();
        this.itemOffers = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            this.itemOffers.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        }
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(listingId, 36);
        buf.writeDouble(moneyOffer);
        buf.writeInt(itemOffers.size());
        for (ItemStack stack : itemOffers) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, stack);
        }
    }
    
    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer buyer = (ServerPlayer) ctx.player();
            if (buyer == null) return;
            
            MineBayManager mineBayManager = MineBayManager.getInstance();
            EconomyManager economyManager = EconomyManager.getInstance();
            
            MineBayListing listing = mineBayManager.getListing(listingId);
            
            // Validation
            if (listing == null) {
                buyer.sendSystemMessage(Component.literal("§cListing not found!"));
                return;
            }
            
            if (listing.getStatus() != MineBayListing.ListingStatus.ACTIVE) {
                buyer.sendSystemMessage(Component.literal("§cThis listing is no longer active!"));
                return;
            }
            
            if (listing.getOfferType() != MineBayListing.OfferType.NEGOTIABLE) {
                buyer.sendSystemMessage(Component.literal("§cThis listing doesn't accept offers!"));
                return;
            }
            
            if (listing.getSellerId().equals(buyer.getUUID())) {
                buyer.sendSystemMessage(Component.literal("§cYou cannot make an offer on your own listing!"));
                return;
            }
            
            // Check if buyer has sufficient money
            if (moneyOffer > 0) {
                double buyerBalance = economyManager.getOrCreateAccount(buyer.getUUID()).getBalance();
                if (buyerBalance < moneyOffer) {
                    buyer.sendSystemMessage(Component.literal("§cYou don't have enough money! Need $" + 
                        String.format("%.2f", moneyOffer) + " but only have $" + 
                        String.format("%.2f", buyerBalance)));
                    return;
                }
            }
            
            // Check if buyer has the items they're offering
            for (ItemStack offeredStack : itemOffers) {
                if (offeredStack.isEmpty()) continue;
                
                int found = 0;
                for (ItemStack invStack : buyer.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(invStack, offeredStack)) {
                        found += invStack.getCount();
                    }
                }
                
                if (found < offeredStack.getCount()) {
                    buyer.sendSystemMessage(Component.literal("§cYou don't have enough " + 
                        offeredStack.getDisplayName().getString() + "! Need " + 
                        offeredStack.getCount() + " but only have " + found));
                    return;
                }
            }
            
            // Escrow money from buyer's account (atomic check-and-deduct)
            boolean moneyEscrowed = false;
            if (moneyOffer > 0) {
                com.servermanagement.features.economy.BankAccount buyerAccount = 
                    economyManager.getOrCreateAccount(buyer.getUUID());
                if (!buyerAccount.tryWithdraw(moneyOffer)) {
                    buyer.sendSystemMessage(Component.literal("§cInsufficient funds to escrow!"));
                    return;
                }
                moneyEscrowed = true;
            }
            
            // Create the offer with escrow rollback on failure
            try {
                // Remove offered items from buyer inventory (escrow)
                for (ItemStack offeredStack : itemOffers) {
                    if (offeredStack.isEmpty()) continue;
                    int remaining = offeredStack.getCount();
                    for (int i = 0; i < buyer.getInventory().items.size() && remaining > 0; i++) {
                        ItemStack invStack = buyer.getInventory().items.get(i);
                        if (ItemStack.isSameItemSameComponents(invStack, offeredStack)) {
                            int toRemove = Math.min(remaining, invStack.getCount());
                            invStack.shrink(toRemove);
                            remaining -= toRemove;
                        }
                    }
                }
                
                MineBayOffer offer = new MineBayOffer(
                    listingId,
                    buyer.getUUID(),
                    buyer.getName().getString(),
                    moneyOffer,
                    itemOffers
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
                }
                buyer.sendSystemMessage(Component.literal("§cFailed to create offer. Your money/items have been returned."));
                com.servermanagement.ServerManagementMod.LOGGER.error("Failed to create offer for listing {}", listingId, e);
                return;
            }
            
            // Notify buyer
            buyer.sendSystemMessage(Component.literal("§aOffer submitted successfully! (Money/items escrowed)"));
            buyer.sendSystemMessage(Component.literal("§7The seller will be notified of your offer."));
            
            // Notify seller if online
            ServerPlayer seller = buyer.server.getPlayerList().getPlayer(listing.getSellerId());
            if (seller != null) {
                seller.sendSystemMessage(Component.literal("§e[MineBay] New offer received!"));
                seller.sendSystemMessage(Component.literal("§7" + buyer.getName().getString() + 
                    " made an offer on your " + listing.getItemForSale().getDisplayName().getString()));
                seller.sendSystemMessage(Component.literal("§7Money: $" + String.format("%.2f", moneyOffer)));
                if (!itemOffers.isEmpty()) {
                    seller.sendSystemMessage(Component.literal("§7Items: " + itemOffers.size() + " item(s)"));
                }
                seller.sendSystemMessage(Component.literal("§7Use /minebay to view and accept/reject offers"));
            }
        });
        
    }
}
