package com.servermanagement.gui.minebay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.gui.ScreenScaler;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Main MineBay screen - Browse and create listings
 */
public class MineBayScreen extends AbstractContainerScreen<MineBayMenu> {
    
    private ScreenState currentState;
    private List<MineBayListing> listings;
    private List<MineBayListing> allListings; // Store all listings
    private boolean showingMyListings = false; // Track filter mode
    private int scrollOffset = 0;
    private static final int LISTINGS_PER_PAGE = 4; // Changed from 2 to 4
    private float emptyStateAnimation = 0f; // Animation counter for empty state
    
    // For creating new listings
    private EditBox moneyPriceBox;
    private EditBox marginPercentBox; // Margin % input for dynamic pricing
    private EditBox[] priceAmountBoxes = new EditBox[3]; // Amount inputs for 3 price items
    private PriceItemEntry[] priceItems = new PriceItemEntry[3]; // Price items (items buyer must provide)
    private boolean[] useStacks = new boolean[3]; // Stack mode toggle for each price item
    private boolean itemPlaced = false;
    private ItemStack placedItem = ItemStack.EMPTY;
    private MineBayListing.OfferType selectedOfferType = MineBayListing.OfferType.FIXED;
    
    // Edit state
    private MineBayListing listingBeingEdited = null;
    private boolean isEditMode = false;
    
    // Negotiation state
    private MineBayListing selectedListingForOffer = null;
    private double offerMoney = 0.0;
    private ItemStack[] offerItems = new ItemStack[3];
    
    // Delete confirmation state
    private MineBayListing listingToDelete = null;
    
    // View details state
    private MineBayListing selectedListingForDetails = null;
    
    // Buy confirmation state
    private MineBayListing listingToBuy = null;
    private PaymentMode buyPaymentMode = PaymentMode.NONE;
    private java.util.Set<Integer> selectedPaymentSlots = new java.util.HashSet<>();
    
    private enum PaymentMode { NONE, BALANCE, ITEMS }
    
    // View offers state
    private MineBayListing selectedListingForOffers = null;
    private int offerScrollOffset = 0;
    private static final int OFFERS_PER_PAGE = 3;
    private List<com.servermanagement.features.minebay.MineBayOffer> cachedOffers = new ArrayList<>();
    private boolean offersLoading = false;
    
    // Status message (shown briefly after actions)
    private String statusMessage = null;
    private long statusMessageTime = 0;
    private int statusMessageColor = 0x55FF55;
    
    public enum ScreenState {
        BROWSE,              // Browse all listings (default, shows inventory only if creating)
        CREATE_STEP1,        // Place seller item (shows inventory)
        CREATE_STEP2,        // Enter price details + price items
        CREATE_STEP3,        // Final confirmation (hides inventory)
        VIEW_DETAILS,        // View listing details and offers
        MAKE_OFFER,          // Make an offer on a negotiable listing
        VIEW_MY_LISTINGS,    // View your own listings and manage offers
        DELETE_CONFIRM,      // Confirmation dialog for deleting a listing
        BUY_CONFIRM,         // Confirmation dialog for buying a listing
        VIEW_OFFERS          // View and manage offers on own listing
    }
    
    public MineBayScreen(MineBayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 600;
        this.imageHeight = 400;
        this.currentState = ScreenState.BROWSE; // Default to browse
        this.allListings = com.servermanagement.client.ClientMineBayData.getListings(); // Load from cache
        this.listings = new ArrayList<>(allListings); // Start with all listings
        
        // Initialize price items
        for (int i = 0; i < 3; i++) {
            priceItems[i] = new PriceItemEntry();
            useStacks[i] = false;
        }
        
        // Initialize offer items
        for (int i = 0; i < 3; i++) {
            offerItems[i] = ItemStack.EMPTY;
        }
        
        // Adjust label positions
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 1000; // Hide title
    }
    
    @Override
    protected void init() {
        int[] dim = ScreenScaler.scale(600, 400, this.width, this.height);
        this.imageWidth = dim[0];
        this.imageHeight = dim[1];
        this.inventoryLabelY = this.imageHeight - 94;
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Clear all widgets to rebuild
        this.clearWidgets();
        
        // Update inventory visibility based on state
        updateInventoryVisibility();
        
        // Close button (always visible)
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 90, centerY + 5, 80, 20,
            Component.literal("Close"),
            button -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        switch (currentState) {
            case BROWSE:
                initBrowseScreen(centerX, centerY);
                break;
            case CREATE_STEP1:
                initCreateStep1(centerX, centerY);
                break;
            case CREATE_STEP2:
                initCreateStep2(centerX, centerY);
                break;
            case CREATE_STEP3:
                initCreateStep3(centerX, centerY);
                break;
            case MAKE_OFFER:
                initMakeOffer(centerX, centerY);
                break;
            case VIEW_DETAILS:
                initViewDetails(centerX, centerY);
                break;
            case VIEW_MY_LISTINGS:
                initMyListings(centerX, centerY);
                break;
            case DELETE_CONFIRM:
                initDeleteConfirm(centerX, centerY);
                break;
            case BUY_CONFIRM:
                initBuyConfirm(centerX, centerY);
                break;
            case VIEW_OFFERS:
                initViewOffers(centerX, centerY);
                break;
        }
    }
    
    /**
     * Update inventory visibility based on current screen state
     */
    private void updateInventoryVisibility() {
        // Show inventory on CREATE_STEP1 (placing item to sell) and MAKE_OFFER (offering items)
        boolean shouldShowInventory = (currentState == ScreenState.CREATE_STEP1 || currentState == ScreenState.MAKE_OFFER);
        this.menu.setInventoryVisible(shouldShowInventory);
        
        // Show offering slot only on CREATE_STEP1
        boolean shouldShowOffering = (currentState == ScreenState.CREATE_STEP1);
        this.menu.setOfferingSlotVisible(shouldShowOffering);
        
        // Show offer slots only on MAKE_OFFER
        boolean shouldShowOfferSlots = (currentState == ScreenState.MAKE_OFFER);
        this.menu.setOfferSlotsVisible(shouldShowOfferSlots);
    }
    
    private void initBrowseScreen(int centerX, int centerY) {
        // All Listings button (top left, PRIMARY as it's the default/home view)
        ModernButton.ButtonStyle allListingsStyle = showingMyListings ? 
            ModernButton.ButtonStyle.SECONDARY : ModernButton.ButtonStyle.PRIMARY;
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 120, 25,
            Component.literal("All Listings"),
            button -> showAllListings(),
            allListingsStyle
        ));
        
        // My Listings button (next to All Listings)
        ModernButton.ButtonStyle myListingsStyle = showingMyListings ? 
            ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY;
        this.addRenderableWidget(new ModernButton(
            centerX + 140, centerY + 5, 120, 25,
            Component.literal("My Listings"),
            button -> filterMyListings(),
            myListingsStyle
        ));
        
        // If no listings, show empty state with create button in center
        if (listings.isEmpty()) {
            // Create Listing button (centered below empty state text)
            this.addRenderableWidget(new ModernButton(
                centerX + (this.imageWidth / 2) - 75, centerY + 170, 150, 30,
                Component.literal("+ Create Listing"),
                button -> switchState(ScreenState.CREATE_STEP1),
                ModernButton.ButtonStyle.SUCCESS
            ));
            return; // Don't show scroll buttons
        }
        
        // If there are listings, show create button in top right
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 220, centerY + 5, 120, 25,
            Component.literal("+ Create New"),
            button -> switchState(ScreenState.CREATE_STEP1),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        // Scroll buttons - below card area (cards end at ~centerY+275)
        int navY = centerY + 280;
        if (scrollOffset > 0) {
            this.addRenderableWidget(new ModernButton(
                centerX + 50, navY, 90, 18,
                Component.literal("◀ Previous"),
                button -> {
                    scrollOffset--;
                    this.rebuildWidgets();
                },
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
        
        if (scrollOffset + LISTINGS_PER_PAGE < listings.size()) {
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 140, navY, 90, 18,
                Component.literal("Next ▶"),
                button -> {
                    scrollOffset++;
                    this.rebuildWidgets();
                },
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
        
        // Render listing action buttons
        for (int i = 0; i < Math.min(LISTINGS_PER_PAGE, listings.size() - scrollOffset); i++) {
            int listingIndex = i + scrollOffset;
            MineBayListing listing = listings.get(listingIndex);
            int yPos = centerY + 60 + (i * 55); // Match rendering spacing
            
            // Check if this is the player's own listing
            boolean isOwnListing = minecraft != null && minecraft.player != null && 
                listing.getSellerId().equals(minecraft.player.getUUID());
            
            if (showingMyListings) {
                // In "My Listings" view - show Edit and Delete buttons stacked on right side
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 130, yPos + 5, 100, 18,
                    Component.literal("Edit"),
                    button -> editListing(listing),
                    ModernButton.ButtonStyle.PRIMARY
                ));
                
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 130, yPos + 27, 100, 18,
                    Component.literal("Delete"),
                    button -> deleteListing(listing),
                    ModernButton.ButtonStyle.DANGER
                ));
            } else if (!isOwnListing) {
                // In "All Listings" view - only show Buy/Negotiate if NOT own listing
                if (listing.getOfferType() == MineBayListing.OfferType.FIXED) {
                    this.addRenderableWidget(new ModernButton(
                        centerX + this.imageWidth - 130, yPos + 5, 100, 18,
                        Component.literal("Buy Now"),
                        button -> buyListing(listing),
                        ModernButton.ButtonStyle.SUCCESS
                    ));
                } else {
                    this.addRenderableWidget(new ModernButton(
                        centerX + this.imageWidth - 130, yPos + 5, 100, 18,
                        Component.literal("Negotiate"),
                        button -> negotiateListing(listing),
                        ModernButton.ButtonStyle.PRIMARY
                    ));
                }
                
                // View Details button below action button
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 130, yPos + 27, 100, 18,
                    Component.literal("Details"),
                    button -> viewListingDetails(listing),
                    ModernButton.ButtonStyle.SECONDARY
                ));
            } else {
                // Own listing in All Listings view - show Details only
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 130, yPos + 15, 100, 20,
                    Component.literal("Details"),
                    button -> viewListingDetails(listing),
                    ModernButton.ButtonStyle.SECONDARY
                ));
            }
        }
    }
    
    private void initCreateStep1(int centerX, int centerY) {
        // Step 1: Place Item to Sell
        // Back button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> {
                // Clear edit mode when going back
                isEditMode = false;
                listingBeingEdited = null;
                switchState(ScreenState.BROWSE);
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Action button positioned inside content area (above inventory)
        int contentBottom = centerY + 46 + (menu.isInventoryVisible() ? 165 : (this.imageHeight - 60));
        int buttonY = contentBottom - 40;
        ItemStack currentOffering = this.menu.getOfferingItem();
        boolean hasItem = !currentOffering.isEmpty();
        
        this.addRenderableWidget(new ModernButton(
            centerX + (this.imageWidth - 170) / 2, buttonY, 170, 30,
            Component.literal(hasItem ? "Next: Set Prices \u2192" : "Place Item"),
            button -> {
                ItemStack offeringItem = this.menu.getOfferingItem();
                if (!offeringItem.isEmpty()) {
                    this.placedItem = offeringItem.copy();
                    this.itemPlaced = true;
                    switchState(ScreenState.CREATE_STEP2);
                }
            },
            hasItem ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void initCreateStep2(int centerX, int centerY) {
        // Step 2: Set Prices (item is already placed from Step 1)
        int formX = centerX + 15;
        int formY = centerY + 76; // Below title + underline + spacing
        
        // Back button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> switchState(ScreenState.CREATE_STEP1),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Layout widths relative to imageWidth
        int availWidth = this.imageWidth - 30; // total form width
        int priceW = (int)(availWidth * 0.40); // 40% for price input
        int marginW = (int)(availWidth * 0.20); // 20% for margin input
        int typeW = (int)(availWidth * 0.35); // 35% for type button
        
        // Money price input
        if (moneyPriceBox == null) {
            moneyPriceBox = new EditBox(this.font, formX, formY, priceW, 18, Component.literal("Money Price"));
            moneyPriceBox.setMaxLength(10);
            if (isEditMode && listingBeingEdited != null) {
                moneyPriceBox.setValue(String.format(Locale.US, "%.2f", listingBeingEdited.getMoneyPrice()));
            } else if (!placedItem.isEmpty()) {
                // Auto-populate with market base price for the placed item
                double basePrice = com.servermanagement.client.ClientMarketData.getStackPrice(placedItem);
                moneyPriceBox.setValue(String.format(Locale.US, "%.2f", basePrice));
            } else {
                moneyPriceBox.setValue("0");
            }
            moneyPriceBox.setHint(Component.literal("$..."));
            moneyPriceBox.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        } else {
            moneyPriceBox.setPosition(formX, formY);
            moneyPriceBox.setWidth(priceW);
        }
        this.addRenderableWidget(moneyPriceBox);
        
        // Margin % input (dynamic pricing)
        int marginX = formX + priceW + 10;
        if (marginPercentBox == null) {
            marginPercentBox = new EditBox(this.font, marginX, formY, marginW, 18, Component.literal("Margin %"));
            marginPercentBox.setMaxLength(5);
            if (isEditMode && listingBeingEdited != null) {
                marginPercentBox.setValue(String.valueOf((int) listingBeingEdited.getMarginPercent()));
            } else {
                marginPercentBox.setValue("10"); // Default 10% margin
            }
            marginPercentBox.setHint(Component.literal("%"));
            marginPercentBox.setFilter(s -> s.matches("-?\\d*"));
        } else {
            marginPercentBox.setPosition(marginX, formY);
            marginPercentBox.setWidth(marginW);
        }
        this.addRenderableWidget(marginPercentBox);
        
        // Offer type selector
        int typeX = marginX + marginW + 10;
        this.addRenderableWidget(new ModernButton(
            typeX, formY - 2, typeW, 22,
            Component.literal(selectedOfferType == MineBayListing.OfferType.FIXED ? 
                "Fixed Price" : "Negotiable"),
            button -> {
                selectedOfferType = selectedOfferType == MineBayListing.OfferType.FIXED ? 
                    MineBayListing.OfferType.NEGOTIABLE : MineBayListing.OfferType.FIXED;
                this.rebuildWidgets();
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Price Items section (3 slots)
        int priceItemY = formY + 65;
        for (int i = 0; i < 3; i++) {
            int slotY = priceItemY + (i * 28);
            
            // Amount input for each price item
            if (priceAmountBoxes[i] == null) {
                priceAmountBoxes[i] = new EditBox(this.font, formX + 22, slotY + 1, 35, 16, 
                    Component.literal("Amount"));
                priceAmountBoxes[i].setMaxLength(4);
                priceAmountBoxes[i].setValue("1");
                priceAmountBoxes[i].setFilter(s -> s.matches("\\d*"));
            } else {
                priceAmountBoxes[i].setPosition(formX + 22, slotY + 1);
            }
            this.addRenderableWidget(priceAmountBoxes[i]);
            
            // Stack/Count toggle button for each price item
            final int index = i;
            this.addRenderableWidget(new ModernButton(
                formX + 62, slotY - 1, 55, 20,
                Component.literal(useStacks[i] ? "Stacks" : "Items"),
                button -> {
                    useStacks[index] = !useStacks[index];
                    this.rebuildWidgets();
                },
                ModernButton.ButtonStyle.SECONDARY
            ));
            
            // Clear button (only show if item is set)
            if (priceItems[i] != null && !priceItems[i].isEmpty()) {
                this.addRenderableWidget(new ModernButton(
                    formX + 122, slotY - 1, 18, 20,
                    Component.literal("x"),
                    button -> {
                        priceItems[index] = new PriceItemEntry();
                        if (priceAmountBoxes[index] != null) {
                            priceAmountBoxes[index].setValue("1");
                        }
                        useStacks[index] = false;
                        this.rebuildWidgets();
                    },
                    ModernButton.ButtonStyle.DANGER
                ));
            }
        }
        
        // Next button
        String nextButtonText = isEditMode ? "Next: Confirm Changes \u2192" : "Next: Confirm \u2192";
        int buttonWidth = isEditMode ? 200 : 160;
        
        this.addRenderableWidget(new ModernButton(
            formX, priceItemY + (3 * 28) + 10, buttonWidth, 25,
            Component.literal(nextButtonText),
            button -> switchState(ScreenState.CREATE_STEP3),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    
    private void initCreateStep3(int centerX, int centerY) {
        // If in edit mode, place the item in the offering slot
        if (isEditMode && !placedItem.isEmpty()) {
            this.menu.setOfferingItem(placedItem.copy());
        }
        
        // Buttons at bottom of content area
        int buttonY = centerY + 185;
        
        // Cancel button - returns item
        this.addRenderableWidget(new ModernButton(
            centerX + 150, buttonY, 120, 30,
            Component.literal("✗ Cancel"),
            button -> {
                boolean wasEditMode = isEditMode;
                // Clear edit mode when canceling
                isEditMode = false;
                listingBeingEdited = null;
                if (wasEditMode) {
                    // In edit mode, the item was copied from the existing listing,
                    // not taken from inventory. Just clear the offering slot without
                    // returning the item to prevent duplication.
                    this.menu.clearOfferingSlot();
                    itemPlaced = false;
                    placedItem = ItemStack.EMPTY;
                    switchState(ScreenState.BROWSE);
                } else {
                    cancelListing();
                }
            },
            ModernButton.ButtonStyle.DANGER
        ));
        
        // Confirm button - creates listing (or updates if editing)
        String confirmText = isEditMode ? "✓ Update Listing" : "✓ Create Listing";
        this.addRenderableWidget(new ModernButton(
            centerX + 280, buttonY, 140, 30,
            Component.literal(confirmText),
            button -> confirmListing(),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    
    private void switchState(ScreenState newState) {
        ScreenState oldState = this.currentState;
        this.currentState = newState;
        
        // Reset EditBox references when leaving create/edit flow
        // so they get recreated fresh when re-entering
        if (oldState == ScreenState.CREATE_STEP2 || oldState == ScreenState.CREATE_STEP3) {
            if (newState == ScreenState.BROWSE || newState == ScreenState.DELETE_CONFIRM || 
                newState == ScreenState.BUY_CONFIRM) {
                moneyPriceBox = null;
                marginPercentBox = null;
                for (int i = 0; i < priceAmountBoxes.length; i++) {
                    priceAmountBoxes[i] = null;
                }
            }
        }
        
        // Update inventory visibility immediately
        updateInventoryVisibility();
        
        // Rebuild the entire GUI
        this.rebuildWidgets();
    }
    
    private void filterMyListings() {
        if (this.minecraft != null && this.minecraft.player != null) {
            // Filter to show only player's listings
            showingMyListings = true;
            listings = new ArrayList<>();
            for (MineBayListing listing : allListings) {
                if (listing.getSellerId().equals(minecraft.player.getUUID())) {
                    listings.add(listing);
                }
            }
            scrollOffset = 0;
            this.rebuildWidgets(); // Use rebuildWidgets instead of init for proper refresh
        }
    }
    
    private void showAllListings() {
        // Show all listings
        showingMyListings = false;
        listings = new ArrayList<>(allListings);
        scrollOffset = 0;
        this.rebuildWidgets(); // Use rebuildWidgets instead of init for proper refresh
    }
    
    private void viewListingDetails(MineBayListing listing) {
        selectedListingForDetails = listing;
        switchState(ScreenState.VIEW_DETAILS);
    }
    
    private void buyListing(MineBayListing listing) {
        // Show buy confirmation dialog
        listingToBuy = listing;
        buyPaymentMode = PaymentMode.NONE;
        selectedPaymentSlots.clear();
        switchState(ScreenState.BUY_CONFIRM);
    }
    
    private void confirmBuy() {
        if (listingToBuy == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        boolean hasMoneyPrice = listingToBuy.getMoneyPrice() > 0;
        // Determine effective payment mode
        int paymentModeId;
        int[] slotsArray;
        if (!hasMoneyPrice || buyPaymentMode == PaymentMode.BALANCE) {
            paymentModeId = 0; // BALANCE
            slotsArray = new int[0];
        } else {
            paymentModeId = 1; // ITEMS
            slotsArray = selectedPaymentSlots.stream().mapToInt(Integer::intValue).toArray();
        }
        
        // Send purchase packet to server with payment mode
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.PurchaseListingPacket(
                listingToBuy.getListingId(), paymentModeId, slotsArray)
        );
        
        // Show status message and return to browse
        showStatusMessage("Purchase request sent!", 0x55FF55);
        
        // Remove from local cache optimistically
        allListings.remove(listingToBuy);
        listings.remove(listingToBuy);
        listingToBuy = null;
        buyPaymentMode = PaymentMode.NONE;
        selectedPaymentSlots.clear();
        
        switchState(ScreenState.BROWSE);
    }
    
    private double getSelectedItemsTotal() {
        if (minecraft == null || minecraft.player == null) return 0.0;
        double total = 0.0;
        for (int slot : selectedPaymentSlots) {
            if (slot >= 0 && slot < minecraft.player.getInventory().items.size()) {
                ItemStack stack = minecraft.player.getInventory().items.get(slot);
                if (!stack.isEmpty()) {
                    total += com.servermanagement.client.ClientMarketData.getStackPrice(stack);
                }
            }
        }
        return total;
    }
    
    private void cancelBuy() {
        listingToBuy = null;
        buyPaymentMode = PaymentMode.NONE;
        selectedPaymentSlots.clear();
        switchState(ScreenState.BROWSE);
    }
    
    private void negotiateListing(MineBayListing listing) {
        selectedListingForOffer = listing;
        offerMoney = listing.getMoneyPrice(); // Start with asking price
        for (int i = 0; i < 3; i++) {
            offerItems[i] = ItemStack.EMPTY.copy();
        }
        switchState(ScreenState.MAKE_OFFER);
    }
    
    private void editListing(MineBayListing listing) {
        // Set edit mode and populate form with existing listing data
        isEditMode = true;
        listingBeingEdited = listing;
        
        // Pre-populate the item being sold
        itemPlaced = true;
        placedItem = listing.getItemForSale().copy();
        
        // Pre-populate offer type
        selectedOfferType = listing.getOfferType();
        
        // Pre-populate price items
        List<PriceItemEntry> existingPriceItems = listing.getPriceItems();
        for (int i = 0; i < 3; i++) {
            if (i < existingPriceItems.size() && !existingPriceItems.get(i).isEmpty()) {
                PriceItemEntry existing = existingPriceItems.get(i);
                priceItems[i] = new PriceItemEntry(
                    existing.getItemStack(),
                    existing.getAmount(),
                    existing.isUseStacks()
                );
                useStacks[i] = existing.isUseStacks();
            } else {
                priceItems[i] = new PriceItemEntry();
                useStacks[i] = false;
            }
        }
        
        // Go to CREATE_STEP2 to edit prices (item is already placed, skip Step 1)
        switchState(ScreenState.CREATE_STEP2);
    }
    
    private void deleteListing(MineBayListing listing) {
        // Show confirmation dialog instead of immediately deleting
        listingToDelete = listing;
        switchState(ScreenState.DELETE_CONFIRM);
    }
    
    private void confirmDelete() {
        if (listingToDelete == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        // Send delete listing packet to server
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.DeleteListingPacket(listingToDelete.getListingId())
        );
        
        // Remove from local cache
        allListings.remove(listingToDelete);
        listings.remove(listingToDelete);
        
        // Clear the deletion state
        listingToDelete = null;
        
        // Return to browse screen with feedback
        showStatusMessage("Listing deleted", 0xFFAA00);
        switchState(ScreenState.BROWSE);
    }
    
    private void cancelDelete() {
        // Clear the deletion state and return to browse
        listingToDelete = null;
        switchState(ScreenState.BROWSE);
    }
    
    private void cancelListing() {
        // Return the offering slot item to player inventory
        ItemStack offeringItem = this.menu.getOfferingItem();
        if (!offeringItem.isEmpty() && minecraft != null && minecraft.player != null) {
            // Move item back to player inventory on client side
            minecraft.player.getInventory().placeItemBackInInventory(offeringItem.copy());
            this.menu.clearOfferingSlot();
        }
        
        itemPlaced = false;
        placedItem = ItemStack.EMPTY;
        switchState(ScreenState.BROWSE);
    }
    
    private void initMakeOffer(int centerX, int centerY) {
        if (selectedListingForOffer == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        int formX = centerX + 15;
        int formY = centerY + 50; // Header area
        
        // Back button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> {
                selectedListingForOffer = null;
                switchState(ScreenState.BROWSE);
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Money offer input
        int inputWidth = Math.min(250, this.imageWidth - 40);
        EditBox moneyInput = new EditBox(this.font, formX, formY + 55, inputWidth, 20, Component.literal("Money Offer"));
        moneyInput.setValue(String.format(Locale.US, "%.2f", offerMoney));
        moneyInput.setResponder(value -> {
            try {
                offerMoney = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                offerMoney = 0.0;
            }
        });
        this.addRenderableWidget(moneyInput);
        
        // Offer slots are real container slots (handled by the menu system)
        // Player places items from inventory into these slots
        
        // Submit offer button (above inventory slots which start at menu-relative Y=230)
        int btnWidth = (this.imageWidth - 50) / 2;
        int btnY = centerY + 200;
        this.addRenderableWidget(new ModernButton(
            formX, btnY, btnWidth, 25,
            Component.literal("Submit Offer"),
            button -> submitOffer(),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        // Cancel button
        this.addRenderableWidget(new ModernButton(
            formX + btnWidth + 10, btnY, btnWidth, 25,
            Component.literal("Cancel"),
            button -> {
                selectedListingForOffer = null;
                switchState(ScreenState.BROWSE);
            },
            ModernButton.ButtonStyle.DANGER
        ));
    }
    
    private void initViewDetails(int centerX, int centerY) {
        if (selectedListingForDetails == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        // Back button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> {
                selectedListingForDetails = null;
                switchState(ScreenState.BROWSE);
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        MineBayListing listing = selectedListingForDetails;
        boolean isOwnListing = minecraft != null && minecraft.player != null && 
            listing.getSellerId().equals(minecraft.player.getUUID());
        
        int buttonY = centerY + 210;
        
        if (!isOwnListing) {
            if (listing.getOfferType() == MineBayListing.OfferType.FIXED) {
                this.addRenderableWidget(new ModernButton(
                    centerX + 200, buttonY, 120, 25,
                    Component.literal("Buy Now"),
                    button -> buyListing(listing),
                    ModernButton.ButtonStyle.SUCCESS
                ));
            } else {
                this.addRenderableWidget(new ModernButton(
                    centerX + 200, buttonY, 120, 25,
                    Component.literal("Make Offer"),
                    button -> negotiateListing(listing),
                    ModernButton.ButtonStyle.PRIMARY
                ));
            }
        } else {
            this.addRenderableWidget(new ModernButton(
                centerX + 100, buttonY, 100, 25,
                Component.literal("Edit"),
                button -> editListing(listing),
                ModernButton.ButtonStyle.PRIMARY
            ));
            
            // View Offers button for negotiable own listings
            if (listing.getOfferType() == MineBayListing.OfferType.NEGOTIABLE) {
                int pendingCount = listing.getPendingOfferCount();
                String btnText = pendingCount > 0 ? "Offers (" + pendingCount + ")" : "Offers";
                this.addRenderableWidget(new ModernButton(
                    centerX + 210, buttonY, 100, 25,
                    Component.literal(btnText),
                    button -> {
                        selectedListingForOffers = listing;
                        offerScrollOffset = 0;
                        cachedOffers.clear();
                        offersLoading = true;
                        // Request offers from server
                        ModNetworking.sendToServer(
                            new com.servermanagement.network.packet.minebay.RequestListingOffersPacket(
                                listing.getListingId()));
                        switchState(ScreenState.VIEW_OFFERS);
                    },
                    pendingCount > 0 ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
                ));
            }
            
            this.addRenderableWidget(new ModernButton(
                centerX + 320, buttonY, 100, 25,
                Component.literal("Delete"),
                button -> deleteListing(listing),
                ModernButton.ButtonStyle.DANGER
            ));
        }
    }
    
    private void initMyListings(int centerX, int centerY) {
        // My Listings is handled by the browse screen filter, redirect there
        showingMyListings = true;
        filterMyListings();
        switchState(ScreenState.BROWSE);
    }
    
    private void initDeleteConfirm(int centerX, int centerY) {
        if (listingToDelete == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        // Create a centered confirmation dialog
        int dialogWidth = 400;
        int dialogHeight = 200;
        int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
        int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
        
        // Title label is rendered in render method
        
        // Item info label is rendered in render method
        
        // Confirm button (destructive action)
        this.addRenderableWidget(new ModernButton(
            dialogX + 30, dialogY + dialogHeight - 50, 160, 30,
            Component.literal("✓ Yes, Delete"),
            button -> confirmDelete(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        // Cancel button
        this.addRenderableWidget(new ModernButton(
            dialogX + dialogWidth - 190, dialogY + dialogHeight - 50, 160, 30,
            Component.literal("✗ Cancel"),
            button -> cancelDelete(),
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void initBuyConfirm(int centerX, int centerY) {
        if (listingToBuy == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        boolean hasMoneyPrice = listingToBuy.getMoneyPrice() > 0;
        
        // Dialog sizing: taller when in ITEMS mode to fit inventory grid
        int dialogWidth = 400;
        int dialogHeight = (buyPaymentMode == PaymentMode.ITEMS) ? 310 : 220;
        int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
        int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
        
        if (buyPaymentMode == PaymentMode.NONE && hasMoneyPrice) {
            // Payment method selection buttons
            double balance = com.servermanagement.client.ClientBankData.getBalance();
            boolean canPayWithBalance = balance >= listingToBuy.getMoneyPrice();
            
            int btnWidth = 170;
            int btnY = dialogY + 110;
            
            // Pay with Balance button
            ModernButton balanceBtn = new ModernButton(
                dialogX + (dialogWidth / 2) - btnWidth - 5, btnY, btnWidth, 25,
                Component.literal("Pay with Balance"),
                button -> { buyPaymentMode = PaymentMode.BALANCE; this.rebuildWidgets(); },
                canPayWithBalance ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
            );
            if (!canPayWithBalance) balanceBtn.active = false;
            this.addRenderableWidget(balanceBtn);
            
            // Pay with Items button
            this.addRenderableWidget(new ModernButton(
                dialogX + (dialogWidth / 2) + 5, btnY, btnWidth, 25,
                Component.literal("Pay with Items"),
                button -> { buyPaymentMode = PaymentMode.ITEMS; selectedPaymentSlots.clear(); this.rebuildWidgets(); },
                ModernButton.ButtonStyle.PRIMARY
            ));
            
            // Cancel button
            this.addRenderableWidget(new ModernButton(
                dialogX + (dialogWidth - 100) / 2, dialogY + dialogHeight - 40, 100, 25,
                Component.literal("Cancel"),
                button -> cancelBuy(),
                ModernButton.ButtonStyle.SECONDARY
            ));
        } else if (buyPaymentMode == PaymentMode.BALANCE || !hasMoneyPrice) {
            // Balance payment - simple confirm/cancel
            this.addRenderableWidget(new ModernButton(
                dialogX + 30, dialogY + dialogHeight - 45, 160, 28,
                Component.literal("Confirm Purchase"),
                button -> confirmBuy(),
                ModernButton.ButtonStyle.SUCCESS
            ));
            
            this.addRenderableWidget(new ModernButton(
                dialogX + dialogWidth - 190, dialogY + dialogHeight - 45, 160, 28,
                Component.literal("Cancel"),
                button -> cancelBuy(),
                ModernButton.ButtonStyle.SECONDARY
            ));
        } else if (buyPaymentMode == PaymentMode.ITEMS) {
            // Item payment - confirm only when total selected >= price
            double selectedTotal = getSelectedItemsTotal();
            boolean canConfirm = selectedTotal >= listingToBuy.getMoneyPrice();
            
            ModernButton confirmBtn = new ModernButton(
                dialogX + 30, dialogY + dialogHeight - 45, 160, 28,
                Component.literal("Confirm Purchase"),
                button -> confirmBuy(),
                canConfirm ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
            );
            if (!canConfirm) confirmBtn.active = false;
            this.addRenderableWidget(confirmBtn);
            
            this.addRenderableWidget(new ModernButton(
                dialogX + dialogWidth - 190, dialogY + dialogHeight - 45, 160, 28,
                Component.literal("Cancel"),
                button -> cancelBuy(),
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
    }
    
    private void initViewOffers(int centerX, int centerY) {
        if (selectedListingForOffers == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        // Back button (always available, even while loading)
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> {
                selectedListingForOffers = null;
                cachedOffers.clear();
                switchState(ScreenState.VIEW_DETAILS);
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Refresh button (always available)
        this.addRenderableWidget(new ModernButton(
            centerX + 120, centerY + 5, 80, 20,
            Component.literal("Refresh"),
            button -> {
                offersLoading = true;
                ModNetworking.sendToServer(
                    new com.servermanagement.network.packet.minebay.RequestListingOffersPacket(
                        selectedListingForOffers.getListingId()));
                this.rebuildWidgets();
            },
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Wait for server response before rendering offer cards
        if (offersLoading || cachedOffers.isEmpty()) return;
        
        // Pagination
        int navY = centerY + this.imageHeight - 40;
        if (offerScrollOffset > 0) {
            this.addRenderableWidget(new ModernButton(
                centerX + 50, navY, 90, 18,
                Component.literal("◀ Previous"),
                button -> { offerScrollOffset--; this.rebuildWidgets(); },
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
        if (offerScrollOffset + OFFERS_PER_PAGE < cachedOffers.size()) {
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 140, navY, 90, 18,
                Component.literal("Next ▶"),
                button -> { offerScrollOffset++; this.rebuildWidgets(); },
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
        
        // Accept/Reject buttons per offer
        for (int i = 0; i < Math.min(OFFERS_PER_PAGE, cachedOffers.size() - offerScrollOffset); i++) {
            int offerIndex = i + offerScrollOffset;
            com.servermanagement.features.minebay.MineBayOffer offer = cachedOffers.get(offerIndex);
            int cardY = centerY + 90 + (i * 80);
            
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 130, cardY + 10, 100, 20,
                Component.literal("✓ Accept"),
                button -> {
                    ModNetworking.sendToServer(
                        new com.servermanagement.network.packet.minebay.AcceptOfferPacket(
                            selectedListingForOffers.getListingId(), offer.getOfferId()));
                    showStatusMessage("Offer accepted!", 0x55FF55);
                    selectedListingForOffers = null;
                    cachedOffers.clear();
                    switchState(ScreenState.BROWSE);
                },
                ModernButton.ButtonStyle.SUCCESS
            ));
            
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 130, cardY + 35, 100, 20,
                Component.literal("✗ Reject"),
                button -> {
                    ModNetworking.sendToServer(
                        new com.servermanagement.network.packet.minebay.RejectOfferPacket(
                            selectedListingForOffers.getListingId(), offer.getOfferId()));
                    showStatusMessage("Offer rejected.", 0xFF5555);
                    // Remove from cached list and rebuild
                    cachedOffers.remove(offer);
                    this.rebuildWidgets();
                },
                ModernButton.ButtonStyle.DANGER
            ));
        }
    }
    
    private void renderViewOffers(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (selectedListingForOffers == null) return;
        
        // Header
        Component title = Component.literal("Offers on: " + selectedListingForOffers.getItemForSale().getHoverName().getString());
        int tw = this.font.width(title);
        if (tw > this.imageWidth - 130) {
            String shortened = selectedListingForOffers.getItemForSale().getHoverName().getString();
            while (this.font.width("Offers on: " + shortened + "...") > this.imageWidth - 130 && shortened.length() > 5) {
                shortened = shortened.substring(0, shortened.length() - 1);
            }
            title = Component.literal("Offers on: " + shortened + "...");
            tw = this.font.width(title);
        }
        guiGraphics.drawString(this.font, title, centerX + 15, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + 15, centerY + 61, centerX + 15 + tw, centerY + 62, 0x60FFD700);
        
        // Asking price
        String askingStr = "Asking: $" + String.format(Locale.US, "%.2f", selectedListingForOffers.getMoneyPrice());
        guiGraphics.drawString(this.font, Component.literal(askingStr), centerX + 15, centerY + 67, 0x999999, true);
        
        // Loading state
        if (offersLoading) {
            guiGraphics.drawString(this.font, Component.literal("Loading offers..."),
                centerX + (this.imageWidth - this.font.width("Loading offers...")) / 2, 
                centerY + 130, 0xFFFF55, true);
            return;
        }
        
        if (cachedOffers.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("No pending offers."),
                centerX + (this.imageWidth - this.font.width("No pending offers.")) / 2, 
                centerY + 130, 0x888888, true);
            return;
        }
        
        // Page info
        int totalPages = (int) Math.ceil((double) cachedOffers.size() / OFFERS_PER_PAGE);
        int currentPage = offerScrollOffset + 1;
        String pageStr = "Page " + currentPage + "/" + totalPages + " (" + cachedOffers.size() + " offers)";
        guiGraphics.drawString(this.font, Component.literal(pageStr),
            centerX + (this.imageWidth - this.font.width(pageStr)) / 2, centerY + 78, 0xAAAAAA, true);
        
        // Render offer cards
        for (int i = 0; i < Math.min(OFFERS_PER_PAGE, cachedOffers.size() - offerScrollOffset); i++) {
            int offerIndex = i + offerScrollOffset;
            com.servermanagement.features.minebay.MineBayOffer offer = cachedOffers.get(offerIndex);
            int cardY = centerY + 90 + (i * 80);
            int cardX = centerX + 15;
            int cardRight = centerX + this.imageWidth - 140;
            
            // Card background
            guiGraphics.fill(cardX, cardY, cardRight, cardY + 72, 0xFF333333);
            guiGraphics.fill(cardX + 1, cardY + 1, cardRight - 1, cardY + 71, 0xFF1E1E1E);
            
            // Buyer name
            guiGraphics.drawString(this.font, Component.literal("From: §e" + offer.getBuyerName()),
                cardX + 8, cardY + 5, 0xFFFFFF, true);
            
            // Money offer
            if (offer.getMoneyOffer() > 0) {
                String moneyStr = "Money: $" + String.format(Locale.US, "%.2f", offer.getMoneyOffer());
                int moneyColor = offer.getMoneyOffer() >= selectedListingForOffers.getMoneyPrice() ? 0x55FF55 : 0xFFAA00;
                guiGraphics.drawString(this.font, Component.literal(moneyStr),
                    cardX + 8, cardY + 18, moneyColor, true);
            }
            
            // Item offers
            List<ItemStack> itemOffers = offer.getItemOffers();
            if (!itemOffers.isEmpty()) {
                guiGraphics.drawString(this.font, Component.literal("Items:"),
                    cardX + 8, cardY + 31, 0xAAAAAA, true);
                
                int itemDrawX = cardX + 45;
                for (int j = 0; j < Math.min(itemOffers.size(), 5); j++) {
                    ItemStack stack = itemOffers.get(j);
                    if (!stack.isEmpty()) {
                        guiGraphics.renderItem(stack, itemDrawX + (j * 20), cardY + 27);
                        guiGraphics.renderItemDecorations(this.font, stack, itemDrawX + (j * 20), cardY + 27);
                    }
                }
            }
            
            // Total value
            double totalValue = offer.getMoneyOffer();
            for (ItemStack stack : itemOffers) {
                totalValue += com.servermanagement.client.ClientMarketData.getStackPrice(stack);
            }
            String totalStr = "Total Value: $" + String.format(Locale.US, "%.2f", totalValue);
            int totalColor = totalValue >= selectedListingForOffers.getMoneyPrice() ? 0x55FF55 : 0xFFAA00;
            guiGraphics.drawString(this.font, Component.literal(totalStr),
                cardX + 8, cardY + 55, totalColor, true);
            
            // Time ago
            long elapsed = System.currentTimeMillis() - offer.getCreatedTimestamp();
            String timeStr = formatTimeAgo(elapsed);
            int timeW = this.font.width(timeStr);
            guiGraphics.drawString(this.font, Component.literal(timeStr),
                cardRight - timeW - 8, cardY + 55, 0x666666, true);
        }
    }
    
    private String formatTimeAgo(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
    
    private void submitOffer() {
        if (selectedListingForOffer == null || minecraft == null || minecraft.player == null) {
            return;
        }
        
        // Collect non-empty offer items from the container slots
        List<ItemStack> itemOffers = this.menu.getOfferItems();
        
        // Validate offer
        if (offerMoney <= 0 && itemOffers.isEmpty()) {
            showStatusMessage("You must offer money or items!", 0xFF5555);
            return;
        }
        
        // Send offer packet — items will be taken from inventory by server
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.CreateOfferPacket(
                selectedListingForOffer.getListingId(),
                offerMoney,
                itemOffers
            )
        );
        
        // Mark offer items as submitted (don't return on close)
        this.menu.clearOfferItems();
        
        // Show feedback and return to browse
        selectedListingForOffer = null;
        showStatusMessage("Offer submitted successfully!", 0x55FF55);
        switchState(ScreenState.BROWSE);
    }
    
    private void confirmListing() {
        // Get the actual item from the menu slot
        ItemStack offeringItem = this.menu.getOfferingItem();
        
        // Validate that we have an item
        if (offeringItem.isEmpty()) {
            showStatusMessage("No item was placed for listing!", 0xFF5555);
            return;
        }
        
        // Get money price
        double moneyPrice = 0.0;
        if (moneyPriceBox != null && !moneyPriceBox.getValue().isEmpty()) {
            try {
                moneyPrice = Double.parseDouble(moneyPriceBox.getValue());
            } catch (NumberFormatException e) {
                moneyPrice = 0.0;
            }
        }
        
        // Get margin percent
        double marginPercent = 0.0;
        if (marginPercentBox != null && !marginPercentBox.getValue().isEmpty()) {
            try {
                marginPercent = Double.parseDouble(marginPercentBox.getValue());
            } catch (NumberFormatException e) {
                marginPercent = 0.0;
            }
        }
        
        // Validate that either money or price items are set
        boolean hasPriceItems = false;
        for (PriceItemEntry entry : priceItems) {
            if (!entry.getItemStack().isEmpty()) {
                hasPriceItems = true;
                break;
            }
        }
        
        if (moneyPrice <= 0 && !hasPriceItems) {
            // No valid price set
            showStatusMessage("Please set a price (money or items)", 0xFF5555);
            return;
        }
        
        // If in edit mode, delete the old listing first
        if (isEditMode && listingBeingEdited != null) {
            com.servermanagement.network.ModNetworking.sendToServer(
                new com.servermanagement.network.packet.minebay.DeleteListingPacket(listingBeingEdited.getListingId())
            );
            
            // Remove from local cache
            allListings.remove(listingBeingEdited);
            listings.remove(listingBeingEdited);
        }
        
        // Collect non-empty price items
        List<PriceItemEntry> priceItemsList = new ArrayList<>();
        for (int i = 0; i < priceItems.length; i++) {
            if (priceItems[i] != null && !priceItems[i].isEmpty()) {
                // Update the amount from the text box if present
                if (priceAmountBoxes[i] != null && !priceAmountBoxes[i].getValue().isEmpty()) {
                    try {
                        int amount = Integer.parseInt(priceAmountBoxes[i].getValue());
                        priceItems[i].setAmount(amount);
                    } catch (NumberFormatException e) {
                        // Keep existing amount
                    }
                }
                // Update the useStacks mode
                priceItems[i].setUseStacks(useStacks[i]);
                priceItemsList.add(priceItems[i]);
            }
        }
        
        // Send packet to server to create listing (or create new one if editing)
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.CreateListingPacket(
                offeringItem.copy(), // Send the actual item
                moneyPrice,
                marginPercent,
                selectedOfferType,
                priceItemsList // Send the price items
            )
        );
        
        // Reset all state
        itemPlaced = false;
        boolean wasEdit = isEditMode;
        isEditMode = false;
        listingBeingEdited = null;
        placedItem = ItemStack.EMPTY;
        moneyPriceBox = null;
        marginPercentBox = null;
        for (int i = 0; i < priceAmountBoxes.length; i++) {
            priceAmountBoxes[i] = null;
            priceItems[i] = new PriceItemEntry();
            useStacks[i] = false;
        }
        
        showStatusMessage(wasEdit ? "Listing updated!" : "Listing created!", 0x55FF55);
        switchState(ScreenState.BROWSE);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Main background with border
        guiGraphics.fill(centerX - 2, centerY - 2, centerX + this.imageWidth + 2, centerY + this.imageHeight + 2, 0xFF000000);
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + this.imageHeight, 0xE0101010);
        
        // Header bar (with subtle bottom border)
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 46, 0xE0202020);
        guiGraphics.fill(centerX, centerY + 45, centerX + this.imageWidth, centerY + 46, 0xFF333333);
        
        // Content area - constrained bounds
        int contentAreaHeight = menu.isInventoryVisible() ? 165 : (this.imageHeight - 60);
        guiGraphics.fill(centerX + 10, centerY + 46, centerX + this.imageWidth - 10, 
            centerY + 46 + contentAreaHeight, 0xE01A1A1A);
        
        // Inventory area background (only when inventory is visible)
        if (menu.isInventoryVisible() && this.imageWidth > 420) {
            int invMargin = Math.max(10, (this.imageWidth - 180) / 2);
            guiGraphics.fill(centerX + invMargin, centerY + 220, centerX + this.imageWidth - invMargin, 
                centerY + this.imageHeight - 5, 0xE0202020);
            
            // Inventory border
            guiGraphics.fill(centerX + invMargin - 2, centerY + 218, centerX + this.imageWidth - invMargin + 2, centerY + 220, 0xFF555555);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only draw the inventory label when inventory is visible, centered above inventory area
        if (menu.isInventoryVisible()) {
            int labelX = (this.imageWidth - this.font.width(this.playerInventoryTitle)) / 2;
            guiGraphics.drawString(this.font, this.playerInventoryTitle, labelX, this.inventoryLabelY, 0xAAAAAA, true);
            
            // Total inventory value display
            if (minecraft != null && minecraft.player != null) {
                double totalValue = 0.0;
                for (ItemStack invStack : minecraft.player.getInventory().items) {
                    if (!invStack.isEmpty()) {
                        totalValue += com.servermanagement.client.ClientMarketData.getStackPrice(invStack);
                    }
                }
                
                String valueStr = String.format("Inventory Value: $%.2f", totalValue);
                int valueW = this.font.width(valueStr);
                int valueX = (this.imageWidth - valueW) / 2;
                
                // Background pill behind the value text
                guiGraphics.fill(valueX - 4, this.inventoryLabelY + 10, valueX + valueW + 4, this.inventoryLabelY + 22, 0xC0000000);
                guiGraphics.fill(valueX - 4, this.inventoryLabelY + 10, valueX + valueW + 4, this.inventoryLabelY + 11, 0xFF555555);
                
                guiGraphics.drawString(this.font, Component.literal(valueStr),
                    valueX, this.inventoryLabelY + 12, 0x55FFFF, true);
            }
        }
        // Never draw the title label (we render our own)
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Render header elements BEFORE scissor (so they aren't clipped)
        // Title (second row, below buttons)
        Component titleText = Component.literal("MineBay - Player Trading");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, 
            centerX + 10, 
            centerY + 34, 
            0xFFD700, true);
        
        // Balance display (second row, right-aligned)
        if (minecraft != null && minecraft.player != null) {
            double balance = com.servermanagement.client.ClientBankData.getBalance();
            String balanceStr = String.format("Balance: $%.2f", balance);
            int balanceW = this.font.width(balanceStr);
            guiGraphics.drawString(this.font, Component.literal(balanceStr),
                centerX + this.imageWidth - balanceW - 10,
                centerY + 34, 0x55FF55, true);
        }
        
        // Enable scissor for content area to prevent overflow
        int contentHeight = menu.isInventoryVisible() ? 165 : (this.imageHeight - 60);
        guiGraphics.enableScissor(
            centerX + 10,
            centerY + 46,
            centerX + this.imageWidth - 10,
            centerY + 46 + contentHeight
        );
        
        switch (currentState) {
            case BROWSE:
                renderBrowseScreen(guiGraphics, centerX, centerY);
                break;
            case CREATE_STEP1:
                renderCreateStep1(guiGraphics, centerX, centerY);
                break;
            case CREATE_STEP2:
                renderCreateStep2(guiGraphics, centerX, centerY);
                break;
            case CREATE_STEP3:
                renderCreateStep3(guiGraphics, centerX, centerY);
                break;
            case MAKE_OFFER:
                renderMakeOffer(guiGraphics, centerX, centerY);
                break;
            case VIEW_DETAILS:
                renderViewDetails(guiGraphics, centerX, centerY);
                break;
            case VIEW_MY_LISTINGS:
                renderMyListings(guiGraphics, centerX, centerY);
                break;
            case DELETE_CONFIRM:
                renderDeleteConfirm(guiGraphics, centerX, centerY);
                break;
            case BUY_CONFIRM:
                renderBuyConfirm(guiGraphics, centerX, centerY);
                break;
            case VIEW_OFFERS:
                renderViewOffers(guiGraphics, centerX, centerY);
                break;
        }
        
        // Disable scissor
        guiGraphics.disableScissor();
        
        // Render status message (above content, fades out after 3 seconds)
        if (statusMessage != null) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < 3000) {
                int alpha = elapsed > 2000 ? (int)(255 * (1.0 - (elapsed - 2000) / 1000.0)) : 255;
                if (alpha > 0) {
                    int color = (alpha << 24) | (statusMessageColor & 0x00FFFFFF);
                    Component msg = Component.literal(statusMessage);
                    int msgWidth = this.font.width(msg);
                    guiGraphics.drawString(this.font, msg, 
                        centerX + (this.imageWidth - msgWidth) / 2, centerY + 42, color, true);
                }
            } else {
                statusMessage = null;
            }
        }
        
        // Render tooltips with market price info (outside scissor region)
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Dynamically check for item placement in CREATE_STEP1
        if (currentState == ScreenState.CREATE_STEP1) {
            ItemStack currentItem = this.menu.getOfferingItem();
            boolean wasEmpty = placedItem.isEmpty();
            boolean nowHasItem = !currentItem.isEmpty();
            
            // If item state changed, update the display
            if (wasEmpty != !nowHasItem) {
                if (nowHasItem) {
                    this.placedItem = currentItem.copy();
                    this.itemPlaced = true;
                } else {
                    this.placedItem = ItemStack.EMPTY;
                    this.itemPlaced = false;
                }
                // Trigger a rebuild to update button states
                this.rebuildWidgets();
            }
        }
    }
    
    private void renderBrowseScreen(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (listings.isEmpty()) {
            // Empty state
            emptyStateAnimation += 0.05f;
            float bounce = (float) Math.sin(emptyStateAnimation) * 3;
            
            // Draw an empty box icon using fills
            int boxX = centerX + (this.imageWidth / 2) - 12;
            int boxY = (int)(centerY + 80 + bounce);
            guiGraphics.fill(boxX, boxY, boxX + 24, boxY + 20, 0xFF555555);
            guiGraphics.fill(boxX + 1, boxY + 1, boxX + 23, boxY + 19, 0xFF333333);
            guiGraphics.fill(boxX + 1, boxY + 8, boxX + 23, boxY + 10, 0xFF555555);
            
            Component noListingsText = Component.literal("No Active Listings");
            guiGraphics.drawCenteredString(this.font, noListingsText,
                centerX + (this.imageWidth / 2), centerY + 115, 0xFFFFFF);
            
            Component createHintText = Component.literal("Create a listing to start trading!");
            guiGraphics.drawCenteredString(this.font, createHintText,
                centerX + (this.imageWidth / 2), centerY + 130, 0xBBBBBB);
        } else {
            // Render listing cards
            for (int i = 0; i < Math.min(LISTINGS_PER_PAGE, listings.size() - scrollOffset); i++) {
                int listingIndex = i + scrollOffset;
                MineBayListing listing = listings.get(listingIndex);
                int yPos = centerY + 60 + (i * 55);
                
                // Card background with subtle border
                guiGraphics.fill(centerX + 20, yPos - 1, centerX + this.imageWidth - 20, yPos + 51, 0xFF333333);
                guiGraphics.fill(centerX + 21, yPos, centerX + this.imageWidth - 21, yPos + 50, 0xFF1E1E1E);
                
                // Left accent bar (color based on offer type)
                int accentColor = listing.getOfferType() == MineBayListing.OfferType.FIXED ? 
                    0xFF55FF55 : 0xFFFFAA00;
                guiGraphics.fill(centerX + 21, yPos, centerX + 24, yPos + 50, accentColor);
                
                // Item icon with slot background
                ItemStack itemForSale = listing.getItemForSale();
                int itemX = centerX + 32;
                int itemY = yPos + 8;
                
                guiGraphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0xFF555555);
                guiGraphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0xFF8B8B8B);
                guiGraphics.renderItem(itemForSale, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, itemForSale, itemX, itemY);
                
                // Item name (bold)
                String itemName = itemForSale.getHoverName().getString();
                if (itemName.length() > 22) {
                    itemName = itemName.substring(0, 20) + "...";
                }
                guiGraphics.drawString(this.font, 
                    Component.literal(itemName),
                    itemX + 22, itemY + 1, 0xFFFFFF, true);
                
                // Seller name
                guiGraphics.drawString(this.font, 
                    Component.literal("by " + listing.getSellerName()),
                    itemX + 22, itemY + 13, 0x999999, true);
                
                // Separator line before price
                guiGraphics.fill(centerX + 200, yPos + 5, centerX + 201, yPos + 45, 0xFF444444);
                
                // Price section
                int priceX = centerX + 210;
                int priceY = yPos + 6;
                
                // Money price
                if (listing.getMoneyPrice() > 0) {
                    guiGraphics.drawString(this.font, 
                        Component.literal("$" + String.format(Locale.US, "%.2f", listing.getMoneyPrice())),
                        priceX, priceY, 0x55FF55, true);
                    priceY += 11;
                    
                    // Show margin indicator if market pricing data is available
                    if (listing.getBaseMarketPrice() > 0) {
                        String marginStr = listing.getMarginPercent() >= 0 
                            ? "+" + String.format(Locale.US, "%.0f", listing.getMarginPercent()) + "%" 
                            : String.format(Locale.US, "%.0f", listing.getMarginPercent()) + "%";
                        int marginColor = listing.getMarginPercent() >= 0 ? 0x55FFFF : 0xFFAA00;
                        guiGraphics.drawString(this.font,
                            Component.literal(marginStr),
                            priceX, priceY, marginColor, true);
                        priceY += 11;
                    } else {
                        priceY += 2;
                    }
                }
                
                // Price items (render up to 3 inline)
                List<PriceItemEntry> cardPriceItems = listing.getPriceItems();
                if (cardPriceItems != null && !cardPriceItems.isEmpty()) {
                    for (int j = 0; j < Math.min(3, cardPriceItems.size()); j++) {
                        PriceItemEntry priceItem = cardPriceItems.get(j);
                        if (priceItem != null && !priceItem.isEmpty()) {
                            ItemStack priceStack = priceItem.getItemStack();
                            
                            // Small item icon
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                            guiGraphics.renderItem(priceStack, (int)(priceX / 0.75f), (int)(priceY / 0.75f));
                            guiGraphics.pose().popPose();
                            
                            String amountText = "x" + priceItem.getAmount() + (priceItem.isUseStacks() ? "s" : "");
                            guiGraphics.drawString(this.font, 
                                Component.literal(amountText),
                                priceX + 14, priceY + 2, 0xCCCCCC, true);
                            
                            priceY += 14;
                        }
                    }
                }
                
                // No price info at all
                if (listing.getMoneyPrice() <= 0 && (cardPriceItems == null || cardPriceItems.isEmpty())) {
                    guiGraphics.drawString(this.font, 
                        Component.literal("No price"),
                        priceX, priceY, 0xFF5555, true);
                }
                
                // Offer type badge (right of price, above buttons)
                String offerTypeText = listing.getOfferType() == MineBayListing.OfferType.FIXED ? 
                    "FIXED" : "NEGOTIABLE";
                int badgeColor = listing.getOfferType() == MineBayListing.OfferType.FIXED ? 
                    0xFF2D6B2D : 0xFF6B4F00;
                int badgeTextColor = listing.getOfferType() == MineBayListing.OfferType.FIXED ? 
                    0xFF88FF88 : 0xFFFFCC66;
                int badgeW = this.font.width(offerTypeText) + 8;
                int badgeX = centerX + this.imageWidth - 140 - badgeW;
                guiGraphics.fill(badgeX, yPos + 4, badgeX + badgeW, yPos + 16, badgeColor);
                guiGraphics.drawString(this.font, 
                    Component.literal(offerTypeText),
                    badgeX + 4, yPos + 5, badgeTextColor, true);
            }
            
            // Page indicator (always visible, below cards)
            int currentPage = (scrollOffset / LISTINGS_PER_PAGE) + 1;
            int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / LISTINGS_PER_PAGE));
            String pageStr = "Page " + currentPage + " / " + totalPages;
            int pageW = this.font.width(pageStr);
            guiGraphics.drawString(this.font, 
                Component.literal(pageStr),
                centerX + (this.imageWidth - pageW) / 2, centerY + 285, 0x888888, true);
        }
    }
    
    private void renderCreateStep1(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Step 1: Place Item to Sell
        guiGraphics.drawString(this.font, 
            Component.literal("Step 1: Place Item to Sell"),
            centerX + 15, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + 15, centerY + 61, centerX + 215, centerY + 62, 0x60FFD700);
        
        // Instructions
        guiGraphics.drawString(this.font, 
            Component.literal("Place or drag an item into the slot below:"),
            centerX + 30, centerY + 68, 0xFFFFFF, true);
        
        // Offering slot rendered by menu system - use actual slot position
        int slotX = centerX + this.menu.getOfferingSlotX();
        int slotY = centerY + this.menu.getOfferingSlotY();
        
        ItemStack offeringItem = this.menu.getOfferingItem();
        
        if (offeringItem.isEmpty()) {
            // Empty slot - draw animated highlight
            int alpha = (int)((Math.sin(System.currentTimeMillis() / 300.0) + 1) * 127) + 128;
            int color = (alpha << 24) | 0xFFD700;
            guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, color);
            
            Component placeHint = Component.literal(">> Place item here");
            int hintW = this.font.width(placeHint);
            guiGraphics.drawString(this.font, placeHint,
                slotX + 9 - hintW / 2, slotY + 25, 0xFFAA00, true);
        } else {
            // Item placed - show success
            guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0xFF55FF55);
            
            String itemNameStr = offeringItem.getHoverName().getString();
            Component itemText = Component.literal(itemNameStr);
            int nameW = this.font.width(itemText);
            guiGraphics.drawString(this.font, itemText,
                slotX + 9 - nameW / 2, slotY + 25, 0x55FF55, true);
        }
        
        // Help text
        guiGraphics.drawString(this.font, 
            Component.literal("* Shift+Click to quick-move items"),
            centerX + 30, centerY + 115, 0x888888, true);
    }
    
    private void renderCreateStep2(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Step 2: Set Prices (item already placed)
        int formX = centerX + 15;
        int formY = centerY + 76; // Must match initCreateStep2 and mouseClicked
        
        // Section header with underline
        guiGraphics.drawString(this.font, 
            Component.literal(isEditMode ? "Edit Listing - Set Prices" : "Step 2: Set Prices"),
            formX, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(formX, centerY + 61, formX + 200, centerY + 62, 0x60FFD700);
        
        // Money price label
        guiGraphics.drawString(this.font, 
            Component.literal("Price ($):"),
            formX, formY - 12, 0xFFFFFF, true);
        
        // Margin % label (positioned relative to imageWidth)
        int availWidth = this.imageWidth - 30;
        int priceW = (int)(availWidth * 0.40);
        int marginX = formX + priceW + 10;
        guiGraphics.drawString(this.font, 
            Component.literal("Margin %:"),
            marginX, formY - 12, 0xFFFFFF, true);
        
        // Offer type label
        int marginW = (int)(availWidth * 0.20);
        int typeX = marginX + marginW + 10;
        guiGraphics.drawString(this.font, 
            Component.literal("Type:"),
            typeX, formY - 12, 0xFFFFFF, true);
        
        // Show market pricing info (item is always placed at this step)
        if (!placedItem.isEmpty()) {
            double basePrice = com.servermanagement.client.ClientMarketData.getStackPrice(placedItem);
            double margin = 0.0;
            if (marginPercentBox != null && !marginPercentBox.getValue().isEmpty()) {
                try { margin = Double.parseDouble(marginPercentBox.getValue()); } catch (NumberFormatException e) {}
            }
            double finalPrice = com.servermanagement.client.ClientMarketData.calculateFinalPrice(basePrice, margin);
            
            // Market base price (left side)
            guiGraphics.drawString(this.font,
                Component.literal("Market Base: $" + String.format(Locale.US, "%.2f", basePrice)),
                formX, formY + 22, 0x55FFFF, true);
            
            // Final price preview (right-aligned)
            String finalStr = "Final Price: $" + String.format(Locale.US, "%.2f", finalPrice);
            int finalColor = margin >= 0 ? 0x55FF55 : 0xFFAA00;
            int finalW = this.font.width(finalStr);
            guiGraphics.drawString(this.font,
                Component.literal(finalStr),
                centerX + this.imageWidth - 15 - finalW, formY + 22, finalColor, true);
            
            // Listing preview (item icon + truncated name below market info)
            guiGraphics.drawString(this.font,
                Component.literal("Listing: "),
                formX, formY + 34, 0x999999, true);
            guiGraphics.renderItem(placedItem, formX + 50, formY + 30);
            String listingLabel = placedItem.getCount() > 1 
                ? placedItem.getCount() + "x " + placedItem.getHoverName().getString()
                : placedItem.getHoverName().getString();
            // Truncate to fit available space
            int maxNameWidth = this.imageWidth - 100;
            if (this.font.width(listingLabel) > maxNameWidth) {
                while (this.font.width(listingLabel + "...") > maxNameWidth && listingLabel.length() > 3) {
                    listingLabel = listingLabel.substring(0, listingLabel.length() - 1);
                }
                listingLabel += "...";
            }
            guiGraphics.drawString(this.font,
                Component.literal(listingLabel),
                formX + 70, formY + 34, 0xFFFFFF, true);
        }
        
        // Price Items section header (below market info with proper spacing)
        int priceItemY = formY + 65; // Must match initCreateStep2 and mouseClicked
        guiGraphics.drawString(this.font, 
            Component.literal("Price Items (buyer provides):"),
            formX, priceItemY - 13, 0xFFAA00, true);
        
        // Render 3 price item slots
        for (int i = 0; i < 3; i++) {
            int slotY = priceItemY + (i * 28);
            
            // Item slot visual (18x18)
            int slotX = formX;
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFFFFFFFF);
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
            
            // Render placed item if any
            if (!priceItems[i].isEmpty()) {
                ItemStack itemStack = priceItems[i].getItemStack();
                guiGraphics.renderItem(itemStack, slotX, slotY);
                guiGraphics.renderItemDecorations(this.font, itemStack, slotX, slotY);
                
                String itemName = itemStack.getHoverName().getString();
                if (itemName.length() > 20) {
                    itemName = itemName.substring(0, 18) + "...";
                }
                guiGraphics.drawString(this.font, 
                    Component.literal(itemName),
                    slotX + 145, slotY + 5, 0xFFFFFF, true);
            } else {
                guiGraphics.drawString(this.font, 
                    Component.literal("\u2190 Click to pick"),
                    slotX + 145, slotY + 5, 0x888888, true);
            }
        }
        
        // Help text at bottom
        guiGraphics.drawString(this.font, 
            Component.literal("* Click slots to pick items"),
            formX, priceItemY + 78, 0x888888, true);
    }
    
    private void renderCreateStep3(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Section header with underline
        Component step3Title = Component.literal(isEditMode ? "Confirm Changes" : "Confirm Listing");
        int s3tw = this.font.width(step3Title);
        guiGraphics.drawString(this.font, step3Title,
            centerX + (this.imageWidth - s3tw) / 2, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + (this.imageWidth - s3tw) / 2, centerY + 61, 
            centerX + (this.imageWidth + s3tw) / 2, centerY + 62, 0x60FFD700);
        
        // Card background
        guiGraphics.fill(centerX + 30, centerY + 68, centerX + this.imageWidth - 30, centerY + 180, 0xFF333333);
        guiGraphics.fill(centerX + 31, centerY + 69, centerX + this.imageWidth - 31, centerY + 179, 0xFF1E1E1E);
        
        int infoX = centerX + 50;
        int infoY = centerY + 76;
        
        // Item preview - icon + name (with count)
        if (!placedItem.isEmpty()) {
            guiGraphics.fill(infoX - 1, infoY - 1, infoX + 17, infoY + 17, 0xFF555555);
            guiGraphics.fill(infoX, infoY, infoX + 16, infoY + 16, 0xFF8B8B8B);
            guiGraphics.renderItem(placedItem, infoX, infoY);
            guiGraphics.renderItemDecorations(this.font, placedItem, infoX, infoY);
            String confirmItemLabel = placedItem.getCount() > 1 
                ? placedItem.getCount() + "x " + placedItem.getHoverName().getString()
                : placedItem.getHoverName().getString();
            guiGraphics.drawString(this.font, Component.literal(confirmItemLabel),
                infoX + 22, infoY + 4, 0xFFFFFF, true);
        }
        
        // Separator
        infoY += 24;
        guiGraphics.fill(infoX, infoY, centerX + this.imageWidth - 50, infoY + 1, 0xFF444444);
        infoY += 6;
        
        // Price
        String moneyValue = moneyPriceBox != null && !moneyPriceBox.getValue().isEmpty() ? 
            moneyPriceBox.getValue() : "0";
        guiGraphics.drawString(this.font, 
            Component.literal("Price: $" + moneyValue),
            infoX, infoY, 0x55FF55, true);
        
        // Offer type badge
        String offerType = selectedOfferType == MineBayListing.OfferType.FIXED ? "FIXED" : "NEGOTIABLE";
        int badgeColor = selectedOfferType == MineBayListing.OfferType.FIXED ? 0xFF2D6B2D : 0xFF6B4F00;
        int badgeTextColor = selectedOfferType == MineBayListing.OfferType.FIXED ? 0xFF88FF88 : 0xFFFFCC66;
        int bw = this.font.width(offerType) + 8;
        int badgeX = infoX + 120;
        guiGraphics.fill(badgeX, infoY - 1, badgeX + bw, infoY + 11, badgeColor);
        guiGraphics.drawString(this.font, Component.literal(offerType), badgeX + 4, infoY, badgeTextColor, true);
        
        infoY += 16;
        
        // Price items
        int priceItemCount = 0;
        for (PriceItemEntry entry : priceItems) {
            if (entry != null && !entry.isEmpty()) priceItemCount++;
        }
        
        if (priceItemCount > 0) {
            guiGraphics.drawString(this.font, 
                Component.literal("Required Items:"),
                infoX, infoY, 0x55FFFF, true);
            infoY += 12;
            
            for (int i = 0; i < priceItems.length; i++) {
                if (priceItems[i] != null && !priceItems[i].isEmpty()) {
                    ItemStack pStack = priceItems[i].getItemStack();
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                    guiGraphics.renderItem(pStack, (int)(infoX / 0.75f), (int)(infoY / 0.75f));
                    guiGraphics.pose().popPose();
                    guiGraphics.drawString(this.font, 
                        Component.literal(priceItems[i].getDisplayString()),
                        infoX + 14, infoY + 2, 0xAAFFFF, true);
                    infoY += 14;
                }
            }
        }
        
        // Warning at bottom of card
        guiGraphics.drawString(this.font, 
            Component.literal("⚠ Item cannot be retrieved until sold or cancelled."),
            infoX, centerY + 166, 0xFF5555, true);
    }
    
    private void renderMakeOffer(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (selectedListingForOffer == null) return;
        
        int formX = centerX + 15;
        int formY = centerY + 50;
        
        // Section header with underline
        Component offerTitle = Component.literal("Make an Offer");
        int otw = this.font.width(offerTitle);
        guiGraphics.drawString(this.font, offerTitle,
            formX, formY, 0xFFD700, true);
        guiGraphics.fill(formX, formY + 11, formX + otw, formY + 12, 0x60FFD700);
        
        // Item being offered on
        String itemName = selectedListingForOffer.getItemForSale().getHoverName().getString();
        int maxItemW = this.imageWidth - 120;
        if (this.font.width(itemName) > maxItemW) {
            while (this.font.width(itemName + "...") > maxItemW && itemName.length() > 3) {
                itemName = itemName.substring(0, itemName.length() - 1);
            }
            itemName += "...";
        }
        guiGraphics.drawString(this.font, 
            Component.literal("Item: " + itemName),
            formX, formY + 16, 0xCCCCCC, true);
        
        // Asking price and market value
        String askingStr = "Asking: $" + String.format(Locale.US, "%.2f", selectedListingForOffer.getMoneyPrice());
        guiGraphics.drawString(this.font, Component.literal(askingStr),
            formX, formY + 28, 0x999999, true);
        
        double marketPrice = com.servermanagement.client.ClientMarketData.getStackPrice(
            selectedListingForOffer.getItemForSale());
        if (marketPrice > 0) {
            String marketStr = "Market Value: $" + String.format(Locale.US, "%.2f", marketPrice);
            guiGraphics.drawString(this.font, Component.literal(marketStr),
                formX + this.font.width(askingStr) + 15, formY + 28, 0x55AAFF, true);
        }
        
        // Money offer label
        guiGraphics.drawString(this.font, 
            Component.literal("Your Money Offer:"),
            formX, formY + 43, 0xFFFFFF, true);
        
        // Items to Offer label
        guiGraphics.drawString(this.font, 
            Component.literal("Items to Offer (optional):"),
            formX, formY + 82, 0xFFFFFF, true);
        
        // Draw slot backgrounds for the 3 offer slots (container-backed)
        for (int i = 0; i < 3; i++) {
            int slotX = centerX + this.menu.getOfferSlotX(i);
            int slotY = centerY + this.menu.getOfferSlotY(i);
            
            // Slot border and background
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF373737);
        }
        
        // Calculate total offer value
        double totalOfferValue = offerMoney;
        java.util.List<ItemStack> offerItemsList = this.menu.getOfferItems();
        for (ItemStack stack : offerItemsList) {
            totalOfferValue += com.servermanagement.client.ClientMarketData.getStackPrice(stack);
        }
        
        // Total offer value display (below the offer slots)
        int valueY = formY + 120;
        String totalStr = String.format("Total Offer Value: $%.2f", totalOfferValue);
        int totalColor = totalOfferValue >= selectedListingForOffer.getMoneyPrice() ? 0x55FF55 : 0xFFAA00;
        guiGraphics.drawString(this.font, Component.literal(totalStr),
            formX, valueY, totalColor, true);
        
        // Help text
        guiGraphics.drawString(this.font, 
            Component.literal("Place items from your inventory into the slots above"),
            formX, valueY + 14, 0x888888, true);
    }
    
    private void renderViewDetails(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (selectedListingForDetails == null) return;
        
        MineBayListing listing = selectedListingForDetails;
        
        // Section header with underline (inside scissor area)
        Component detailsTitle = Component.literal("Listing Details");
        int dtw = this.font.width(detailsTitle);
        guiGraphics.drawString(this.font, detailsTitle,
            centerX + (this.imageWidth - dtw) / 2, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + (this.imageWidth - dtw) / 2, centerY + 61, 
            centerX + (this.imageWidth + dtw) / 2, centerY + 62, 0x60FFD700);
        
        // Details card background
        guiGraphics.fill(centerX + 30, centerY + 68, centerX + this.imageWidth - 30, centerY + 210, 0xFF333333);
        guiGraphics.fill(centerX + 31, centerY + 69, centerX + this.imageWidth - 31, centerY + 209, 0xFF1E1E1E);
        
        // Item display
        ItemStack itemForSale = listing.getItemForSale();
        int itemX = centerX + 50;
        int itemY = centerY + 80;
        
        // Item slot background
        guiGraphics.fill(itemX - 2, itemY - 2, itemX + 20, itemY + 20, 0xFF555555);
        guiGraphics.fill(itemX - 1, itemY - 1, itemX + 19, itemY + 19, 0xFF8B8B8B);
        guiGraphics.renderItem(itemForSale, itemX, itemY);
        guiGraphics.renderItemDecorations(this.font, itemForSale, itemX, itemY);
        
        // Item name
        guiGraphics.drawString(this.font, 
            itemForSale.getHoverName(),
            itemX + 28, itemY + 4, 0xFFFFFF, true);
        
        // Seller info
        int infoY = itemY + 30;
        guiGraphics.drawString(this.font, 
            Component.literal("Seller:"),
            itemX, infoY, 0xAAAAAA, true);
        guiGraphics.drawString(this.font, 
            Component.literal(listing.getSellerName()),
            itemX + 50, infoY, 0xFFAA00, true);
        
        // Offer type
        infoY += 15;
        String typeText = listing.getOfferType() == MineBayListing.OfferType.FIXED ? "Fixed Price" : "Negotiable";
        int typeColor = listing.getOfferType() == MineBayListing.OfferType.FIXED ? 0x55FF55 : 0xFFAA00;
        guiGraphics.drawString(this.font, 
            Component.literal("Type:"),
            itemX, infoY, 0xAAAAAA, true);
        guiGraphics.drawString(this.font, 
            Component.literal(typeText),
            itemX + 50, infoY, typeColor, true);
        
        // Price section header
        infoY += 20;
        guiGraphics.drawString(this.font, 
            Component.literal("— Price —"),
            itemX, infoY, 0xFFD700, true);
        
        // Money price
        infoY += 15;
        if (listing.getMoneyPrice() > 0) {
            guiGraphics.drawString(this.font, 
                Component.literal("Total: $" + String.format(Locale.US, "%.2f", listing.getMoneyPrice())),
                itemX, infoY, 0x55FF55, true);
            infoY += 15;
            
            // Show market pricing breakdown if available
            if (listing.getBaseMarketPrice() > 0) {
                guiGraphics.drawString(this.font,
                    Component.literal("Market Base: $" + String.format(Locale.US, "%.2f", listing.getBaseMarketPrice())),
                    itemX, infoY, 0x55FFFF, true);
                infoY += 12;
                String marginStr = listing.getMarginPercent() >= 0 
                    ? "+" + String.format(Locale.US, "%.0f", listing.getMarginPercent()) + "%" 
                    : String.format(Locale.US, "%.0f", listing.getMarginPercent()) + "%";
                int marginColor = listing.getMarginPercent() >= 0 ? 0x55FF55 : 0xFFAA00;
                guiGraphics.drawString(this.font,
                    Component.literal("Seller Margin: " + marginStr),
                    itemX, infoY, marginColor, true);
                infoY += 15;
            }
        }
        
        // Required items
        List<PriceItemEntry> priceItems = listing.getPriceItems();
        if (priceItems != null && !priceItems.isEmpty()) {
            guiGraphics.drawString(this.font, 
                Component.literal("Required Items:"),
                itemX, infoY, 0xAAAAAA, true);
            infoY += 14;
            
            for (PriceItemEntry priceItem : priceItems) {
                if (priceItem != null && !priceItem.isEmpty()) {
                    // Render small item icon
                    ItemStack priceStack = priceItem.getItemStack();
                    guiGraphics.renderItem(priceStack, itemX + 5, infoY - 2);
                    
                    String amountText = priceItem.getAmount() + 
                        (priceItem.isUseStacks() ? " stacks of " : "x ") + 
                        priceStack.getHoverName().getString();
                    guiGraphics.drawString(this.font, 
                        Component.literal(amountText),
                        itemX + 25, infoY + 2, 0xFFFFFF, true);
                    infoY += 20;
                }
            }
        }
        
        // No price set
        if (listing.getMoneyPrice() <= 0 && (priceItems == null || priceItems.isEmpty())) {
            guiGraphics.drawString(this.font, 
                Component.literal("No price set"),
                itemX, infoY, 0xFF5555, true);
        }
    }
    
    private void renderMyListings(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Handled by browse screen with filter - should not render here
    }
    
    private void renderDeleteConfirm(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (listingToDelete == null) {
            return;
        }
        
        // Create a centered confirmation dialog
        int dialogWidth = 400;
        int dialogHeight = 200;
        int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
        int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
        
        // Draw semi-transparent overlay behind dialog
        guiGraphics.fill(centerX, centerY, 
            centerX + this.imageWidth, centerY + this.imageHeight, 
            0x80000000); // Semi-transparent black
        
        // Draw dialog background
        guiGraphics.fill(dialogX, dialogY, 
            dialogX + dialogWidth, dialogY + dialogHeight, 
            0xFF2B2B2B); // Dark gray
        
        // Draw dialog border
        guiGraphics.fill(dialogX - 2, dialogY - 2, 
            dialogX + dialogWidth + 2, dialogY + 2, 
            0xFFFF5555); // Red border top
        guiGraphics.fill(dialogX - 2, dialogY + dialogHeight - 2, 
            dialogX + dialogWidth + 2, dialogY + dialogHeight + 2, 
            0xFFFF5555); // Red border bottom
        guiGraphics.fill(dialogX - 2, dialogY, 
            dialogX + 2, dialogY + dialogHeight, 
            0xFFFF5555); // Red border left
        guiGraphics.fill(dialogX + dialogWidth - 2, dialogY, 
            dialogX + dialogWidth + 2, dialogY + dialogHeight, 
            0xFFFF5555); // Red border right
        
        // Draw title
        Component titleText = Component.literal("⚠ Delete Listing?");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText,
            dialogX + (dialogWidth - titleWidth) / 2,
            dialogY + 20,
            0xFFFF5555, true);
        
        // Draw warning message
        Component warningText = Component.literal("Are you sure you want to delete this listing?");
        int warningWidth = this.font.width(warningText);
        guiGraphics.drawString(this.font, warningText,
            dialogX + (dialogWidth - warningWidth) / 2,
            dialogY + 50,
            0xFFFFFFFF, true);
        
        Component warningText2 = Component.literal("This action cannot be undone.");
        int warningWidth2 = this.font.width(warningText2);
        guiGraphics.drawString(this.font, warningText2,
            dialogX + (dialogWidth - warningWidth2) / 2,
            dialogY + 65,
            0xFFAAAAAA, true);
        
        // Draw item info
        ItemStack itemForSale = listingToDelete.getItemForSale();
        if (!itemForSale.isEmpty()) {
            // Render item icon centered
            int iconX = dialogX + dialogWidth / 2 - 8;
            guiGraphics.renderItem(itemForSale, iconX, dialogY + 85);
            
            // Item name centered below icon
            Component itemName = itemForSale.getHoverName();
            int nameW = this.font.width(itemName);
            guiGraphics.drawString(this.font, itemName,
                dialogX + (dialogWidth - nameW) / 2,
                dialogY + 108,
                0xFFFFFF55, true);
        }
    }
    
    private void renderBuyConfirm(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (listingToBuy == null) return;
        
        boolean hasMoneyPrice = listingToBuy.getMoneyPrice() > 0;
        int dialogWidth = 400;
        int dialogHeight = (buyPaymentMode == PaymentMode.ITEMS) ? 310 : 220;
        int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
        int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
        
        // Semi-transparent overlay
        guiGraphics.fill(centerX, centerY, 
            centerX + this.imageWidth, centerY + this.imageHeight, 
            0x80000000);
        
        // Dialog background
        guiGraphics.fill(dialogX, dialogY, 
            dialogX + dialogWidth, dialogY + dialogHeight, 
            0xFF2B2B2B);
        
        // Green border
        guiGraphics.fill(dialogX - 2, dialogY - 2, dialogX + dialogWidth + 2, dialogY + 2, 0xFF55FF55);
        guiGraphics.fill(dialogX - 2, dialogY + dialogHeight - 2, dialogX + dialogWidth + 2, dialogY + dialogHeight + 2, 0xFF55FF55);
        guiGraphics.fill(dialogX - 2, dialogY, dialogX + 2, dialogY + dialogHeight, 0xFF55FF55);
        guiGraphics.fill(dialogX + dialogWidth - 2, dialogY, dialogX + dialogWidth + 2, dialogY + dialogHeight, 0xFF55FF55);
        
        // Title
        Component title = Component.literal("Confirm Purchase");
        int titleW = this.font.width(title);
        guiGraphics.drawString(this.font, title, dialogX + (dialogWidth - titleW) / 2, dialogY + 12, 0x55FF55, true);
        
        // Item info - centered icon with name below
        ItemStack itemForSale = listingToBuy.getItemForSale();
        if (!itemForSale.isEmpty()) {
            int iconX = dialogX + dialogWidth / 2 - 8;
            guiGraphics.renderItem(itemForSale, iconX, dialogY + 30);
            guiGraphics.renderItemDecorations(this.font, itemForSale, iconX, dialogY + 30);
            
            Component itemName = itemForSale.getHoverName();
            int nameW = this.font.width(itemName);
            guiGraphics.drawString(this.font, itemName, 
                dialogX + (dialogWidth - nameW) / 2, dialogY + 50, 0xFFFFFF, true);
        }
        
        // Price info centered
        if (hasMoneyPrice) {
            String priceText = "Price: $" + String.format(Locale.US, "%.2f", listingToBuy.getMoneyPrice());
            Component priceComp = Component.literal(priceText);
            int priceW = this.font.width(priceComp);
            guiGraphics.drawString(this.font, priceComp, dialogX + (dialogWidth - priceW) / 2, dialogY + 65, 0x55FF55, true);
        }
        
        // Required items (always shown regardless of payment mode)
        List<PriceItemEntry> reqItems = listingToBuy.getPriceItems();
        int contentY = dialogY + (hasMoneyPrice ? 80 : 65);
        if (reqItems != null && !reqItems.isEmpty()) {
            Component reqLabel = Component.literal("Required items:");
            int reqLabelW = this.font.width(reqLabel);
            guiGraphics.drawString(this.font, reqLabel, 
                dialogX + (dialogWidth - reqLabelW) / 2, contentY, 0xAAAAAA, true);
            contentY += 12;
            for (PriceItemEntry entry : reqItems) {
                if (entry != null && !entry.isEmpty()) {
                    String text = entry.getAmount() + (entry.isUseStacks() ? " stacks of " : "x ") + 
                        entry.getItemStack().getHoverName().getString();
                    Component reqComp = Component.literal(text);
                    int reqW = this.font.width(reqComp);
                    guiGraphics.drawString(this.font, reqComp, 
                        dialogX + (dialogWidth - reqW) / 2, contentY, 0xFFFFFF, true);
                    contentY += 12;
                }
            }
        }
        
        if (buyPaymentMode == PaymentMode.NONE && hasMoneyPrice) {
            // Show payment method selection prompt
            Component chooseText = Component.literal("Choose payment method:");
            int chooseW = this.font.width(chooseText);
            guiGraphics.drawString(this.font, chooseText, 
                dialogX + (dialogWidth - chooseW) / 2, dialogY + 95, 0xFFD700, true);
            
            // Show balance info below button
            double balance = com.servermanagement.client.ClientBankData.getBalance();
            Component balInfo = Component.literal("Balance: $" + String.format(Locale.US, "%.2f", balance));
            int balW = this.font.width(balInfo);
            guiGraphics.drawString(this.font, balInfo, 
                dialogX + (dialogWidth - balW) / 2, dialogY + 140, 0xAAAAAA, true);
        } else if (buyPaymentMode == PaymentMode.BALANCE) {
            // Balance payment confirmation
            double balance = com.servermanagement.client.ClientBankData.getBalance();
            Component payText = Component.literal("Paying $" + String.format(Locale.US, "%.2f", listingToBuy.getMoneyPrice()) + " from bank balance");
            int payW = this.font.width(payText);
            guiGraphics.drawString(this.font, payText, dialogX + (dialogWidth - payW) / 2, dialogY + 95, 0x55FFFF, true);
            
            Component balText = Component.literal("Remaining balance: $" + String.format(Locale.US, "%.2f", balance - listingToBuy.getMoneyPrice()));
            int balW = this.font.width(balText);
            guiGraphics.drawString(this.font, balText, dialogX + (dialogWidth - balW) / 2, dialogY + 110, 0xAAAAAA, true);
        } else if (buyPaymentMode == PaymentMode.ITEMS) {
            // Item payment - render inventory grid for selection
            renderItemPaymentGrid(guiGraphics, dialogX, dialogY, dialogWidth, dialogHeight);
        } else if (!hasMoneyPrice) {
            // No money price - just item requirements 
            Component freeText = Component.literal("No money cost - only required items above");
            int freeW = this.font.width(freeText);
            guiGraphics.drawString(this.font, freeText, dialogX + (dialogWidth - freeW) / 2, contentY + 5, 0xAAAAAA, true);
        }
    }
    
    private void renderItemPaymentGrid(GuiGraphics guiGraphics, int dialogX, int dialogY, int dialogWidth, int dialogHeight) {
        if (minecraft == null || minecraft.player == null) return;
        
        Component selectText = Component.literal("Click items to select for payment:");
        int selectW = this.font.width(selectText);
        guiGraphics.drawString(this.font, selectText, dialogX + (dialogWidth - selectW) / 2, dialogY + 82, 0xFFD700, true);
        
        // Render 9x4 grid of main inventory (slots 0-35)
        int gridCols = 9;
        int gridRows = 4;
        int slotSize = 18;
        int gridWidth = gridCols * slotSize;
        int gridStartX = dialogX + (dialogWidth - gridWidth) / 2;
        int gridStartY = dialogY + 96;
        
        net.minecraft.world.entity.player.Inventory inv = minecraft.player.getInventory();
        
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                // Hotbar (0-8) at bottom row, main inv (9-35) at top 3 rows
                int slotIdx;
                if (row < 3) {
                    slotIdx = 9 + (row * 9) + col; // Main inventory rows
                } else {
                    slotIdx = col; // Hotbar
                }
                
                int slotX = gridStartX + col * slotSize;
                int slotY = gridStartY + row * slotSize;
                
                // Slot background
                boolean isSelected = selectedPaymentSlots.contains(slotIdx);
                guiGraphics.fill(slotX, slotY, slotX + slotSize - 1, slotY + slotSize - 1, 
                    isSelected ? 0xFF335533 : 0xFF1A1A2E);
                
                // Selected highlight border
                if (isSelected) {
                    guiGraphics.fill(slotX - 1, slotY - 1, slotX + slotSize, slotY, 0xFF55FF55);
                    guiGraphics.fill(slotX - 1, slotY + slotSize - 1, slotX + slotSize, slotY + slotSize, 0xFF55FF55);
                    guiGraphics.fill(slotX - 1, slotY, slotX, slotY + slotSize - 1, 0xFF55FF55);
                    guiGraphics.fill(slotX + slotSize - 1, slotY, slotX + slotSize, slotY + slotSize - 1, 0xFF55FF55);
                }
                
                if (slotIdx < inv.items.size()) {
                    ItemStack stack = inv.items.get(slotIdx);
                    if (!stack.isEmpty()) {
                        guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                        guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
                    }
                }
            }
        }
        
        // Separator line between inventory and hotbar
        int sepY = gridStartY + 3 * slotSize;
        guiGraphics.fill(gridStartX, sepY, gridStartX + gridWidth, sepY + 1, 0xFF555555);
        
        // Summary line
        double selectedTotal = getSelectedItemsTotal();
        double price = listingToBuy.getMoneyPrice();
        int summaryY = gridStartY + gridRows * slotSize + 5;
        
        String totalText = "Selected: $" + String.format(Locale.US, "%.2f", selectedTotal) + " / $" + String.format(Locale.US, "%.2f", price);
        Component totalComp = Component.literal(totalText);
        int totalW = this.font.width(totalComp);
        int totalColor = selectedTotal >= price ? 0x55FF55 : 0xFFAA00;
        guiGraphics.drawString(this.font, totalComp, dialogX + (dialogWidth - totalW) / 2, summaryY, totalColor, true);
        
        // Refund notice
        if (selectedTotal > price) {
            double refund = selectedTotal - price;
            String overText = "Refund of $" + String.format(Locale.US, "%.2f", refund) + " added to balance";
            Component overComp = Component.literal(overText);
            int overW = this.font.width(overComp);
            guiGraphics.drawString(this.font, overComp, dialogX + (dialogWidth - overW) / 2, summaryY + 12, 0xAAAAAA, true);
        }
    }
    
    private void showStatusMessage(String message, int color) {
        this.statusMessage = message;
        this.statusMessageColor = color;
        this.statusMessageTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Handle item payment grid clicks in BUY_CONFIRM with ITEMS mode
        if (currentState == ScreenState.BUY_CONFIRM && buyPaymentMode == PaymentMode.ITEMS && listingToBuy != null) {
            int dialogWidth = 400;
            int dialogHeight = 310;
            int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
            int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
            
            int gridCols = 9;
            int slotSize = 18;
            int gridWidth = gridCols * slotSize;
            int gridStartX = dialogX + (dialogWidth - gridWidth) / 2;
            int gridStartY = dialogY + 96;
            
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = gridStartX + col * slotSize;
                    int slotY = gridStartY + row * slotSize;
                    
                    if (mouseX >= slotX && mouseX < slotX + slotSize &&
                        mouseY >= slotY && mouseY < slotY + slotSize) {
                        int slotIdx = row < 3 ? 9 + (row * 9) + col : col;
                        
                        // Only toggle if slot has an item with value
                        if (minecraft != null && minecraft.player != null && 
                            slotIdx < minecraft.player.getInventory().items.size()) {
                            ItemStack stack = minecraft.player.getInventory().items.get(slotIdx);
                            if (!stack.isEmpty() && com.servermanagement.client.ClientMarketData.getStackPrice(stack) > 0) {
                                if (selectedPaymentSlots.contains(slotIdx)) {
                                    selectedPaymentSlots.remove(slotIdx);
                                } else {
                                    selectedPaymentSlots.add(slotIdx);
                                }
                                this.rebuildWidgets();
                            }
                        }
                        return true;
                    }
                }
            }
        }
        
        // Handle price item slot clicks in CREATE_STEP2
        if (currentState == ScreenState.CREATE_STEP2) {
            int formX = centerX + 15;
            int formY = centerY + 76;
            int priceItemY = formY + 65;
            
            for (int i = 0; i < 3; i++) {
                int slotX = formX;
                int slotY = priceItemY + (i * 28);
                
                if (mouseX >= slotX && mouseX < slotX + 18 &&
                    mouseY >= slotY && mouseY < slotY + 18) {
                    handlePriceItemSlotClick(i);
                    return true;
                }
            }
        }
        
        // MAKE_OFFER slots are now handled by the container system (real slots)
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handle clicking on a price item slot - opens item picker
     */
    private void handlePriceItemSlotClick(int slotIndex) {
        if (this.minecraft != null) {
            // Create item picker screen
            ItemPickerScreen picker = new ItemPickerScreen(this, selectedItem -> {
                // Callback when item is selected
                ItemStack itemCopy = selectedItem.copy();
                itemCopy.setCount(1); // Use single item for display
                
                // Get amount from text box
                int amount = 1;
                if (priceAmountBoxes[slotIndex] != null && !priceAmountBoxes[slotIndex].getValue().isEmpty()) {
                    try {
                        amount = Integer.parseInt(priceAmountBoxes[slotIndex].getValue());
                        amount = Math.max(1, Math.min(amount, 999)); // Clamp to reasonable range
                    } catch (NumberFormatException e) {
                        amount = 1;
                    }
                }
                
                priceItems[slotIndex] = new PriceItemEntry(itemCopy, amount, useStacks[slotIndex]);
            });
            
            // If slot already has an item, highlight it in picker
            if (priceItems[slotIndex] != null && !priceItems[slotIndex].isEmpty()) {
                picker.setHighlightItem(priceItems[slotIndex].getItemStack());
            }
            
            this.minecraft.setScreen(picker);
        }
    }
    
    /**
     * Update listings from server sync packet
     */
    public void updateListings(List<MineBayListing> newListings) {
        this.allListings = new ArrayList<>(newListings);
        
        // Re-apply filter if showing My Listings
        if (showingMyListings && this.minecraft != null && this.minecraft.player != null) {
            this.listings = new ArrayList<>();
            for (MineBayListing listing : allListings) {
                if (listing.getSellerId().equals(minecraft.player.getUUID())) {
                    this.listings.add(listing);
                }
            }
        } else {
            this.listings = new ArrayList<>(allListings);
        }
        
        this.scrollOffset = 0;
        
        // Rebuild widgets to show new listings
        if (currentState == ScreenState.BROWSE) {
            this.rebuildWidgets();
        }
    }
    
    /**
     * Called from SyncListingOffersPacket when server sends offers for a listing
     */
    public void receiveOffers(String listingId, List<com.servermanagement.features.minebay.MineBayOffer> offers) {
        if (selectedListingForOffers != null && selectedListingForOffers.getListingId().equals(listingId)) {
            this.cachedOffers = new ArrayList<>(offers);
            this.offersLoading = false;
            this.rebuildWidgets();
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check moneyPriceBox if it's visible and focused
        if (moneyPriceBox != null && moneyPriceBox.isFocused() && moneyPriceBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // Check priceAmountBoxes if they're visible and focused
        if (priceAmountBoxes != null) {
            for (EditBox box : priceAmountBoxes) {
                if (box != null && box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Check moneyPriceBox if it's visible and focused
        if (moneyPriceBox != null && moneyPriceBox.isFocused() && moneyPriceBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        
        // Check priceAmountBoxes if they're visible and focused
        if (priceAmountBoxes != null) {
            for (EditBox box : priceAmountBoxes) {
                if (box != null && box.isFocused() && box.charTyped(codePoint, modifiers)) {
                    return true;
                }
            }
        }
        
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentState == ScreenState.BROWSE && !listings.isEmpty()) {
            int maxScroll = Math.max(0, listings.size() - LISTINGS_PER_PAGE);
            if (scrollY > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (scrollY < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public void onClose() {
        // Don't clear item placement data - it persists
        super.onClose();
    }
}
