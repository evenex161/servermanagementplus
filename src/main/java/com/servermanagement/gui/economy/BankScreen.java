package com.servermanagement.gui.economy;

import com.servermanagement.client.ClientBankData;
import com.servermanagement.client.ClientMoneyRequestData;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.BankTransferPacket;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.RespondMoneyRequestPacket;
import com.servermanagement.network.packet.SendMoneyRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Redesigned Bank Account GUI with tabbed interface.
 * Tabs: Account (balance + history), Transfer, Requests
 */
public class BankScreen extends AbstractContainerScreen<BankMenu> {

    private enum Tab { ACCOUNT, TRANSFER, REQUESTS }

    private static final int TRANSACTIONS_PER_PAGE = 5;
    private static final int REQUESTS_PER_PAGE = 4;
    private static final int STATUS_MESSAGE_DURATION = 80; // 4 seconds

    private final NumberFormat currencyFormat;
    private Tab currentTab = Tab.ACCOUNT;
    private int txnPage = 0;
    private int txnMaxPages = 0;
    private int reqPage = 0;
    private int reqMaxPages = 0;
    private boolean showingIncoming = true; // requests sub-tab

    // Transfer fields
    private EditBox transferNameField;
    private EditBox transferAmountField;

    // Request fields
    private EditBox requestNameField;
    private EditBox requestAmountField;
    private EditBox requestMessageField;

    // Status message
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private int statusTimer = 0;

    public BankScreen(BankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 340;
        this.imageWidth = 440;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // ── Row 1: Header buttons ──
        // Dashboard (admin only)
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2)) {
            this.addRenderableWidget(new ModernButton(
                x0 + 5, y0 + 5, 90, 18,
                Component.literal("← Dashboard"),
                b -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD)),
                ModernButton.ButtonStyle.SECONDARY
            ));
        }

        // Close
        this.addRenderableWidget(new ModernButton(
            x0 + this.imageWidth - 75, y0 + 5, 70, 18,
            Component.literal("Close"),
            b -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));

        // ── Row 2: Quick-nav buttons ──
        int navY = y0 + 28;
        this.addRenderableWidget(new ModernButton(
            x0 + 5, navY, 85, 18,
            Component.literal("Daily Tasks"),
            b -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DAILY_TASKS)),
            ModernButton.ButtonStyle.PRIMARY
        ));
        this.addRenderableWidget(new ModernButton(
            x0 + 95, navY, 95, 18,
            Component.literal("Achievements"),
            b -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.ACHIEVEMENTS)),
            ModernButton.ButtonStyle.PRIMARY
        ));
        this.addRenderableWidget(new ModernButton(
            x0 + 195, navY, 75, 18,
            Component.literal("MineBay"),
            b -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.MINEBAY)),
            ModernButton.ButtonStyle.SUCCESS
        ));
        this.addRenderableWidget(new ModernButton(
            x0 + 275, navY, 85, 18,
            Component.literal("MineStacks"),
            b -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.MINESTACKS)),
            ModernButton.ButtonStyle.PRIMARY
        ));

        // ── Row 3: Tab bar ──
        int tabY = y0 + 52;
        int tabW = (this.imageWidth - 20) / 3;
        this.addRenderableWidget(new ModernButton(
            x0 + 5, tabY, tabW, 20,
            Component.literal("Account"),
            b -> { currentTab = Tab.ACCOUNT; txnPage = 0; this.rebuildWidgets(); },
            currentTab == Tab.ACCOUNT ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));
        this.addRenderableWidget(new ModernButton(
            x0 + 5 + tabW + 5, tabY, tabW, 20,
            Component.literal("Transfer"),
            b -> { currentTab = Tab.TRANSFER; this.rebuildWidgets(); },
            currentTab == Tab.TRANSFER ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));

        // Requests tab — show count badge if there are incoming
        List<ClientMoneyRequestData.RequestEntry> incoming = ClientMoneyRequestData.getIncomingRequests();
        int pendingCount = 0;
        for (ClientMoneyRequestData.RequestEntry r : incoming) {
            if (r.isPending()) pendingCount++;
        }
        String reqLabel = pendingCount > 0 ? "Requests (" + pendingCount + ")" : "Requests";
        ModernButton.ButtonStyle reqStyle = currentTab == Tab.REQUESTS ? ModernButton.ButtonStyle.PRIMARY :
            (pendingCount > 0 ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY);
        this.addRenderableWidget(new ModernButton(
            x0 + 5 + (tabW + 5) * 2, tabY, tabW, 20,
            Component.literal(reqLabel),
            b -> { currentTab = Tab.REQUESTS; reqPage = 0; this.rebuildWidgets(); },
            reqStyle
        ));

        // ── Content area (below tab bar) ──
        int contentY = tabY + 26;

        switch (currentTab) {
            case ACCOUNT -> initAccountTab(x0, y0, contentY);
            case TRANSFER -> initTransferTab(x0, y0, contentY);
            case REQUESTS -> initRequestsTab(x0, y0, contentY);
        }
    }

    // ═══════════════════════════════════════════════════
    //  ACCOUNT TAB
    // ═══════════════════════════════════════════════════

    private void initAccountTab(int x0, int y0, int contentY) {
        // Pagination buttons at bottom
        int bottomY = y0 + this.imageHeight - 28;

        ModernButton prev = new ModernButton(
            x0 + 10, bottomY, 80, 20,
            Component.literal("← Previous"),
            b -> { if (txnPage > 0) { txnPage--; this.rebuildWidgets(); } },
            ModernButton.ButtonStyle.SECONDARY
        );
        this.addRenderableWidget(prev);

        ModernButton next = new ModernButton(
            x0 + this.imageWidth - 90, bottomY, 80, 20,
            Component.literal("Next →"),
            b -> { if (txnPage < txnMaxPages - 1) { txnPage++; this.rebuildWidgets(); } },
            ModernButton.ButtonStyle.SECONDARY
        );
        this.addRenderableWidget(next);

        updateTxnPagination();
        prev.active = txnPage > 0;
        next.active = txnPage < txnMaxPages - 1;
    }

    private void updateTxnPagination() {
        List<Transaction> txns = menu.getRecentTransactions();
        txnMaxPages = Math.max(1, (txns.size() + TRANSACTIONS_PER_PAGE - 1) / TRANSACTIONS_PER_PAGE);
        if (txnPage >= txnMaxPages) txnPage = Math.max(0, txnMaxPages - 1);
    }

    // ═══════════════════════════════════════════════════
    //  TRANSFER TAB
    // ═══════════════════════════════════════════════════

    private void initTransferTab(int x0, int y0, int contentY) {
        int fieldX = x0 + 15;

        // Player name
        if (transferNameField == null) {
            transferNameField = new EditBox(this.font, fieldX, contentY + 25, 180, 18, Component.literal("Player Name"));
            transferNameField.setMaxLength(16);
            transferNameField.setHint(Component.literal("Player name..."));
        } else {
            transferNameField.setPosition(fieldX, contentY + 25);
        }
        this.addRenderableWidget(transferNameField);

        // Amount
        if (transferAmountField == null) {
            transferAmountField = new EditBox(this.font, fieldX, contentY + 65, 180, 18, Component.literal("Amount"));
            transferAmountField.setMaxLength(10);
            transferAmountField.setHint(Component.literal("0.00"));
            transferAmountField.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        } else {
            transferAmountField.setPosition(fieldX, contentY + 65);
        }
        this.addRenderableWidget(transferAmountField);

        // Send button
        this.addRenderableWidget(new ModernButton(
            fieldX, contentY + 95, 180, 22,
            Component.literal("Send Money"),
            b -> sendTransfer(),
            ModernButton.ButtonStyle.SUCCESS
        ));
    }

    private void sendTransfer() {
        String target = transferNameField.getValue().trim();
        String amountStr = transferAmountField.getValue().trim();

        if (target.isEmpty()) { setStatusMessage("Enter a player name", 0xFF5555); return; }
        if (amountStr.isEmpty()) { setStatusMessage("Enter an amount", 0xFF5555); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { setStatusMessage("Amount must be positive", 0xFF5555); return; }
            if (amount > ClientBankData.getBalance()) { setStatusMessage("Insufficient funds", 0xFF5555); return; }

            ModNetworking.sendToServer(new BankTransferPacket(target, amount));
            transferNameField.setValue("");
            transferAmountField.setValue("");
            setStatusMessage("Transfer sent to " + target, 0x55FF55);
        } catch (NumberFormatException e) {
            setStatusMessage("Invalid amount", 0xFF5555);
        }
    }

    // ═══════════════════════════════════════════════════
    //  REQUESTS TAB
    // ═══════════════════════════════════════════════════

    private void initRequestsTab(int x0, int y0, int contentY) {
        // Sub-tabs: Incoming / Outgoing
        int subTabW = 100;
        this.addRenderableWidget(new ModernButton(
            x0 + 15, contentY, subTabW, 18,
            Component.literal("Incoming"),
            b -> { showingIncoming = true; reqPage = 0; this.rebuildWidgets(); },
            showingIncoming ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));
        this.addRenderableWidget(new ModernButton(
            x0 + 15 + subTabW + 5, contentY, subTabW, 18,
            Component.literal("Outgoing"),
            b -> { showingIncoming = false; reqPage = 0; this.rebuildWidgets(); },
            !showingIncoming ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY
        ));

        // New Request button (top right of requests tab)
        // Only show when viewing outgoing
        if (!showingIncoming) {
            this.addRenderableWidget(new ModernButton(
                x0 + this.imageWidth - 125, contentY, 110, 18,
                Component.literal("+ New Request"),
                b -> { /* handled via request form below */ },
                ModernButton.ButtonStyle.SUCCESS
            ));
        }

        // Request action buttons
        List<ClientMoneyRequestData.RequestEntry> requests = showingIncoming ?
            ClientMoneyRequestData.getIncomingRequests() :
            ClientMoneyRequestData.getOutgoingRequests();

        // Filter to pending only
        List<ClientMoneyRequestData.RequestEntry> pending = new java.util.ArrayList<>();
        for (ClientMoneyRequestData.RequestEntry r : requests) {
            if (r.isPending()) pending.add(r);
        }

        reqMaxPages = Math.max(1, (pending.size() + REQUESTS_PER_PAGE - 1) / REQUESTS_PER_PAGE);
        if (reqPage >= reqMaxPages) reqPage = Math.max(0, reqMaxPages - 1);

        int startIdx = reqPage * REQUESTS_PER_PAGE;
        int endIdx = Math.min(startIdx + REQUESTS_PER_PAGE, pending.size());

        int entryY = contentY + 24;
        for (int i = startIdx; i < endIdx; i++) {
            ClientMoneyRequestData.RequestEntry entry = pending.get(i);
            int rowY = entryY + (i - startIdx) * 36;

            if (showingIncoming) {
                // Accept button
                this.addRenderableWidget(new ModernButton(
                    x0 + this.imageWidth - 135, rowY + 2, 55, 16,
                    Component.literal("Pay"),
                    b -> {
                        ModNetworking.sendToServer(new RespondMoneyRequestPacket(
                            entry.getRequestId(), RespondMoneyRequestPacket.Action.ACCEPT));
                        setStatusMessage("Paying...", 0x55FF55);
                    },
                    ModernButton.ButtonStyle.SUCCESS
                ));
                // Deny button
                this.addRenderableWidget(new ModernButton(
                    x0 + this.imageWidth - 75, rowY + 2, 55, 16,
                    Component.literal("Deny"),
                    b -> {
                        ModNetworking.sendToServer(new RespondMoneyRequestPacket(
                            entry.getRequestId(), RespondMoneyRequestPacket.Action.DENY));
                        setStatusMessage("Request denied", 0x808080);
                    },
                    ModernButton.ButtonStyle.DANGER
                ));
            } else {
                // Cancel button for outgoing
                this.addRenderableWidget(new ModernButton(
                    x0 + this.imageWidth - 80, rowY + 2, 60, 16,
                    Component.literal("Cancel"),
                    b -> {
                        ModNetworking.sendToServer(new RespondMoneyRequestPacket(
                            entry.getRequestId(), RespondMoneyRequestPacket.Action.CANCEL));
                        setStatusMessage("Request cancelled", 0x808080);
                    },
                    ModernButton.ButtonStyle.DANGER
                ));
            }
        }

        // Request creation form (shown when Outgoing sub-tab)
        if (!showingIncoming) {
            int formY = y0 + this.imageHeight - 100;

            if (requestNameField == null) {
                requestNameField = new EditBox(this.font, x0 + 15, formY, 120, 16, Component.literal("Name"));
                requestNameField.setMaxLength(16);
                requestNameField.setHint(Component.literal("Player..."));
            } else {
                requestNameField.setPosition(x0 + 15, formY);
            }
            this.addRenderableWidget(requestNameField);

            if (requestAmountField == null) {
                requestAmountField = new EditBox(this.font, x0 + 140, formY, 80, 16, Component.literal("Amount"));
                requestAmountField.setMaxLength(10);
                requestAmountField.setHint(Component.literal("$0.00"));
                requestAmountField.setFilter(s -> s.matches("\\d*\\.?\\d*"));
            } else {
                requestAmountField.setPosition(x0 + 140, formY);
            }
            this.addRenderableWidget(requestAmountField);

            if (requestMessageField == null) {
                requestMessageField = new EditBox(this.font, x0 + 15, formY + 22, 205, 16, Component.literal("Message"));
                requestMessageField.setMaxLength(50);
                requestMessageField.setHint(Component.literal("Reason (optional)..."));
            } else {
                requestMessageField.setPosition(x0 + 15, formY + 22);
            }
            this.addRenderableWidget(requestMessageField);

            this.addRenderableWidget(new ModernButton(
                x0 + 225, formY, this.imageWidth - 240, 38,
                Component.literal("Send Request"),
                b -> sendMoneyRequest(),
                ModernButton.ButtonStyle.SUCCESS
            ));
        }

        // Pagination
        if (reqMaxPages > 1) {
            int bottomY = y0 + this.imageHeight - 28;
            ModernButton prev = new ModernButton(
                x0 + 10, bottomY, 80, 20,
                Component.literal("← Prev"),
                b -> { if (reqPage > 0) { reqPage--; this.rebuildWidgets(); } },
                ModernButton.ButtonStyle.SECONDARY
            );
            prev.active = reqPage > 0;
            this.addRenderableWidget(prev);

            ModernButton next = new ModernButton(
                x0 + this.imageWidth - 90, bottomY, 80, 20,
                Component.literal("Next →"),
                b -> { if (reqPage < reqMaxPages - 1) { reqPage++; this.rebuildWidgets(); } },
                ModernButton.ButtonStyle.SECONDARY
            );
            next.active = reqPage < reqMaxPages - 1;
            this.addRenderableWidget(next);
        }
    }

    private void sendMoneyRequest() {
        String target = requestNameField.getValue().trim();
        String amountStr = requestAmountField.getValue().trim();
        String message = requestMessageField.getValue().trim();

        if (target.isEmpty()) { setStatusMessage("Enter a player name", 0xFF5555); return; }
        if (amountStr.isEmpty()) { setStatusMessage("Enter an amount", 0xFF5555); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < 0.01) { setStatusMessage("Minimum request is $0.01", 0xFF5555); return; }
            if (amount > 1000000) { setStatusMessage("Maximum request is $1,000,000", 0xFF5555); return; }

            ModNetworking.sendToServer(new SendMoneyRequestPacket(target, amount, message));
            requestNameField.setValue("");
            requestAmountField.setValue("");
            requestMessageField.setValue("");
            setStatusMessage("Request sent to " + target, 0x55FF55);
        } catch (NumberFormatException e) {
            setStatusMessage("Invalid amount", 0xFF5555);
        }
    }

    // ═══════════════════════════════════════════════════
    //  STATUS MESSAGE
    // ═══════════════════════════════════════════════════

    private void setStatusMessage(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusTimer = STATUS_MESSAGE_DURATION;
    }

    // ═══════════════════════════════════════════════════
    //  RENDERING
    // ═══════════════════════════════════════════════════

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // Main background
        guiGraphics.fill(x0 - 1, y0 - 1, x0 + this.imageWidth + 1, y0 + this.imageHeight + 1, 0xFF000000);
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + this.imageHeight, 0xE0101010);

        // Header bar (rows 1+2)
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + 50, 0xE0202020);
        guiGraphics.fill(x0, y0 + 49, x0 + this.imageWidth, y0 + 50, 0xFF333333);

        // Tab bar background
        guiGraphics.fill(x0, y0 + 50, x0 + this.imageWidth, y0 + 76, 0xE0181818);
        guiGraphics.fill(x0, y0 + 75, x0 + this.imageWidth, y0 + 76, 0xFF333333);

        // Content background
        guiGraphics.fill(x0 + 5, y0 + 76, x0 + this.imageWidth - 5, y0 + this.imageHeight - 5, 0xE01A1A1A);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x0 = (this.width - this.imageWidth) / 2;
        int y0 = (this.height - this.imageHeight) / 2;

        // Title (centered in header)
        Component titleText = Component.literal("Bank Account");
        int tw = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, x0 + (this.imageWidth - tw) / 2, y0 + 9, 0xFFFFFF, true);

        int contentY = y0 + 78;

        // Render current tab content
        switch (currentTab) {
            case ACCOUNT -> renderAccountTab(guiGraphics, x0, y0, contentY);
            case TRANSFER -> renderTransferTab(guiGraphics, x0, y0, contentY);
            case REQUESTS -> renderRequestsTab(guiGraphics, x0, y0, contentY);
        }

        // Status message (centered, fades)
        if (statusTimer > 0) {
            int alpha = statusTimer < 20 ? (int)(255 * statusTimer / 20.0) : 255;
            int color = (alpha << 24) | (statusColor & 0x00FFFFFF);
            Component msg = Component.literal(statusMessage);
            int mw = this.font.width(msg);
            guiGraphics.drawString(this.font, msg, x0 + (this.imageWidth - mw) / 2, y0 + 65, color, true);
            statusTimer--;
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderAccountTab(GuiGraphics guiGraphics, int x0, int y0, int contentY) {
        // Large balance display
        double balance = ClientBankData.getBalance();
        Component bigBalance = Component.literal(currencyFormat.format(balance));
        int bbw = this.font.width(bigBalance);
        guiGraphics.drawString(this.font, Component.literal("Current Balance"),
            x0 + 15, contentY + 5, 0xAAAAAA, false);
        // Draw balance amount larger by rendering twice offset
        guiGraphics.drawString(this.font, bigBalance,
            x0 + 15, contentY + 18, balance >= 0 ? 0x55FF55 : 0xFF5555, true);

        // Divider
        guiGraphics.fill(x0 + 10, contentY + 35, x0 + this.imageWidth - 10, contentY + 36, 0x40FFFFFF);

        // Transaction history header
        guiGraphics.drawString(this.font, Component.literal("Transaction History"),
            x0 + 15, contentY + 42, 0xCCCCCC, false);

        List<Transaction> txns = menu.getRecentTransactions();
        if (txns.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("No transactions yet"),
                x0 + 25, contentY + 60, 0x808080, false);
        } else {
            updateTxnPagination();
            int start = txnPage * TRANSACTIONS_PER_PAGE;
            int end = Math.min(start + TRANSACTIONS_PER_PAGE, txns.size());

            int rowY = contentY + 57;
            for (int i = start; i < end; i++) {
                Transaction t = txns.get(i);

                // Alternating row bg
                int rowBg = (i - start) % 2 == 0 ? 0x15FFFFFF : 0x08FFFFFF;
                guiGraphics.fill(x0 + 10, rowY - 1, x0 + this.imageWidth - 10, rowY + 17, rowBg);

                // Type badge
                String typeStr = getTransactionTypeString(t.getType());
                int typeColor = t.getType().isIncome() ? 0x55FF55 : 0xFF5555;
                guiGraphics.drawString(this.font, Component.literal(typeStr),
                    x0 + 15, rowY + 2, typeColor, false);

                // Amount
                guiGraphics.drawString(this.font, Component.literal(t.getFormattedAmount()),
                    x0 + 95, rowY + 2, t.getAmount() >= 0 ? 0x55FF55 : 0xFF5555, false);

                // Description (truncated)
                String desc = t.getDescription();
                if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
                guiGraphics.drawString(this.font, Component.literal(desc),
                    x0 + 175, rowY + 2, 0xAAAAAA, false);

                rowY += 18;
            }
        }

        // Page indicator
        if (txnMaxPages > 1) {
            String pageText = "Page " + (txnPage + 1) + " / " + txnMaxPages;
            int pw = this.font.width(pageText);
            guiGraphics.drawString(this.font, Component.literal(pageText),
                x0 + (this.imageWidth - pw) / 2, y0 + this.imageHeight - 24, 0x808080, false);
        }
    }

    private void renderTransferTab(GuiGraphics guiGraphics, int x0, int y0, int contentY) {
        // Quick balance reminder
        double balance = ClientBankData.getBalance();
        guiGraphics.drawString(this.font, Component.literal("Available: " + currencyFormat.format(balance)),
            x0 + 15, contentY + 3, 0x55FF55, false);

        // Labels
        guiGraphics.drawString(this.font, Component.literal("Recipient:"),
            x0 + 15, contentY + 16, 0xCCCCCC, false);

        guiGraphics.drawString(this.font, Component.literal("Amount ($):"),
            x0 + 15, contentY + 56, 0xCCCCCC, false);

        // Helpful text
        guiGraphics.drawString(this.font, Component.literal("Send money to another online player."),
            x0 + 15, contentY + 125, 0x666666, false);
        guiGraphics.drawString(this.font, Component.literal("The recipient must be currently online."),
            x0 + 15, contentY + 138, 0x666666, false);
    }

    private void renderRequestsTab(GuiGraphics guiGraphics, int x0, int y0, int contentY) {
        // Balance display (right-aligned in sub-tab row)
        double balance = ClientBankData.getBalance();
        String balStr = "Balance: " + currencyFormat.format(balance);
        int balW = this.font.width(balStr);
        guiGraphics.drawString(this.font, Component.literal(balStr),
            x0 + this.imageWidth - balW - 15, contentY + 5,
            balance >= 0 ? 0x55FF55 : 0xFF5555, false);

        List<ClientMoneyRequestData.RequestEntry> requests = showingIncoming ?
            ClientMoneyRequestData.getIncomingRequests() :
            ClientMoneyRequestData.getOutgoingRequests();

        List<ClientMoneyRequestData.RequestEntry> pending = new java.util.ArrayList<>();
        for (ClientMoneyRequestData.RequestEntry r : requests) {
            if (r.isPending()) pending.add(r);
        }

        int entryY = contentY + 24;

        if (pending.isEmpty()) {
            String emptyMsg = showingIncoming ? "No incoming requests" : "No outgoing requests";
            guiGraphics.drawString(this.font, Component.literal(emptyMsg),
                x0 + 25, entryY + 10, 0x808080, false);
        } else {
            int start = reqPage * REQUESTS_PER_PAGE;
            int end = Math.min(start + REQUESTS_PER_PAGE, pending.size());

            for (int i = start; i < end; i++) {
                ClientMoneyRequestData.RequestEntry entry = pending.get(i);
                int rowY = entryY + (i - start) * 36;

                // Row background
                guiGraphics.fill(x0 + 10, rowY - 2, x0 + this.imageWidth - 10, rowY + 30, 0x18FFFFFF);

                // Player name + amount
                String label = showingIncoming ?
                    entry.getPlayerName() + " requests" :
                    "To " + entry.getPlayerName();
                guiGraphics.drawString(this.font, Component.literal(label),
                    x0 + 15, rowY + 2, 0xFFFFFF, false);

                String amtStr = String.format("$%.2f", entry.getAmount());
                guiGraphics.drawString(this.font, Component.literal(amtStr),
                    x0 + 15 + this.font.width(label) + 5, rowY + 2, 0xFFD700, false);

                // Message + age
                String msg = entry.getMessage().isEmpty() ? "" : "\"" + entry.getMessage() + "\"";
                if (msg.length() > 35) msg = msg.substring(0, 32) + "...\"";
                guiGraphics.drawString(this.font, Component.literal(msg),
                    x0 + 15, rowY + 15, 0x888888, false);

                guiGraphics.drawString(this.font, Component.literal(entry.getAge()),
                    x0 + 15 + this.font.width(msg) + (msg.isEmpty() ? 0 : 8), rowY + 15,
                    0x666666, false);
            }
        }

        // "New Request" form label (outgoing only)
        if (!showingIncoming) {
            int formY = y0 + this.imageHeight - 105;
            guiGraphics.fill(x0 + 10, formY - 5, x0 + this.imageWidth - 10, formY - 4, 0x40FFFFFF);
            guiGraphics.drawString(this.font, Component.literal("New Request:"),
                x0 + 15, formY - 14, 0xCCCCCC, false);
        }

        // Page indicator
        if (reqMaxPages > 1) {
            String pageText = "Page " + (reqPage + 1) + " / " + reqMaxPages;
            int pw = this.font.width(pageText);
            int pageY = showingIncoming ? y0 + this.imageHeight - 24 : y0 + this.imageHeight - 118;
            guiGraphics.drawString(this.font, Component.literal(pageText),
                x0 + (this.imageWidth - pw) / 2, pageY, 0x808080, false);
        }
    }

    private String getTransactionTypeString(TransactionType type) {
        return switch (type) {
            case ACHIEVEMENT -> "REWARD";
            case PLAYER_TRANSFER_SENT -> "SENT";
            case PLAYER_TRANSFER_RECEIVED -> "RECV";
            case ADMIN_GIVE -> "GIFT";
            case ADMIN_TAKE -> "DEDUCT";
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If any EditBox is focused, prevent keybinds
        if (isAnyFieldFocused()) {
            if (getFocusedField() != null && getFocusedField().keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true; // block keybinds like E
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isAnyFieldFocused()) {
            EditBox focused = getFocusedField();
            if (focused != null && focused.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isAnyFieldFocused() {
        return (transferNameField != null && transferNameField.isFocused())
            || (transferAmountField != null && transferAmountField.isFocused())
            || (requestNameField != null && requestNameField.isFocused())
            || (requestAmountField != null && requestAmountField.isFocused())
            || (requestMessageField != null && requestMessageField.isFocused());
    }

    private EditBox getFocusedField() {
        if (transferNameField != null && transferNameField.isFocused()) return transferNameField;
        if (transferAmountField != null && transferAmountField.isFocused()) return transferAmountField;
        if (requestNameField != null && requestNameField.isFocused()) return requestNameField;
        if (requestAmountField != null && requestAmountField.isFocused()) return requestAmountField;
        if (requestMessageField != null && requestMessageField.isFocused()) return requestMessageField;
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
