package com.servermanagement.gui.screen;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.gui.MotdEditorMenu;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.SaveMotdPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;

/**
 * MOTD Editor Screen with live preview and formatting controls.
 * <p>
 * Supports Minecraft color codes (&amp;0-&amp;f), formatting codes (&amp;l, &amp;o, &amp;n, &amp;m, &amp;k),
 * and provides a live rendered preview of the final server list MOTD.
 */
public class MotdEditorScreen extends ScalableContainerScreen<MotdEditorMenu> {

    private static final int SCREEN_WIDTH = 420;
    private static final int SCREEN_HEIGHT = 330;

    /** MOTD supports two lines separated by \n */
    private EditBox line1Box;
    private EditBox line2Box;

    /** Color code palette: codes 0-f */
    private static final char[] COLOR_CODES = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /** Human-readable color names for tooltips */
    private static final String[] COLOR_NAMES = {
        "Black", "Dark Blue", "Dark Green", "Dark Aqua",
        "Dark Red", "Dark Purple", "Gold", "Gray",
        "Dark Gray", "Blue", "Green", "Aqua",
        "Red", "Light Purple", "Yellow", "White"
    };

    /** Actual ARGB colors for palette swatches */
    private static final int[] COLOR_VALUES = {
        0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
        0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
        0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
        0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    /** Formatting codes: bold, italic, underline, strikethrough, obfuscated, reset */
    private static final char[] FORMAT_CODES = { 'l', 'o', 'n', 'm', 'k', 'r' };
    private static final String[] FORMAT_LABELS = { "B", "I", "U", "S", "?", "R" };
    private static final String[] FORMAT_NAMES = {
        "Bold", "Italic", "Underline", "Strikethrough", "Obfuscated", "Reset"
    };

    /** Which line the user last clicked in (1 or 2), for inserting codes */
    private int activeLineIndex = 1;

    /** Tracks the original MOTD at open time to detect unsaved changes */
    private String originalMotdText;

    /** Whether the unsaved-changes confirmation dialog is visible */
    private boolean showingConfirmDialog = false;

    /** The action to run if the user confirms exit without saving */
    private Runnable pendingExitAction = null;

    /** Whether the editor is in advanced mode (shows raw &-codes in EditBoxes) */
    private boolean advancedMode = false;

    /** In simple mode, the formatted text with &-codes stored separately from EditBox display */
    private String line1Formatted = "";
    private String line2Formatted = "";

    /** Previous display text for tracking diffs in simple mode */
    private String previousDisplayL1 = "";
    private String previousDisplayL2 = "";

    /** Prevents responder from firing during programmatic EditBox updates */
    private boolean suppressResponder = false;

    /** Mode toggle button reference for updating its label */
    private ModernButton modeToggleButton;

    /** Formatting button references for toggling active state */
    private final ModernButton[] formatButtons = new ModernButton[5]; // l, o, n, m, k (not reset)

    public MotdEditorScreen(MotdEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // Pull fresh MOTD text from the cache on every init() — but only before
        // the user has started editing (originalMotdText == null), so a late
        // SyncMotdPacket triggering refreshOpenScreen() doesn't clobber pending
        // typing.
        if (this.originalMotdText == null) {
            this.menu.reloadFromClientCache();
        }

        int cx = (this.width - this.imageWidth) / 2;
        int cy = (this.height - this.imageHeight) / 2;

        // Parse existing MOTD into two lines
        String motd = this.menu.getMotdText();
        if (this.originalMotdText == null) {
            this.originalMotdText = motd;
        }
        String[] parts = splitMotd(motd);
        if (line1Formatted.isEmpty() && line2Formatted.isEmpty()
                && (!parts[0].isEmpty() || !parts[1].isEmpty())) {
            line1Formatted = parts[0];
            line2Formatted = parts[1];
        }

        // --- Line 1 input ---
        this.line1Box = new EditBox(this.font, cx + 15, cy + 60, this.imageWidth - 30, 18, Component.literal("Line 1"));
        this.line1Box.setMaxLength(256);
        if (advancedMode) {
            this.line1Box.setValue(line1Formatted);
        } else {
            String d1 = stripFormattingCodes(line1Formatted);
            this.line1Box.setValue(d1);
            this.previousDisplayL1 = d1;
        }
        this.line1Box.setResponder(text -> {
            if (suppressResponder) return;
            this.activeLineIndex = 1;
            if (!advancedMode) {
                line1Formatted = applyDisplayDiff(previousDisplayL1, text, line1Formatted);
                previousDisplayL1 = text;
            }
            updateMenuMotd();
        });
        this.addRenderableWidget(this.line1Box);

        // --- Line 2 input ---
        this.line2Box = new EditBox(this.font, cx + 15, cy + 100, this.imageWidth - 30, 18, Component.literal("Line 2"));
        this.line2Box.setMaxLength(256);
        if (advancedMode) {
            this.line2Box.setValue(line2Formatted);
        } else {
            String d2 = stripFormattingCodes(line2Formatted);
            this.line2Box.setValue(d2);
            this.previousDisplayL2 = d2;
        }
        this.line2Box.setResponder(text -> {
            if (suppressResponder) return;
            this.activeLineIndex = 2;
            if (!advancedMode) {
                line2Formatted = applyDisplayDiff(previousDisplayL2, text, line2Formatted);
                previousDisplayL2 = text;
            }
            updateMenuMotd();
        });
        this.addRenderableWidget(this.line2Box);

        // --- Color palette buttons ---
        int paletteY = cy + 130;
        int swatchSize = 16;
        int swatchSpacing = 4;
        int paletteStartX = cx + 15;
        for (int i = 0; i < COLOR_CODES.length; i++) {
            final char code = COLOR_CODES[i];
            int col = i % 8;
            int row = i / 8;
            int bx = paletteStartX + col * (swatchSize + swatchSpacing);
            int by = paletteY + row * (swatchSize + swatchSpacing);
            this.addRenderableWidget(new ColorSwatchButton(bx, by, swatchSize, swatchSize,
                    COLOR_VALUES[i], COLOR_NAMES[i], () -> insertCode('&', code)));
        }

        // --- Formatting buttons (positioned relative to imageWidth) ---
        int fmtY = paletteY;
        int fmtStartX = cx + this.imageWidth / 2 + 10;
        int fmtBtnW = Math.min(24, (cx + this.imageWidth - 10 - fmtStartX - 5 * 4) / 6);
        int fmtBtnSpacing = fmtBtnW + 4;
        for (int i = 0; i < FORMAT_CODES.length; i++) {
            final char code = FORMAT_CODES[i];
            final int idx = i;
            int bx = fmtStartX + i * fmtBtnSpacing;
            ModernButton btn = new ModernButton(
                    bx, fmtY, fmtBtnW, 18,
                    Component.literal(FORMAT_LABELS[i]),
                    b -> {
                        if (code == 'r') {
                            insertCode('&', 'r');
                        } else {
                            toggleFormat(code);
                        }
                    },
                    ModernButton.ButtonStyle.DARK
            );
            this.addRenderableWidget(btn);
            if (idx < formatButtons.length) {
                formatButtons[idx] = btn;
            }
        }

        // --- Second row formatting: animation helpers ---
        int animY = paletteY + 24;
        int animBtnW = (cx + this.imageWidth - 10 - fmtStartX - 4) / 2;
        // Rainbow text shortcut
        this.addRenderableWidget(new ModernButton(
                fmtStartX, animY, animBtnW, 18,
                Component.literal("Rainbow"),
                btn -> insertRainbowCodes(),
                ModernButton.ButtonStyle.PRIMARY
        ));
        // Gradient shortcut
        this.addRenderableWidget(new ModernButton(
                fmtStartX + animBtnW + 4, animY, animBtnW, 18,
                Component.literal("Gradient"),
                btn -> insertGradientCodes(),
                ModernButton.ButtonStyle.PRIMARY
        ));

        // --- Mode toggle button ---
        this.modeToggleButton = new ModernButton(
                cx + this.imageWidth - 85, cy + 8, 70, 18,
                Component.literal(advancedMode ? "Advanced" : "Simple"),
                btn -> toggleMode(),
                ModernButton.ButtonStyle.DARK
        );
        this.addRenderableWidget(this.modeToggleButton);

        // --- Action buttons ---
        int btnY = cy + this.imageHeight - 35;
        int actionAreaW = this.imageWidth - 20;
        int dashBtnW = (int)(actionAreaW * 0.30);
        int clearBtnW = (int)(actionAreaW * 0.20);
        int saveBtnW = actionAreaW - dashBtnW - clearBtnW - 16;

        // Back to Dashboard
        this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("\u2190 Dashboard"),
                btn -> attemptExit(() -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.DASHBOARD, ""))))
                .bounds(cx + 10, btnY, dashBtnW, 24)
                .style(ModernButton.ButtonStyle.SECONDARY)
                .build());

        // Clear
        this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Clear"),
                btn -> {
                    suppressResponder = true;
                    this.line1Box.setValue("");
                    this.line2Box.setValue("");
                    suppressResponder = false;
                    if (!advancedMode) {
                        line1Formatted = "";
                        line2Formatted = "";
                        previousDisplayL1 = "";
                        previousDisplayL2 = "";
                    }
                    updateMenuMotd();
                })
                .bounds(cx + 10 + dashBtnW + 8, btnY, clearBtnW, 24)
                .style(ModernButton.ButtonStyle.DANGER)
                .build());

        // Save
        this.addRenderableWidget(new ModernButton.Builder(
                Component.literal("Save MOTD"),
                btn -> {
                    if (advancedMode) {
                        line1Formatted = line1Box.getValue();
                        line2Formatted = line2Box.getValue();
                    }
                    updateMenuMotd();
                    ModNetworking.sendToServer(new SaveMotdPacket(this.menu.getMotdText()));
                    this.originalMotdText = this.menu.getMotdText();
                })
                .bounds(cx + this.imageWidth - saveBtnW - 10, btnY, saveBtnW, 24)
                .style(ModernButton.ButtonStyle.SUCCESS)
                .build());

        // Set initial focus to line 1 so it's immediately editable
        this.line1Box.setFocused(true);
        this.setFocused(this.line1Box);
    }

    private void updateMenuMotd() {
        String l1, l2;
        if (advancedMode) {
            l1 = this.line1Box.getValue();
            l2 = this.line2Box.getValue();
        } else {
            l1 = this.line1Formatted;
            l2 = this.line2Formatted;
        }
        String combined = l2.isEmpty() ? l1 : l1 + "\n" + l2;
        this.menu.setMotdText(combined);
    }

    /** Returns true if the current MOTD text differs from what was loaded */
    private boolean hasUnsavedChanges() {
        updateMenuMotd();
        String current = this.menu.getMotdText();
        return !current.equals(this.originalMotdText);
    }

    /** If there are unsaved changes, show the confirmation dialog; otherwise run the exit action immediately */
    private void attemptExit(Runnable exitAction) {
        if (hasUnsavedChanges()) {
            this.pendingExitAction = exitAction;
            this.showingConfirmDialog = true;
        } else {
            exitAction.run();
        }
    }

    @Override
    public void onClose() {
        if (hasUnsavedChanges() && !showingConfirmDialog) {
            this.pendingExitAction = super::onClose;
            this.showingConfirmDialog = true;
            return;
        }
        if (!showingConfirmDialog) {
            super.onClose();
        }
    }

    private String[] splitMotd(String motd) {
        if (motd == null || motd.isEmpty()) {
            return new String[]{"", ""};
        }
        int idx = motd.indexOf('\n');
        if (idx < 0) {
            return new String[]{motd, ""};
        }
        return new String[]{motd.substring(0, idx), motd.substring(idx + 1)};
    }

    private void insertCode(char prefix, char code) {
        EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;
        String insertion = "" + prefix + code;
        if (advancedMode) {
            String current = target.getValue();
            int insertPos = getSelectionStart(target);
            String newText = current.substring(0, insertPos) + insertion + current.substring(insertPos);
            target.setValue(newText);
            target.setCursorPosition(insertPos + insertion.length());
        } else {
            String formatted = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
            int displayInsertPos = getSelectionStart(target);
            int formattedPos = displayToFormattedPos(formatted, displayInsertPos);
            String newFormatted = formatted.substring(0, formattedPos) + insertion + formatted.substring(formattedPos);
            if (activeLineIndex == 2) {
                line2Formatted = newFormatted;
            } else {
                line1Formatted = newFormatted;
            }
        }
        target.setFocused(true);
        updateMenuMotd();
    }

    /**
     * Returns the start position of the current selection in the EditBox,
     * or the cursor position if nothing is selected.
     */
    private int getSelectionStart(EditBox box) {
        int cursor = box.getCursorPosition();
        String highlighted = box.getHighlighted();
        if (highlighted == null || highlighted.isEmpty()) {
            return cursor;
        }
        // highlighted text is between min(cursor, highlightPos) and max(cursor, highlightPos)
        // Determine if cursor is at the start or end of the selection
        String value = box.getValue();
        int selLen = highlighted.length();
        if (cursor >= selLen) {
            String textBeforeCursor = value.substring(cursor - selLen, cursor);
            if (textBeforeCursor.equals(highlighted)) {
                return cursor - selLen;
            }
        }
        return cursor;
    }

    private void insertRainbowCodes() {
        char[] rainbow = {'c', '6', 'e', 'a', 'b', '9', 'd'};
        EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;
        int cursor = target.getCursorPosition();

        if (advancedMode) {
            String current = target.getValue();
            String afterCursor = current.substring(cursor);
            String beforeCursor = current.substring(0, cursor);
            if (afterCursor.isEmpty()) {
                insertCode('&', 'c');
                return;
            }
            StringBuilder sb = new StringBuilder();
            int colorIdx = 0;
            for (int i = 0; i < afterCursor.length(); i++) {
                char ch = afterCursor.charAt(i);
                if (ch == '&' && i + 1 < afterCursor.length()) {
                    sb.append(ch);
                    sb.append(afterCursor.charAt(++i));
                    continue;
                }
                sb.append('&').append(rainbow[colorIdx % rainbow.length]);
                sb.append(ch);
                colorIdx++;
            }
            target.setValue(beforeCursor + sb.toString());
            target.setCursorPosition(target.getValue().length());
        } else {
            String formatted = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
            int formattedCursor = displayToFormattedPos(formatted, cursor);
            String beforeCursor = formatted.substring(0, formattedCursor);
            String afterCursor = formatted.substring(formattedCursor);
            String plainAfter = stripFormattingCodes(afterCursor);
            if (plainAfter.isEmpty()) {
                insertCode('&', 'c');
                return;
            }
            StringBuilder sb = new StringBuilder();
            int colorIdx = 0;
            for (int i = 0; i < plainAfter.length(); i++) {
                sb.append('&').append(rainbow[colorIdx % rainbow.length]);
                sb.append(plainAfter.charAt(i));
                colorIdx++;
            }
            if (activeLineIndex == 2) {
                line2Formatted = beforeCursor + sb.toString();
            } else {
                line1Formatted = beforeCursor + sb.toString();
            }
        }
        updateMenuMotd();
    }

    private void insertGradientCodes() {
        char[] gradient = {'1', '9', '3', 'b', 'a'};
        EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;
        int cursor = target.getCursorPosition();

        if (advancedMode) {
            String current = target.getValue();
            String afterCursor = current.substring(cursor);
            String beforeCursor = current.substring(0, cursor);
            if (afterCursor.isEmpty()) {
                insertCode('&', '1');
                return;
            }
            StringBuilder sb = new StringBuilder();
            int len = afterCursor.length();
            for (int i = 0; i < len; i++) {
                char ch = afterCursor.charAt(i);
                if (ch == '&' && i + 1 < len) {
                    sb.append(ch);
                    sb.append(afterCursor.charAt(++i));
                    continue;
                }
                int gradIdx = (int) ((float) i / len * gradient.length);
                if (gradIdx >= gradient.length) gradIdx = gradient.length - 1;
                sb.append('&').append(gradient[gradIdx]);
                sb.append(ch);
            }
            target.setValue(beforeCursor + sb.toString());
            target.setCursorPosition(target.getValue().length());
        } else {
            String formatted = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
            int formattedCursor = displayToFormattedPos(formatted, cursor);
            String beforeCursor = formatted.substring(0, formattedCursor);
            String afterCursor = formatted.substring(formattedCursor);
            String plainAfter = stripFormattingCodes(afterCursor);
            if (plainAfter.isEmpty()) {
                insertCode('&', '1');
                return;
            }
            StringBuilder sb = new StringBuilder();
            int len = plainAfter.length();
            for (int i = 0; i < len; i++) {
                int gradIdx = (int) ((float) i / len * gradient.length);
                if (gradIdx >= gradient.length) gradIdx = gradient.length - 1;
                sb.append('&').append(gradient[gradIdx]);
                sb.append(plainAfter.charAt(i));
            }
            if (activeLineIndex == 2) {
                line2Formatted = beforeCursor + sb.toString();
            } else {
                line1Formatted = beforeCursor + sb.toString();
            }
        }
        updateMenuMotd();
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        int cx = this.leftPos;
        int cy = this.topPos;

        // Render widgets first (super.render calls renderBg internally)
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);

        // Draw all labels AFTER super.render() so they don't get covered
        // by the second renderBg call inside AbstractContainerScreen.render()

        // Title
        guiGraphics.drawString(this.font, "MOTD Editor",
                cx + 15, cy + 8, 0xFFD700, true);
        guiGraphics.drawString(this.font, "Server Message of the Day",
                cx + 15, cy + 20, 0xAAAAAA, true);
        guiGraphics.drawString(this.font, "Mode:", cx + this.imageWidth - 130, cy + 13, 0x888888, true);

        // Labels
        guiGraphics.drawString(this.font, "Line 1:", cx + 15, cy + 48, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Line 2:", cx + 15, cy + 88, 0xFFFFFF, true);

        // Color palette label
        guiGraphics.drawString(this.font, "Colors:", cx + 15, cy + 120, 0xCCCCCC, true);

        // Formatting label
        guiGraphics.drawString(this.font, "Format:", cx + this.imageWidth / 2 + 10, cy + 120, 0xCCCCCC, true);

        // Format button tooltips
        int fmtStartX = cx + this.imageWidth / 2 + 10;
        int fmtBtnW = Math.min(24, (cx + this.imageWidth - 10 - fmtStartX - 5 * 4) / 6);
        int fmtBtnSpacing = fmtBtnW + 4;
        int fmtY = cy + 130;
        for (int i = 0; i < FORMAT_NAMES.length; i++) {
            int bx = fmtStartX + i * fmtBtnSpacing;
            if (mouseX >= bx && mouseX < bx + fmtBtnW && mouseY >= fmtY && mouseY < fmtY + 18) {
                guiGraphics.renderTooltip(this.font, Component.literal("&" + FORMAT_CODES[i] + " - " + FORMAT_NAMES[i]), mouseX, mouseY);
            }
        }

        // --- Live Preview ---
        int previewY = cy + 180;
        guiGraphics.drawString(this.font, "Live Preview:", cx + 15, previewY - 12, 0xCCCCCC, true);

        // Preview box background (simulating server list dark background)
        int previewBoxX = cx + 15;
        int previewBoxW = this.imageWidth - 30;
        int previewBoxH = 50;
        guiGraphics.fill(previewBoxX, previewY, previewBoxX + previewBoxW, previewY + previewBoxH, 0xFF2C2C2C);
        // Border
        guiGraphics.fill(previewBoxX, previewY, previewBoxX + previewBoxW, previewY + 1, 0xFF555555);
        guiGraphics.fill(previewBoxX, previewY + previewBoxH - 1, previewBoxX + previewBoxW, previewY + previewBoxH, 0xFF555555);
        guiGraphics.fill(previewBoxX, previewY, previewBoxX + 1, previewY + previewBoxH, 0xFF555555);
        guiGraphics.fill(previewBoxX + previewBoxW - 1, previewY, previewBoxX + previewBoxW, previewY + previewBoxH, 0xFF555555);

        // Render the MOTD preview with color code parsing
        String previewL1 = advancedMode ? (this.line1Box != null ? this.line1Box.getValue() : "") : this.line1Formatted;
        String previewL2 = advancedMode ? (this.line2Box != null ? this.line2Box.getValue() : "") : this.line2Formatted;
        renderFormattedLine(guiGraphics, previewL1, previewBoxX + 8, previewY + 8);
        renderFormattedLine(guiGraphics, previewL2, previewBoxX + 8, previewY + 22);

        // Update format button toggle states based on cursor position
        updateFormatButtonStates();

        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render unsaved-changes confirmation dialog on top of everything
        if (showingConfirmDialog) {
            renderConfirmDialog(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Renders a modal confirmation dialog asking the user whether to discard unsaved changes.
     */
    private void renderConfirmDialog(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Push pose to render above all other widgets
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);

        // Semi-transparent overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        int dialogW = 240;
        int dialogH = 100;
        int dx = (this.width - dialogW) / 2;
        int dy = (this.height - dialogH) / 2;

        // Dialog background
        guiGraphics.fill(dx, dy, dx + dialogW, dy + dialogH, 0xFF1A1A2E);
        // Border
        guiGraphics.fill(dx, dy, dx + dialogW, dy + 1, 0xFFFFD700);
        guiGraphics.fill(dx, dy + dialogH - 1, dx + dialogW, dy + dialogH, 0xFFFFD700);
        guiGraphics.fill(dx, dy, dx + 1, dy + dialogH, 0xFFFFD700);
        guiGraphics.fill(dx + dialogW - 1, dy, dx + dialogW, dy + dialogH, 0xFFFFD700);

        // Title
        String title = "Unsaved Changes";
        guiGraphics.drawString(this.font, title, dx + (dialogW - this.font.width(title)) / 2, dy + 10, 0xFFD700, true);

        // Message
        String msg = "You have unsaved changes.";
        guiGraphics.drawString(this.font, msg, dx + (dialogW - this.font.width(msg)) / 2, dy + 28, 0xFFFFFF, false);
        String msg2 = "Exit without saving?";
        guiGraphics.drawString(this.font, msg2, dx + (dialogW - this.font.width(msg2)) / 2, dy + 42, 0xAAAAAA, false);

        // Buttons
        int btnW = 90;
        int btnH = 20;
        int btnY = dy + dialogH - 30;
        int cancelX = dx + dialogW / 2 - btnW - 8;
        int discardX = dx + dialogW / 2 + 8;

        // Cancel button
        boolean cancelHover = mouseX >= cancelX && mouseX < cancelX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        guiGraphics.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, cancelHover ? 0xFF4A90D9 : 0xFF3A6FB5);
        String cancelText = "Cancel";
        guiGraphics.drawString(this.font, cancelText, cancelX + (btnW - this.font.width(cancelText)) / 2, btnY + 6, 0xFFFFFF, true);

        // Discard button
        boolean discardHover = mouseX >= discardX && mouseX < discardX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        guiGraphics.fill(discardX, btnY, discardX + btnW, btnY + btnH, discardHover ? 0xFFCC4444 : 0xFFAA3333);
        String discardText = "Discard & Exit";
        guiGraphics.drawString(this.font, discardText, discardX + (btnW - this.font.width(discardText)) / 2, btnY + 6, 0xFFFFFF, true);

        guiGraphics.pose().popPose();
    }

    /**
     * Renders a single line of text, interpreting &amp;-based color and formatting codes.
     * Supports colors 0-f and formatting l (bold), o (italic), n (underline), m (strikethrough),
     * k (obfuscated), and r (reset).
     */
    private void renderFormattedLine(GuiGraphics guiGraphics, String text, int x, int y) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int currentColor = 0xFFFFFF;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        int drawX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                int colorIndex = "0123456789abcdef".indexOf(code);
                if (colorIndex >= 0) {
                    currentColor = COLOR_VALUES[colorIndex] & 0x00FFFFFF;
                    // Color code only changes color, preserves active formatting
                    i++;
                    continue;
                }
                switch (code) {
                    case 'l': bold = true; i++; continue;
                    case 'o': italic = true; i++; continue;
                    case 'n': underline = true; i++; continue;
                    case 'm': strikethrough = true; i++; continue;
                    case 'k': obfuscated = true; i++; continue;
                    case 'r':
                        currentColor = 0xFFFFFF;
                        bold = false;
                        italic = false;
                        underline = false;
                        strikethrough = false;
                        obfuscated = false;
                        i++;
                        continue;
                }
            }

            // Render the character using Component with Style for proper multi-format support
            String charStr = String.valueOf(ch);
            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(currentColor))
                    .withBold(bold)
                    .withItalic(italic)
                    .withUnderlined(underline)
                    .withStrikethrough(strikethrough)
                    .withObfuscated(obfuscated);
            Component comp = Component.literal(charStr).withStyle(style);
            guiGraphics.drawString(this.font, comp, drawX, y, 0xFFFFFF, true);
            drawX += this.font.width(comp);
        }
    }

    private static char getObfuscatedChar(char original) {
        if (original == ' ') return ' ';
        // Cycle through printable ASCII for the obfuscated effect
        long tick = System.currentTimeMillis() / 50;
        return (char) (33 + (tick + original) % 93);
    }

    /**
     * Scans the formatted text from the beginning up to the cursor position
     * and returns which formatting codes (l, o, n, m, k) are currently active.
     */
    private boolean[] getActiveFormatsAtCursor() {
        boolean[] active = new boolean[5]; // l=0, o=1, n=2, m=3, k=4
        EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;
        if (target == null) return active;

        String formatted;
        int scanUpTo;
        if (advancedMode) {
            formatted = target.getValue();
            scanUpTo = target.getCursorPosition();
        } else {
            formatted = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
            scanUpTo = displayToFormattedPos(formatted, target.getCursorPosition());
        }
        if (scanUpTo > formatted.length()) scanUpTo = formatted.length();

        // Include format codes sitting right at the cursor position so they are detected as active
        while (scanUpTo < formatted.length() - 1 && formatted.charAt(scanUpTo) == '&'
                && isFormattingCode(formatted.charAt(scanUpTo + 1))) {
            scanUpTo += 2;
        }

        for (int i = 0; i < scanUpTo - 1; i++) {
            if (formatted.charAt(i) == '&') {
                char code = Character.toLowerCase(formatted.charAt(i + 1));
                switch (code) {
                    case 'l': active[0] = true; i++; break;
                    case 'o': active[1] = true; i++; break;
                    case 'n': active[2] = true; i++; break;
                    case 'm': active[3] = true; i++; break;
                    case 'k': active[4] = true; i++; break;
                    case 'r':
                        active[0] = active[1] = active[2] = active[3] = active[4] = false;
                        i++;
                        break;
                    default:
                        if ("0123456789abcdef".indexOf(code) >= 0) {
                            // Color codes do NOT reset formatting in our system
                            i++;
                        }
                        break;
                }
            }
        }
        return active;
    }

    /** Updates the toggle visual state of formatting buttons based on active formats at cursor */
    private void updateFormatButtonStates() {
        boolean[] active = getActiveFormatsAtCursor();
        for (int i = 0; i < formatButtons.length; i++) {
            if (formatButtons[i] != null) {
                formatButtons[i].setToggled(active[i]);
            }
        }
    }

    /**
     * Toggles a formatting code at the cursor position.
     * If the format is already active, deactivates it by removing its code at the
     * cursor and (if still active from earlier codes) inserting &amp;r plus re-applied formats.
     * If not active, inserts the format code.
     */
    private void toggleFormat(char code) {
        boolean[] active = getActiveFormatsAtCursor();
        int codeIndex = "lonmk".indexOf(Character.toLowerCase(code));
        if (codeIndex < 0) {
            insertCode('&', code);
            return;
        }

        if (active[codeIndex]) {
            // Format is active — deactivate it
            EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;

            String text;
            int cursorPos;
            if (advancedMode) {
                text = target.getValue();
                cursorPos = getSelectionStart(target);
            } else {
                text = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
                cursorPos = displayToFormattedPos(text, getSelectionStart(target));
            }

            String result = deactivateFormatInText(text, cursorPos, code, codeIndex);

            if (advancedMode) {
                target.setValue(result);
                target.setCursorPosition(cursorPos);
            } else {
                if (activeLineIndex == 2) {
                    line2Formatted = result;
                } else {
                    line1Formatted = result;
                }
            }
            target.setFocused(true);
            updateMenuMotd();
        } else {
            // Format is not active — just insert it
            insertCode('&', code);
        }
    }

    /**
     * Deactivates a single format code in the text at the given cursor position.
     * <p>
     * 1) Removes any {@code &code} pairs for the target format from the format codes
     *    sitting right at the cursor position (so they don't re-enable after our reset).
     * 2) Scans from the start up to (but not including) the cursor to determine whether
     *    the format is still active from earlier codes.  If so, inserts {@code &r} plus
     *    re-application of the colour and other still-active formats at the cursor position
     *    (before any remaining codes at the cursor that belong to other formats).
     */
    private String deactivateFormatInText(String text, int cursorPos, char code, int codeIndex) {
        StringBuilder sb = new StringBuilder(text);

        // Step 1: Remove all &code for the deactivated format from codes at cursor
        int pos = cursorPos;
        while (pos < sb.length() - 1 && sb.charAt(pos) == '&'
                && isFormattingCode(sb.charAt(pos + 1))) {
            if (Character.toLowerCase(sb.charAt(pos + 1)) == Character.toLowerCase(code)) {
                sb.delete(pos, pos + 2);
                // don't advance — text shifted left
            } else {
                pos += 2;
            }
        }

        // Step 2: Determine active formats from BEFORE the cursor (no skip-past)
        boolean[] activeBeforeCursor = new boolean[5]; // l, o, n, m, k
        String lastColor = null;
        for (int i = 0; i < cursorPos - 1 && i < sb.length() - 1; i++) {
            if (sb.charAt(i) == '&') {
                char c = Character.toLowerCase(sb.charAt(i + 1));
                switch (c) {
                    case 'l': activeBeforeCursor[0] = true; i++; break;
                    case 'o': activeBeforeCursor[1] = true; i++; break;
                    case 'n': activeBeforeCursor[2] = true; i++; break;
                    case 'm': activeBeforeCursor[3] = true; i++; break;
                    case 'k': activeBeforeCursor[4] = true; i++; break;
                    case 'r':
                        activeBeforeCursor[0] = activeBeforeCursor[1] = activeBeforeCursor[2] =
                        activeBeforeCursor[3] = activeBeforeCursor[4] = false;
                        lastColor = null;
                        i++;
                        break;
                    default:
                        if ("0123456789abcdef".indexOf(c) >= 0) {
                            lastColor = "&" + c;
                        }
                        i++;
                        break;
                }
            }
        }

        // Step 3: If format is still active from before cursor, insert &r + re-apply
        if (activeBeforeCursor[codeIndex]) {
            StringBuilder insertion = new StringBuilder("&r");
            if (lastColor != null) {
                insertion.append(lastColor);
            }
            char[] fmtChars = {'l', 'o', 'n', 'm', 'k'};
            for (int i = 0; i < activeBeforeCursor.length; i++) {
                if (activeBeforeCursor[i] && i != codeIndex) {
                    insertion.append('&').append(fmtChars[i]);
                }
            }
            sb.insert(cursorPos, insertion);
        }

        return sb.toString();
    }

    /**
     * Scans the formatted text up to the cursor and returns the last active color code
     * as a string like "&amp;c", or null if only the default color is active.
     */
    private String getActiveColorAtCursor() {
        EditBox target = (activeLineIndex == 2) ? line2Box : line1Box;
        if (target == null) return null;

        String formatted;
        int scanUpTo;
        if (advancedMode) {
            formatted = target.getValue();
            scanUpTo = target.getCursorPosition();
        } else {
            formatted = (activeLineIndex == 2) ? line2Formatted : line1Formatted;
            scanUpTo = displayToFormattedPos(formatted, target.getCursorPosition());
        }
        if (scanUpTo > formatted.length()) scanUpTo = formatted.length();

        // Include format codes sitting right at the cursor position
        while (scanUpTo < formatted.length() - 1 && formatted.charAt(scanUpTo) == '&'
                && isFormattingCode(formatted.charAt(scanUpTo + 1))) {
            scanUpTo += 2;
        }

        String lastColor = null;
        for (int i = 0; i < scanUpTo - 1; i++) {
            if (formatted.charAt(i) == '&') {
                char code = Character.toLowerCase(formatted.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    lastColor = "&" + code;
                    i++;
                } else if (code == 'r') {
                    lastColor = null;
                    i++;
                } else {
                    i++;
                }
            }
        }
        return lastColor;
    }

    /** Returns true if the character is a valid Minecraft formatting code character */
    private static boolean isFormattingCode(char c) {
        c = Character.toLowerCase(c);
        return "0123456789abcdeflonmkr".indexOf(c) >= 0;
    }

    /** Strips all &X formatting code pairs from the text, returning only visible characters */
    private static String stripFormattingCodes(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length() && isFormattingCode(text.charAt(i + 1))) {
                i++;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** Maps a display (stripped) cursor position to the corresponding index in the formatted string */
    private static int displayToFormattedPos(String formatted, int displayPos) {
        int fi = 0;
        int di = 0;
        while (fi < formatted.length() && di < displayPos) {
            if (formatted.charAt(fi) == '&' && fi + 1 < formatted.length()
                    && isFormattingCode(formatted.charAt(fi + 1))) {
                fi += 2;
            } else {
                fi++;
                di++;
            }
        }
        return fi;
    }

    /**
     * Applies a diff between old and new display text to the underlying formatted string.
     * Preserves formatting codes that preceded deleted characters.
     */
    private String applyDisplayDiff(String oldDisplay, String newDisplay, String formatted) {
        if (oldDisplay.equals(newDisplay)) return formatted;

        int prefixLen = 0;
        int minLen = Math.min(oldDisplay.length(), newDisplay.length());
        while (prefixLen < minLen && oldDisplay.charAt(prefixLen) == newDisplay.charAt(prefixLen)) {
            prefixLen++;
        }

        int oldSuffixStart = oldDisplay.length();
        int newSuffixStart = newDisplay.length();
        while (oldSuffixStart > prefixLen && newSuffixStart > prefixLen
                && oldDisplay.charAt(oldSuffixStart - 1) == newDisplay.charAt(newSuffixStart - 1)) {
            oldSuffixStart--;
            newSuffixStart--;
        }

        int formattedStart = displayToFormattedPos(formatted, prefixLen);
        int formattedEnd = displayToFormattedPos(formatted, oldSuffixStart);

        // Preserve any formatting codes at formattedStart (codes that preceded deleted characters)
        StringBuilder preservedCodes = new StringBuilder();
        int scan = formattedStart;
        while (scan < formattedEnd && formatted.charAt(scan) == '&'
                && scan + 1 < formattedEnd && isFormattingCode(formatted.charAt(scan + 1))) {
            preservedCodes.append(formatted.charAt(scan));
            preservedCodes.append(formatted.charAt(scan + 1));
            scan += 2;
        }

        String inserted = newDisplay.substring(prefixLen, newSuffixStart);
        return formatted.substring(0, formattedStart) + preservedCodes + inserted
                + formatted.substring(formattedEnd);
    }

    /** Toggles between Simple and Advanced editing modes */
    private void toggleMode() {
        suppressResponder = true;
        if (advancedMode) {
            // Advanced -> Simple: store EditBox values as formatted, show stripped text
            line1Formatted = line1Box.getValue();
            line2Formatted = line2Box.getValue();
            String d1 = stripFormattingCodes(line1Formatted);
            String d2 = stripFormattingCodes(line2Formatted);
            line1Box.setValue(d1);
            line2Box.setValue(d2);
            previousDisplayL1 = d1;
            previousDisplayL2 = d2;
            advancedMode = false;
        } else {
            // Simple -> Advanced: show formatted strings in EditBoxes
            line1Box.setValue(line1Formatted);
            line2Box.setValue(line2Formatted);
            advancedMode = true;
        }
        com.servermanagement.gui.debug.DebugLogger.logStateChange("MotdEditorScreen", "advancedMode", !advancedMode, advancedMode);
        suppressResponder = false;
        modeToggleButton.setMessage(Component.literal(advancedMode ? "Advanced" : "Simple"));
        updateMenuMotd();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle confirmation dialog input first
        if (showingConfirmDialog) {
            if (keyCode == 256) { // Escape dismisses the dialog
                showingConfirmDialog = false;
                pendingExitAction = null;
                return true;
            }
            return true; // Consume all keys while dialog is showing
        }

        // Escape key triggers exit attempt with unsaved-changes check
        if (keyCode == 256) {
            attemptExit(super::onClose);
            return true;
        }

        // When an edit box is focused, route keys directly to it
        // instead of through AbstractContainerScreen which would close on 'E'
        if (this.line1Box != null && this.line1Box.isFocused()) {
            return this.line1Box.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        if (this.line2Box != null && this.line2Box.isFocused()) {
            return this.line2Box.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showingConfirmDialog) {
            return true; // Consume all chars while dialog is showing
        }
        if (this.line1Box != null && this.line1Box.isFocused()) {
            return this.line1Box.charTyped(codePoint, modifiers);
        }
        if (this.line2Box != null && this.line2Box.isFocused()) {
            return this.line2Box.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showingConfirmDialog) {
            // Convert raw screen-pixel coords to design-space because the
            // screen is rendered through ScalableContainerScreen's pose
            // scale. Without this the discard-confirm dialog buttons stop
            // responding at non-1.0 GUI scales.
            double designMouseX = inverseMouseX(mouseX);
            double designMouseY = inverseMouseY(mouseY);

            int dialogW = 240;
            int dialogH = 100;
            int dx = (this.width - dialogW) / 2;
            int dy = (this.height - dialogH) / 2;

            int btnW = 90;
            int btnH = 20;
            int btnY = dy + dialogH - 30;
            int cancelX = dx + dialogW / 2 - btnW - 8;
            int discardX = dx + dialogW / 2 + 8;

            // Cancel button click
            if (designMouseX >= cancelX && designMouseX < cancelX + btnW && designMouseY >= btnY && designMouseY < btnY + btnH) {
                showingConfirmDialog = false;
                pendingExitAction = null;
                return true;
            }

            // Discard & Exit button click
            if (designMouseX >= discardX && designMouseX < discardX + btnW && designMouseY >= btnY && designMouseY < btnY + btnH) {
                showingConfirmDialog = false;
                if (pendingExitAction != null) {
                    // Reset original so onClose doesn't re-trigger the dialog
                    this.originalMotdText = this.menu.getMotdText();
                    pendingExitAction.run();
                    pendingExitAction = null;
                }
                return true;
            }

            // Consume click anywhere else on the overlay
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, 0xE0101010);
        // Header bar
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + 30, 0xFF1A1A2E);
        guiGraphics.fill(this.leftPos, this.topPos + 30, this.leftPos + this.imageWidth,
                this.topPos + 31, 0xFF333333);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }

    /**
     * A small colored square button for the color palette.
     */
    private static class ColorSwatchButton extends net.minecraft.client.gui.components.AbstractWidget {
        private final int swatchColor;
        private final String colorName;
        private final Runnable onPress;

        ColorSwatchButton(int x, int y, int width, int height, int color, String name, Runnable onPress) {
            super(x, y, width, height, Component.literal(name));
            this.swatchColor = color;
            this.colorName = name;
            this.onPress = onPress;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            // Swatch fill
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, swatchColor);
            // Border (bright enough to distinguish dark swatches from background)
            int border = hovered ? 0xFFFFFFFF : 0xFF888888;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, border);
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, border);

            // Tooltip on hover
            if (hovered) {
                guiGraphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                        Component.literal(colorName), mouseX, mouseY);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.onPress.run();
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
