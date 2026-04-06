package com.servermanagement.gui.minebay;

import com.servermanagement.features.minebay.MineBayListing;
import com.servermanagement.features.minebay.MineBayManager;
import com.servermanagement.features.minebay.PriceItemEntry;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    
    // Status message (shown briefly after actions)
    private String statusMessage = null;
    private long statusMessageTime = 0;
    private int statusMessageColor = 0x55FF55;
    
    public enum ScreenState {
        BROWSE,              // Browse all listings (default, shows inventory only if creating)
        CREATE_STEP1,        // Enter price details + price items (shows inventory)
        CREATE_STEP2,        // Place seller item (shows inventory)
        CREATE_STEP3,        // Final confirmation (hides inventory)
        VIEW_DETAILS,        // View listing details and offers
        MAKE_OFFER,          // Make an offer on a negotiable listing
        VIEW_MY_LISTINGS,    // View your own listings and manage offers
        DELETE_CONFIRM,      // Confirmation dialog for deleting a listing
        BUY_CONFIRM          // Confirmation dialog for buying a listing
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
        }
    }
    
    /**
     * Update inventory visibility based on current screen state
     */
    private void updateInventoryVisibility() {
        // Show inventory on CREATE_STEP2 (placing item to sell) and MAKE_OFFER (offering items)
        boolean shouldShowInventory = (currentState == ScreenState.CREATE_STEP2 || currentState == ScreenState.MAKE_OFFER);
        this.menu.setInventoryVisible(shouldShowInventory);
        
        // Show offering slot only on CREATE_STEP2
        boolean shouldShowOffering = (currentState == ScreenState.CREATE_STEP2);
        this.menu.setOfferingSlotVisible(shouldShowOffering);
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
            // Create Listing button (centered in empty state)
            this.addRenderableWidget(new ModernButton(
                centerX + (this.imageWidth / 2) - 75, centerY + 150, 150, 30,
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
        // Layout starts below the title/underline (rendered at centerY+50/61)
        int formX = centerX + 15;
        int formY = centerY + 76; // Below title + underline + spacing
        
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
        
        // Money price input
        if (moneyPriceBox == null) {
            moneyPriceBox = new EditBox(this.font, formX, formY, 120, 18, Component.literal("Money Price"));
            moneyPriceBox.setMaxLength(10);
            // Pre-populate with existing price if in edit mode
            if (isEditMode && listingBeingEdited != null) {
                moneyPriceBox.setValue(String.valueOf((int) listingBeingEdited.getMoneyPrice()));
            } else {
                moneyPriceBox.setValue("0");
            }
            moneyPriceBox.setHint(Component.literal("$..."));
            moneyPriceBox.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        } else {
            moneyPriceBox.setPosition(formX, formY);
        }
        this.addRenderableWidget(moneyPriceBox);
        
        // Offer type selector
        this.addRenderableWidget(new ModernButton(
            formX + 130, formY - 2, 140, 22,
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
        int priceItemY = formY + 38;
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
                        // Reset amount box to default
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
        // Skip to STEP3 directly if editing (item already exists)
        String nextButtonText = isEditMode ? "Next: Confirm Changes \u2192" : "Next: Place Item \u2192";
        ScreenState nextState = isEditMode ? ScreenState.CREATE_STEP3 : ScreenState.CREATE_STEP2;
        int buttonWidth = isEditMode ? 200 : 180;
        
        this.addRenderableWidget(new ModernButton(
            formX, priceItemY + (3 * 28) + 10, buttonWidth, 25,
            Component.literal(nextButtonText),
            button -> switchState(nextState),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }
    
    private void initCreateStep2(int centerX, int centerY) {
        // Back button
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 5, 100, 20,
            Component.literal("← Back"),
            button -> switchState(ScreenState.CREATE_STEP1),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Action button positioned below inventory area
        // Changes text and color based on whether an item has been placed
        int buttonY = centerY + 350;
        ItemStack currentOffering = this.menu.getOfferingItem();
        boolean hasItem = !currentOffering.isEmpty();
        
        this.addRenderableWidget(new ModernButton(
            centerX + 220, buttonY, 160, 30,
            Component.literal(hasItem ? "Continue \u2192" : "Place Item"),
            button -> {
                ItemStack offeringItem = this.menu.getOfferingItem();
                if (!offeringItem.isEmpty()) {
                    this.placedItem = offeringItem.copy();
                    this.itemPlaced = true;
                    switchState(ScreenState.CREATE_STEP3);
                }
            },
            hasItem ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
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
                // Clear edit mode when canceling
                isEditMode = false;
                listingBeingEdited = null;
                cancelListing();
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
        if (oldState == ScreenState.CREATE_STEP1 || oldState == ScreenState.CREATE_STEP3) {
            if (newState == ScreenState.BROWSE || newState == ScreenState.DELETE_CONFIRM || 
                newState == ScreenState.BUY_CONFIRM) {
                moneyPriceBox = null;
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
        switchState(ScreenState.BUY_CONFIRM);
    }
    
    private void confirmBuy() {
        if (listingToBuy == null) {
            switchState(ScreenState.BROWSE);
            return;
        }
        
        // Send purchase packet to server
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.PurchaseListingPacket(listingToBuy.getListingId())
        );
        
        // Show status message and return to browse
        showStatusMessage("Purchase request sent!", 0x55FF55);
        
        // Remove from local cache optimistically
        allListings.remove(listingToBuy);
        listings.remove(listingToBuy);
        listingToBuy = null;
        
        switchState(ScreenState.BROWSE);
    }
    
    private void cancelBuy() {
        listingToBuy = null;
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
        
        // Go to CREATE_STEP1 to edit prices (money price will be set in initCreateStep1)
        switchState(ScreenState.CREATE_STEP1);
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
        int formY = centerY + 45;
        
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
        
        // Show what they're offering on
        int detailsY = formY - 20;
        
        // Money offer input
        EditBox moneyInput = new EditBox(this.font, formX, formY + 30, 200, 20, Component.literal("Money Offer"));
        moneyInput.setValue(String.format("%.2f", offerMoney));
        moneyInput.setResponder(value -> {
            try {
                offerMoney = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                offerMoney = 0.0;
            }
        });
        this.addRenderableWidget(moneyInput);
        
        // Item offer slots (3 slots like price items)
        for (int i = 0; i < 3; i++) {
            int slotX = formX + (i * 80);
            int slotY = formY + 80;
            
            final int index = i;
            this.addRenderableWidget(new ModernButton(
                slotX, slotY + 40, 70, 20,
                Component.literal("Clear"),
                button -> {
                    offerItems[index] = ItemStack.EMPTY;
                    this.rebuildWidgets();
                },
                ModernButton.ButtonStyle.SECONDARY
            ));
        }
        
        // Submit offer button
        this.addRenderableWidget(new ModernButton(
            formX, formY + 160, 150, 25,
            Component.literal("Submit Offer"),
            button -> submitOffer(),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        // Cancel button
        this.addRenderableWidget(new ModernButton(
            formX + 160, formY + 160, 100, 25,
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
        
        int buttonY = centerY + 185;
        
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
                centerX + 150, buttonY, 100, 25,
                Component.literal("Edit"),
                button -> editListing(listing),
                ModernButton.ButtonStyle.PRIMARY
            ));
            this.addRenderableWidget(new ModernButton(
                centerX + 260, buttonY, 100, 25,
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
        
        int dialogWidth = 400;
        int dialogHeight = 200;
        int dialogX = centerX + (this.imageWidth - dialogWidth) / 2;
        int dialogY = centerY + (this.imageHeight - dialogHeight) / 2;
        
        // Confirm button
        this.addRenderableWidget(new ModernButton(
            dialogX + 30, dialogY + dialogHeight - 50, 160, 30,
            Component.literal("✓ Confirm Purchase"),
            button -> confirmBuy(),
            ModernButton.ButtonStyle.SUCCESS
        ));
        
        // Cancel button
        this.addRenderableWidget(new ModernButton(
            dialogX + dialogWidth - 190, dialogY + dialogHeight - 50, 160, 30,
            Component.literal("✗ Cancel"),
            button -> cancelBuy(),
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void submitOffer() {
        if (selectedListingForOffer == null || minecraft == null || minecraft.player == null) {
            return;
        }
        
        // Collect non-empty offer items
        List<ItemStack> itemOffers = new ArrayList<>();
        for (ItemStack stack : offerItems) {
            if (!stack.isEmpty()) {
                itemOffers.add(stack.copy());
            }
        }
        
        // Validate offer
        if (offerMoney <= 0 && itemOffers.isEmpty()) {
            showStatusMessage("You must offer money or items!", 0xFF5555);
            return;
        }
        
        // Send offer packet
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.minebay.CreateOfferPacket(
                selectedListingForOffer.getListingId(),
                offerMoney,
                itemOffers
            )
        );
        
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
        int contentAreaHeight = menu.isInventoryVisible() ? 165 : 340;
        guiGraphics.fill(centerX + 10, centerY + 46, centerX + this.imageWidth - 10, 
            centerY + 46 + contentAreaHeight, 0xE01A1A1A);
        
        // Inventory area background (only when inventory is visible)
        if (menu.isInventoryVisible()) {
            guiGraphics.fill(centerX + 210, centerY + 220, centerX + this.imageWidth - 210, 
                centerY + this.imageHeight - 5, 0xE0202020);
            
            // Inventory border
            guiGraphics.fill(centerX + 208, centerY + 218, centerX + this.imageWidth - 208, centerY + 220, 0xFF555555);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only draw the inventory label when inventory is visible, centered above inventory area
        if (menu.isInventoryVisible()) {
            int labelX = (this.imageWidth - this.font.width(this.playerInventoryTitle)) / 2;
            guiGraphics.drawString(this.font, this.playerInventoryTitle, labelX, this.inventoryLabelY, 0xAAAAAA, true);
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
        int contentHeight = menu.isInventoryVisible() ? 165 : 340;
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
        
        // Render tooltips (outside scissor region)
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Dynamically check for item placement in CREATE_STEP2
        if (currentState == ScreenState.CREATE_STEP2) {
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
            int boxY = (int)(centerY + 95 + bounce);
            guiGraphics.fill(boxX, boxY, boxX + 24, boxY + 20, 0xFF555555);
            guiGraphics.fill(boxX + 1, boxY + 1, boxX + 23, boxY + 19, 0xFF333333);
            guiGraphics.fill(boxX + 1, boxY + 8, boxX + 23, boxY + 10, 0xFF555555);
            
            guiGraphics.drawString(this.font, 
                Component.literal("No Active Listings"),
                centerX + (this.imageWidth / 2) - 60, centerY + 130, 0xFFFFFF, true);
            
            guiGraphics.drawString(this.font, 
                Component.literal("Create a listing to start trading!"),
                centerX + (this.imageWidth / 2) - 80, centerY + 145, 0xBBBBBB, true);
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
                        Component.literal("$" + String.format("%.0f", listing.getMoneyPrice())),
                        priceX, priceY, 0x55FF55, true);
                    priceY += 13;
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
        int formX = centerX + 15;
        int formY = centerY + 76; // Must match initCreateStep1 and mouseClicked
        
        // Section header with underline
        guiGraphics.drawString(this.font, 
            Component.literal(isEditMode ? "Edit Listing - Set Prices" : "Step 1: Set Prices"),
            formX, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(formX, centerY + 61, formX + 200, centerY + 62, 0x60FFD700);
        
        // Money price label
        guiGraphics.drawString(this.font, 
            Component.literal("Money ($):"),
            formX, formY - 12, 0xFFFFFF, true);
        
        // Offer type label
        guiGraphics.drawString(this.font, 
            Component.literal("Type:"),
            formX + 130, formY - 12, 0xFFFFFF, true);
        
        // Price Items section header
        int priceItemY = formY + 38; // Must match initCreateStep1 and mouseClicked
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
                
                // Show item name (after clear button)
                String itemName = itemStack.getHoverName().getString();
                if (itemName.length() > 20) {
                    itemName = itemName.substring(0, 18) + "...";
                }
                guiGraphics.drawString(this.font, 
                    Component.literal(itemName),
                    slotX + 145, slotY + 5, 0xFFFFFF, true);
            } else {
                guiGraphics.drawString(this.font, 
                    Component.literal("Click to select ->"),
                    slotX + 145, slotY + 5, 0x888888, true);
            }
        }
        
        // Help text at bottom (above the Next button at priceItemY + 94)
        guiGraphics.drawString(this.font, 
            Component.literal("* Click slots to pick items"),
            formX, priceItemY + 78, 0x888888, true);
    }
    
    private void renderCreateStep2(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Section header with underline
        guiGraphics.drawString(this.font, 
            Component.literal("Step 2: Place Item to Sell"),
            centerX + 15, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + 15, centerY + 61, centerX + 215, centerY + 62, 0x60FFD700);
        
        // Instructions
        guiGraphics.drawString(this.font, 
            Component.literal("Place or drag an item into the slot below:"),
            centerX + 30, centerY + 68, 0xFFFFFF, true);
        
        // Offering slot is rendered by the menu system at (300, 85)
        // Draw a highlight box around it
        int slotX = centerX + 300 - 10;
        int slotY = centerY + 85 - 1;
        
        ItemStack offeringItem = this.menu.getOfferingItem();
        
        if (offeringItem.isEmpty()) {
            // Empty slot - draw animated highlight
            int alpha = (int)((Math.sin(System.currentTimeMillis() / 300.0) + 1) * 127) + 128;
            int color = (alpha << 24) | 0xFFD700;
            guiGraphics.fill(slotX - 2, slotY - 2, slotX + 20, slotY + 20, color);
            
            Component placeHint = Component.literal(">> Place item here");
            int hintW = this.font.width(placeHint);
            guiGraphics.drawString(this.font, placeHint,
                slotX + 9 - hintW / 2, slotY + 25, 0xFFAA00, true);
        } else {
            // Item placed - show success
            guiGraphics.fill(slotX - 2, slotY - 2, slotX + 20, slotY + 20, 0xFF55FF55);
            
            // Center the item name under the slot
            String itemNameStr = offeringItem.getHoverName().getString();
            Component itemText = Component.literal(itemNameStr);
            int nameW = this.font.width(itemText);
            guiGraphics.drawString(this.font, itemText,
                slotX + 9 - nameW / 2, slotY + 25, 0x55FF55, true);
            // Note: state (itemPlaced/placedItem) is managed by containerTick, not here
        }
        
        // Help text
        guiGraphics.drawString(this.font, 
            Component.literal("* Shift+Click to quick-move items"),
            centerX + 30, centerY + 115, 0x888888, true);
    }
    
    private void renderCreateStep3(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Section header with underline
        Component step3Title = Component.literal(isEditMode ? "Confirm Changes" : "Step 3: Confirm Listing");
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
        
        // Item preview - icon + name
        if (!placedItem.isEmpty()) {
            guiGraphics.fill(infoX - 1, infoY - 1, infoX + 17, infoY + 17, 0xFF555555);
            guiGraphics.fill(infoX, infoY, infoX + 16, infoY + 16, 0xFF8B8B8B);
            guiGraphics.renderItem(placedItem, infoX, infoY);
            guiGraphics.renderItemDecorations(this.font, placedItem, infoX, infoY);
            guiGraphics.drawString(this.font, placedItem.getHoverName(),
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
        
        int formY = centerY + 45;
        
        // Section header with underline (inside scissor area)
        Component offerTitle = Component.literal("Make an Offer");
        int otw = this.font.width(offerTitle);
        guiGraphics.drawString(this.font, offerTitle,
            centerX + 15, centerY + 50, 0xFFD700, true);
        guiGraphics.fill(centerX + 15, centerY + 61, centerX + 15 + otw, centerY + 62, 0x60FFD700);
        
        // Item being offered on (compact single line)
        guiGraphics.drawString(this.font, 
            Component.literal("Item: "),
            centerX + 15, centerY + 63, 0x999999, true);
        guiGraphics.drawString(this.font, selectedListingForOffer.getItemForSale().getHoverName(),
            centerX + 50, centerY + 63, 0xFFFFFF, true);
        String askingStr = " | Asking: $" + String.format("%.0f", selectedListingForOffer.getMoneyPrice());
        int nameEnd = centerX + 50 + this.font.width(selectedListingForOffer.getItemForSale().getHoverName());
        guiGraphics.drawString(this.font, Component.literal(askingStr),
            nameEnd, centerY + 63, 0x999999, true);
        
        // Money offer label
        guiGraphics.drawString(this.font, 
            Component.literal("Your Money Offer:"),
            centerX + 15, formY + 15, 0xFFFFFF, true);
        
        // Item offer labels and slots
        guiGraphics.drawString(this.font, 
            Component.literal("Items to Offer (optional):"),
            centerX + 15, formY + 65, 0xFFFFFF, true);
        
        for (int i = 0; i < 3; i++) {
            int slotX = centerX + 15 + (i * 80);
            int slotY = formY + 80;
            
            // Draw slot background
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF373737);
            
            // Render item if present
            if (!offerItems[i].isEmpty()) {
                guiGraphics.renderItem(offerItems[i], slotX, slotY);
                guiGraphics.renderItemDecorations(this.font, offerItems[i], slotX, slotY);
            }
        }
        
        // Instructions
        guiGraphics.drawString(this.font, 
            Component.literal("Click an item slot, then click an item from your inventory"),
            centerX + 15, formY + 140, 0xAAAAAA, true);
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
                Component.literal("Money: $" + String.format("%.2f", listing.getMoneyPrice())),
                itemX, infoY, 0x55FF55, true);
            infoY += 15;
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
        
        int dialogWidth = 400;
        int dialogHeight = 200;
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
        guiGraphics.drawString(this.font, title, dialogX + (dialogWidth - titleW) / 2, dialogY + 20, 0x55FF55, true);
        
        // Item info - centered icon with name below
        ItemStack itemForSale = listingToBuy.getItemForSale();
        if (!itemForSale.isEmpty()) {
            int iconX = dialogX + dialogWidth / 2 - 8;
            guiGraphics.renderItem(itemForSale, iconX, dialogY + 45);
            guiGraphics.renderItemDecorations(this.font, itemForSale, iconX, dialogY + 45);
            
            Component itemName = itemForSale.getHoverName();
            int nameW = this.font.width(itemName);
            guiGraphics.drawString(this.font, itemName, 
                dialogX + (dialogWidth - nameW) / 2, dialogY + 66, 0xFFFFFF, true);
        }
        
        // Price info centered below item
        String priceText = "Price: $" + String.format("%.2f", listingToBuy.getMoneyPrice());
        Component priceComp = Component.literal(priceText);
        int priceW = this.font.width(priceComp);
        guiGraphics.drawString(this.font, priceComp, dialogX + (dialogWidth - priceW) / 2, dialogY + 82, 0x55FF55, true);
        
        // Required items
        List<PriceItemEntry> reqItems = listingToBuy.getPriceItems();
        if (reqItems != null && !reqItems.isEmpty()) {
            int reqY = dialogY + 98;
            guiGraphics.drawString(this.font, Component.literal("Also requires:"), 
                dialogX + 50, reqY, 0xAAAAAA, true);
            reqY += 12;
            for (PriceItemEntry entry : reqItems) {
                if (entry != null && !entry.isEmpty()) {
                    String text = entry.getAmount() + (entry.isUseStacks() ? " stacks of " : "x ") + 
                        entry.getItemStack().getHoverName().getString();
                    guiGraphics.drawString(this.font, Component.literal("  • " + text), 
                        dialogX + 55, reqY, 0xFFFFFF, true);
                    reqY += 12;
                }
            }
        }
        
        // Balance check
        double balance = com.servermanagement.client.ClientBankData.getBalance();
        if (listingToBuy.getMoneyPrice() > balance) {
            Component warning = Component.literal("⚠ Insufficient funds! Balance: $" + String.format("%.2f", balance));
            int warnW = this.font.width(warning);
            guiGraphics.drawString(this.font, warning, dialogX + (dialogWidth - warnW) / 2, dialogY + dialogHeight - 70, 0xFF5555, true);
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
        
        // Handle price item slot clicks in CREATE_STEP1
        if (currentState == ScreenState.CREATE_STEP1) {
            int formX = centerX + 15;
            int formY = centerY + 76;
            int priceItemY = formY + 38;
            
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
        
        // Handle offer item slot clicks in MAKE_OFFER
        if (currentState == ScreenState.MAKE_OFFER) {
            int formY = centerY + 45;
            
            for (int i = 0; i < 3; i++) {
                int slotX = centerX + 15 + (i * 80);
                int slotY = formY + 80;
                
                if (mouseX >= slotX && mouseX < slotX + 16 &&
                    mouseY >= slotY && mouseY < slotY + 16) {
                    handleOfferItemSlotClick(i);
                    return true;
                }
            }
        }
        
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
     * Handle clicking on an offer item slot - opens item picker
     */
    private void handleOfferItemSlotClick(int slotIndex) {
        if (this.minecraft != null) {
            // Create item picker screen for selecting items from inventory
            ItemPickerScreen picker = new ItemPickerScreen(this, selectedItem -> {
                // Count how many of this item the player has
                int available = 0;
                for (ItemStack invStack : this.minecraft.player.getInventory().items) {
                    if (ItemStack.isSameItemSameComponents(invStack, selectedItem)) {
                        available += invStack.getCount();
                    }
                }
                
                // Create stack with available amount
                ItemStack offerStack = selectedItem.copy();
                offerStack.setCount(Math.min(available, selectedItem.getMaxStackSize()));
                
                offerItems[slotIndex] = offerStack;
                this.rebuildWidgets(); // Refresh UI
            });
            
            // If slot already has an item, highlight it in picker
            if (!offerItems[slotIndex].isEmpty()) {
                picker.setHighlightItem(offerItems[slotIndex]);
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
