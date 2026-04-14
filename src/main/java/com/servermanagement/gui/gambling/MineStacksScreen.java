package com.servermanagement.gui.gambling;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.gambling.GamblingResult;
import com.servermanagement.features.gambling.games.*;
import com.servermanagement.gui.ScreenScaler;
import com.servermanagement.gui.widgets.ModernButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * MineStacks - Gambling interface
 */
public class MineStacksScreen extends AbstractContainerScreen<MineStacksMenu> {
    
    private enum GameMode {
        MENU,           // Main menu - select game
        COIN_FLIP,      // Coin flip game
        DICE_ROLL,      // Dice roll game
        SLOT_MACHINE,   // Slot machine
        ROULETTE,       // Roulette
        STATS           // Player statistics
    }
    
    private GameMode currentMode = GameMode.MENU;
    private EditBox betAmountBox;
    private String lastResult = "";
    private long resultShowTime = 0;
    private boolean useMoney = true; // true = money bet, false = item bet
    private final NumberFormat currencyFormat; // Format balance like Bank GUI
    
    // Coin flip state
    private String coinChoice = "heads";
    
    // Dice roll state
    private DiceRollGame.BetType diceType = DiceRollGame.BetType.HIGH;
    
    // Roulette state
    private RouletteGame.BetType rouletteType = RouletteGame.BetType.RED;
    
    // Tension/Animation state
    private boolean isTensionActive = false;
    private boolean isEndingAnimation = false;
    private long tensionStartTime = 0;
    private long endingStartTime = 0;
    private com.servermanagement.network.packet.GamblingTensionPacket.GameType tensionGameType = null;
    private String tensionGameOption = "";
    private float tensionRotation = 0f; // For coin/dice/wheel rotation
    private float tensionReelOffset = 0f; // For slot machine reels
    private static final long TENSION_DURATION = 3000; // 3 seconds
    private static final long ENDING_DURATION = 500; // 0.5 seconds
    
    // Pending result (stored until ending animation completes)
    private boolean hasPendingResult = false;
    private boolean pendingResultWon = false;
    private double pendingResultPayout = 0.0;
    private String pendingResultMessage = "";
    
    // Animation
    private float animationTime = 0f;
    private boolean isAnimating = false;
    private boolean lastResultWon = false;
    private float winAnimationProgress = 0f;
    private int particleCount = 0;
    private java.util.List<AnimatedParticle> particles = new java.util.ArrayList<>();
    
    // Animated particle class for win effects
    private static class AnimatedParticle {
        float x, y, velocityX, velocityY;
        int color;
        float life;
        float maxLife;
        float size;
        
        AnimatedParticle(float x, float y, float velocityX, float velocityY, int color, float maxLife) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.color = color;
            this.life = maxLife;
            this.maxLife = maxLife;
            this.size = 1.0f;
        }
        
        AnimatedParticle(float x, float y, float velocityX, float velocityY, int color, float maxLife, float size) {
            this(x, y, velocityX, velocityY, color, maxLife);
            this.size = size;
        }
        
        void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.1f; // Gravity
            life -= 0.02f;
        }
        
        boolean isAlive() {
            return life > 0;
        }
        
        int getAlphaColor() {
            int alpha = (int)(255 * (life / maxLife));
            return (alpha << 24) | (color & 0x00FFFFFF);
        }
    }
    
    public MineStacksScreen(MineStacksMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 400;
        this.imageHeight = 220;
        this.inventoryLabelY = 1000; // Hide
        this.titleLabelY = 1000; // Hide
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }
    
    @Override
    protected void init() {
        int[] dim = ScreenScaler.scale(400, 220, this.width, this.height);
        this.imageWidth = dim[0];
        this.imageHeight = dim[1];
        super.init();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        
        // Update inventory and betting slot visibility based on bet type and mode
        updateInventoryVisibility();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Dashboard button (admin only)
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2)) {
            this.addRenderableWidget(new ModernButton(
                centerX + 5, centerY + 5, 20, 18,
                Component.literal("\u2190"),
                button -> com.servermanagement.network.ModNetworking.sendToServer(
                    new com.servermanagement.network.packet.OpenGuiPacket(
                        com.servermanagement.network.packet.OpenGuiPacket.GuiType.DASHBOARD)),
                ModernButton.ButtonStyle.SECONDARY
            ));
        }

        // Close button (always visible)
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 65, centerY + 5, 60, 18,
            Component.literal("Close"),
            button -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        switch (currentMode) {
            case MENU:
                initMainMenu(centerX, centerY);
                break;
            case COIN_FLIP:
                initCoinFlip(centerX, centerY);
                break;
            case DICE_ROLL:
                initDiceRoll(centerX, centerY);
                break;
            case SLOT_MACHINE:
                initSlotMachine(centerX, centerY);
                break;
            case ROULETTE:
                initRoulette(centerX, centerY);
                break;
            case STATS:
                initStats(centerX, centerY);
                break;
        }
    }
    
    private void initMainMenu(int centerX, int centerY) {
        // Quick-nav buttons
        this.addRenderableWidget(new ModernButton(
            centerX + 5, centerY + 30, 55, 18,
            Component.literal("Bank"),
            button -> com.servermanagement.network.ModNetworking.sendToServer(
                new com.servermanagement.network.packet.OpenGuiPacket(
                    com.servermanagement.network.packet.OpenGuiPacket.GuiType.BANK)),
            ModernButton.ButtonStyle.PRIMARY
        ));
        this.addRenderableWidget(new ModernButton(
            centerX + 65, centerY + 30, 65, 18,
            Component.literal("MineBay"),
            button -> com.servermanagement.network.ModNetworking.sendToServer(
                new com.servermanagement.network.packet.OpenGuiPacket(
                    com.servermanagement.network.packet.OpenGuiPacket.GuiType.MINEBAY)),
            ModernButton.ButtonStyle.SUCCESS
        ));

        int buttonY = centerY + 55;
        int buttonWidth = (this.imageWidth - 60) / 2;
        int buttonHeight = 25;

        // Game buttons (2x2 grid)
        this.addRenderableWidget(new ModernButton(
            centerX + 20, buttonY,
            buttonWidth, buttonHeight,
            Component.literal("Coin Flip  |  2% edge"),
            button -> switchMode(GameMode.COIN_FLIP),
            ModernButton.ButtonStyle.PRIMARY
        ));

        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth / 2, buttonY,
            buttonWidth, buttonHeight,
            Component.literal("Dice Roll  |  3% edge"),
            button -> switchMode(GameMode.DICE_ROLL),
            ModernButton.ButtonStyle.PRIMARY
        ));

        this.addRenderableWidget(new ModernButton(
            centerX + 20, buttonY + 30,
            buttonWidth, buttonHeight,
            Component.literal("Slot Machine  |  5% edge"),
            button -> switchMode(GameMode.SLOT_MACHINE),
            ModernButton.ButtonStyle.PRIMARY
        ));

        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth / 2, buttonY + 30,
            buttonWidth, buttonHeight,
            Component.literal("Roulette  |  2.7% edge"),
            button -> switchMode(GameMode.ROULETTE),
            ModernButton.ButtonStyle.PRIMARY
        ));

        // Statistics button
        this.addRenderableWidget(new ModernButton(
            centerX + (this.imageWidth - buttonWidth) / 2, buttonY + 65,
            buttonWidth, buttonHeight,
            Component.literal("Your Statistics"),
            button -> switchMode(GameMode.STATS),
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void initCoinFlip(int centerX, int centerY) {
        initBettingControls(centerX, centerY);
        
        int choiceY = centerY + 90;
        
        // Choice buttons
        this.addRenderableWidget(new ModernButton(
            centerX + 20, choiceY, 80, 25,
            Component.literal("Heads"),
            button -> { coinChoice = "heads"; this.init(); },
            coinChoice.equals("heads") ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
        ));
        
        this.addRenderableWidget(new ModernButton(
            centerX + 110, choiceY, 80, 25,
            Component.literal("Tails"),
            button -> { coinChoice = "tails"; this.init(); },
            coinChoice.equals("tails") ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Play button
        this.addRenderableWidget(new ModernButton(
            centerX + 200, choiceY, 100, 25,
            Component.literal("Flip! (2x)"),
            button -> playCoinFlip(),
            ModernButton.ButtonStyle.PRIMARY
        ));
    }
    
    private void initDiceRoll(int centerX, int centerY) {
        initBettingControls(centerX, centerY);
        
        int choiceY = centerY + 85;
        int spacing = 27;
        int btnW = (this.imageWidth - 40) / 3; // 3 buttons with gaps
        int gap = 5;
        
        // Bet type buttons (row 1: 3 buttons)
        addDiceButton(centerX + 10, choiceY, "High (8-12)", DiceRollGame.BetType.HIGH, "2x", btnW);
        addDiceButton(centerX + 10 + btnW + gap, choiceY, "Low (2-6)", DiceRollGame.BetType.LOW, "2x", btnW);
        addDiceButton(centerX + 10 + (btnW + gap) * 2, choiceY, "Seven (7)", DiceRollGame.BetType.SEVEN, "5x", btnW);
        
        // Row 2: Doubles + Roll button
        addDiceButton(centerX + 10, choiceY + spacing, "Doubles", DiceRollGame.BetType.DOUBLES, "6x", btnW);
        
        int rollWidth = btnW * 2 + gap;
        this.addRenderableWidget(new ModernButton(
            centerX + 10 + btnW + gap, choiceY + spacing, rollWidth, 22,
            Component.literal("Roll!!"),
            button -> playDiceRoll(),
            ModernButton.ButtonStyle.PRIMARY
        ));
    }
    
    private void addDiceButton(int x, int y, String label, DiceRollGame.BetType type, String payout, int width) {
        this.addRenderableWidget(new ModernButton(
            x, y, width, 22,
            Component.literal(label + " " + payout),
            button -> { diceType = type; this.init(); },
            diceType == type ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    private void initSlotMachine(int centerX, int centerY) {
        initBettingControls(centerX, centerY);
        
        // Play button
        this.addRenderableWidget(new ModernButton(
            centerX + (this.imageWidth - 160) / 2, centerY + 95, 160, 30,
            Component.literal("SPIN!"),
            button -> playSlotMachine(),
            ModernButton.ButtonStyle.PRIMARY
        ));
    }
    
    private void initRoulette(int centerX, int centerY) {
        initBettingControls(centerX, centerY);
        
        int choiceY = centerY + 85;
        int spacing = 27;
        
        // Bet type buttons
        int rbtnW = (this.imageWidth - 50) / 4;
        addRouletteButton(centerX + 10, choiceY, "Red", RouletteGame.BetType.RED, "2x");
        addRouletteButton(centerX + 10 + rbtnW + 10, choiceY, "Black", RouletteGame.BetType.BLACK, "2x");
        addRouletteButton(centerX + 10 + (rbtnW + 10) * 2, choiceY, "Even", RouletteGame.BetType.EVEN, "2x");
        addRouletteButton(centerX + this.imageWidth - rbtnW - 10, choiceY, "Odd", RouletteGame.BetType.ODD, "2x");
        
        addRouletteButton(centerX + 10, choiceY + spacing, "Low (1-18)", RouletteGame.BetType.LOW, "2x");
        addRouletteButton(centerX + this.imageWidth / 2 - 60, choiceY + spacing, "High (19-36)", RouletteGame.BetType.HIGH, "2x");
        addRouletteButton(centerX + this.imageWidth - 130, choiceY + spacing, "Spin!", null, "");
    }
    
    private void addRouletteButton(int x, int y, String label, RouletteGame.BetType type, String payout) {
        if (type == null) {
            // Play button
            this.addRenderableWidget(new ModernButton(
                x, y, 110, 22,
                Component.literal(label),
                button -> playRoulette(),
                ModernButton.ButtonStyle.PRIMARY
            ));
        } else {
            this.addRenderableWidget(new ModernButton(
                x, y, 80, 22,
                Component.literal(label + " " + payout),
                button -> { rouletteType = type; this.init(); },
                rouletteType == type ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.SECONDARY
            ));
        }
    }
    
    /**
     * Update inventory and hotbar visibility based on useMoney flag
     */
    private void updateInventoryVisibility() {
        // Show inventory only when:
        // 1. In a game mode (not MENU or STATS)
        // 2. AND using items for betting (not money)
        boolean shouldShowInventory = (currentMode != GameMode.MENU && currentMode != GameMode.STATS) && !useMoney;
        this.inventoryLabelY = shouldShowInventory ? (this.imageHeight - 94) : 1000;
        
        // Update betting slot visibility on CLIENT menu immediately (no latency)
        this.menu.setBettingSlotActive(shouldShowInventory);
        
        // ALSO sync slot state to server so it can validate item placement
        // This prevents race conditions where client tries to place item before server gets update
        com.servermanagement.network.ModNetworking.sendToServer(
            new com.servermanagement.network.packet.SyncBettingSlotStatePacket(shouldShowInventory)
        );
    }
    
    private void initBettingControls(int centerX, int centerY) {
        // Back button - moved to avoid overlap with title
        this.addRenderableWidget(new ModernButton(
            centerX + 5, centerY + 30, 60, 18,
            Component.literal("← Back"),
            button -> switchMode(GameMode.MENU),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Bet type toggle - positioned lower to avoid overlap with betting slot
        int toggleY = centerY + 55;
        this.addRenderableWidget(new ModernButton(
            centerX + 10, toggleY, 100, 22,
            Component.literal(useMoney ? "$ Money" : "Item Bet"),
            button -> { useMoney = !useMoney; this.rebuildWidgets(); },
            useMoney ? ModernButton.ButtonStyle.SUCCESS : ModernButton.ButtonStyle.PRIMARY
        ));
        
        if (useMoney) {
            // Bet amount input
            if (betAmountBox == null) {
                betAmountBox = new EditBox(this.font, centerX + 120, toggleY + 2, 100, 18, 
                    Component.literal("Bet Amount"));
                betAmountBox.setMaxLength(10);
                betAmountBox.setValue("100");
                betAmountBox.setFilter(s -> s.matches("\\d*"));
            } else {
                betAmountBox.setPosition(centerX + 120, toggleY + 2);
            }
            this.addRenderableWidget(betAmountBox);
        }
        // Item betting slot position is set in MineStacksMenu constructor
    }
    
    private void initStats(int centerX, int centerY) {
        // Back button - moved to avoid overlap with title
        this.addRenderableWidget(new ModernButton(
            centerX + 5, centerY + 30, 60, 18,
            Component.literal("← Back"),
            button -> switchMode(GameMode.MENU),
            ModernButton.ButtonStyle.SECONDARY
        ));
    }
    
    // Game play methods
    private void playCoinFlip() {
        if (!validateBet()) return;
        sendBetToServer(com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType.COIN_FLIP, coinChoice);
    }
    
    private void playDiceRoll() {
        if (!validateBet()) return;
        sendBetToServer(com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType.DICE_ROLL, diceType.name());
    }
    
    private void playSlotMachine() {
        if (!validateBet()) return;
        sendBetToServer(com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType.SLOT_MACHINE, "");
    }
    
    private void playRoulette() {
        if (!validateBet()) return;
        String option = rouletteType.name();
        sendBetToServer(com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType.ROULETTE, option);
    }
    
    private boolean validateBet() {
        if (useMoney) {
            if (betAmountBox == null || betAmountBox.getValue().isEmpty()) {
                lastResult = "§cEnter a bet amount!";
                resultShowTime = System.currentTimeMillis();
                return false;
            }
            
            try {
                double amount = Double.parseDouble(betAmountBox.getValue());
                if (amount < 10.0) {
                    lastResult = "§cMinimum bet is $10";
                    resultShowTime = System.currentTimeMillis();
                    return false;
                }
            } catch (NumberFormatException e) {
                lastResult = "§cInvalid bet amount!";
                resultShowTime = System.currentTimeMillis();
                return false;
            }
        } else {
            ItemStack bettingItem = menu.getBettingItem();
            if (bettingItem.isEmpty()) {
                lastResult = "§cPlace an item in the slot!";
                resultShowTime = System.currentTimeMillis();
                return false;
            }
        }
        return true;
    }
    
    private void sendBetToServer(com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType gameType, String gameOption) {
        // Play anticipation sound
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(
                net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                0.7f,
                1.2f
            );
            
            // Extra sound for slot machine
            if (gameType == com.servermanagement.network.packet.PlaceGamblingBetPacket.GameType.SLOT_MACHINE) {
                this.minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.DISPENSER_DISPENSE,
                    0.5f,
                    0.8f
                );
            }
        }
        
        if (useMoney) {
            double amount = Double.parseDouble(betAmountBox.getValue());
            com.servermanagement.network.ModNetworking.sendToServer(
                new com.servermanagement.network.packet.PlaceGamblingBetPacket(gameType, amount, gameOption)
            );
        } else {
            com.servermanagement.network.ModNetworking.sendToServer(
                new com.servermanagement.network.packet.PlaceGamblingBetWithItemPacket(gameType, gameOption)
            );
        }
        
        // Show pending message
        lastResult = "§ePlacing bet...";
        resultShowTime = System.currentTimeMillis();
    }
    
    /**
     * Called by GamblingResultPacket when server sends result
     */
    public void handleGamblingResult(boolean won, double payout, String message) {
        // Store result to show after ending animation completes
        hasPendingResult = true;
        pendingResultWon = won;
        pendingResultPayout = payout;
        pendingResultMessage = message;
        
        // Don't show result immediately - wait for ending animation
    }
    
    /**
     * Shows the pending result after ending animation completes
     */
    private void showPendingResult() {
        if (!hasPendingResult) return;
        
        lastResult = pendingResultWon ? "§a" + pendingResultMessage : "§c" + pendingResultMessage;
        lastResultWon = pendingResultWon;
        resultShowTime = System.currentTimeMillis();
        
        // Start win/loss animation
        isAnimating = true;
        winAnimationProgress = 0f;
        
        // Play sound effect
        if (this.minecraft != null && this.minecraft.player != null) {
            if (pendingResultWon) {
                // Big win sound (clamp pitch to valid range)
                this.minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    1.0f, 
                    Math.min(2.0f, 1.0f + (float)(pendingResultPayout / 1000.0))
                );
                
                // Extra celebration for big wins
                if (pendingResultPayout >= 500) {
                    this.minecraft.player.playSound(
                        net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                        0.8f,
                        1.2f
                    );
                }
                
                // Create particle explosion
                createWinParticles(pendingResultPayout);
            } else {
                // Loss sound
                this.minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                    0.5f,
                    0.8f
                );
            }
        }
        
        // Clear bet after play
        if (!useMoney) {
            menu.clearBettingSlot();
        }
        
        // Clear pending result
        hasPendingResult = false;
    }
    
    /**
     * Create particle effects for wins
     */
    private void createWinParticles(double payout) {
        particles.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Number of particles scales with payout
        int count = Math.min(50, 10 + (int)(payout / 20));
        
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.PI * 2 * i / count);
            float speed = 2f + random.nextFloat() * 3f;
            float velocityX = (float)Math.cos(angle) * speed;
            float velocityY = (float)Math.sin(angle) * speed - 2f; // Shoot upward
            
            // Rainbow particle colors based on angle for variety
            float hue = (angle / (float)Math.PI * 180) + (animationTime * 50) % 360;
            int color = hsvToRgb(hue, 0.8f + random.nextFloat() * 0.2f, 1.0f, 1.0f);
            
            float particleSize = 1.0f + random.nextFloat() * 0.5f; // Vary size
            
            particles.add(new AnimatedParticle(
                centerX, centerY,
                velocityX, velocityY,
                color,
                1.0f + random.nextFloat() * 0.5f, // Life
                particleSize
            ));
        }
    }
    
    /**
     * Called by GamblingTensionPacket to start the tension animation
     */
    public void startTension(com.servermanagement.network.packet.GamblingTensionPacket.GameType gameType, String gameOption) {
        this.isTensionActive = true;
        this.tensionStartTime = System.currentTimeMillis();
        this.tensionGameType = gameType;
        this.tensionGameOption = gameOption;
        this.tensionRotation = 0f;
        this.tensionReelOffset = 0f;
        
        // Play tension sound
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                0.5f,
                0.8f
            );
        }
    }
    
    private GamblingResult executeBet(com.servermanagement.features.gambling.GamblingGame game) {
        // Deprecated - now using packet system
        // This method is kept for backwards compatibility but should not be called
        return new GamblingResult(false, 0.0, "Use packet system");
    }
    
    private void switchMode(GameMode mode) {
        currentMode = mode;
        lastResult = "";
        this.rebuildWidgets();
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Rainbow outline effect when animating win
        if (isAnimating && lastResultWon) {
            float hue = (animationTime * 100) % 360;
            int rainbowColor = hsvToRgb(hue, 1.0f, 1.0f, 0.8f);
            
            // Multiple rainbow layers for glow effect
            for (int i = 0; i < 4; i++) {
                int offset = i + 2;
                float layerHue = (hue + i * 30) % 360;
                int layerColor = hsvToRgb(layerHue, 1.0f, 1.0f, 0.3f - i * 0.05f);
                guiGraphics.fill(centerX - offset, centerY - offset, 
                    centerX + this.imageWidth + offset, centerY + this.imageHeight + offset, layerColor);
            }
        }
        
        // Main background with border
        guiGraphics.fill(centerX - 2, centerY - 2, centerX + this.imageWidth + 2, 
            centerY + this.imageHeight + 2, 0xFF000000);
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, 
            centerY + this.imageHeight, 0xE0101010);
        
        // Header bar + separator
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 28, 0xE0202020);
        guiGraphics.fill(centerX, centerY + 28, centerX + this.imageWidth, centerY + 29, 0xFF333333);
    }
    
    /**
     * Convert HSV to RGB with alpha
     */
    private int hsvToRgb(float hue, float saturation, float value, float alpha) {
        hue = ((hue % 360) + 360) % 360; // Normalize hue to [0, 360)
        int h = (int)(hue / 60) % 6;
        float f = hue / 60 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);
        
        float r, g, b;
        switch (h) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            case 5: r = value; g = p; b = q; break;
            default: r = g = b = 0;
        }
        
        int a = (int)(alpha * 255);
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        
        return (a << 24) | (ri << 16) | (gi << 8) | bi;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Update tension animation
        if (isTensionActive) {
            long elapsed = System.currentTimeMillis() - tensionStartTime;
            
            // Check if tension phase is over, start ending animation
            if (elapsed >= TENSION_DURATION && !isEndingAnimation) {
                isTensionActive = false;
                isEndingAnimation = true;
                endingStartTime = System.currentTimeMillis();
            }
            
            float progress = elapsed / (float)TENSION_DURATION;
            
            // Update rotation for spinning animations
            tensionRotation += partialTick * 20f; // Fast spin
            if (tensionRotation > 360f) tensionRotation -= 360f;
            
            // Update reel offset for slot machine
            tensionReelOffset += partialTick * 10f;
            if (tensionReelOffset > 32f) tensionReelOffset -= 32f; // Reset every 32 pixels
        }
        
        // Update ending animation
        if (isEndingAnimation) {
            long elapsed = System.currentTimeMillis() - endingStartTime;
            float progress = elapsed / (float)ENDING_DURATION;
            
            if (elapsed >= ENDING_DURATION) {
                // Ending animation complete, show the result now
                isEndingAnimation = false;
                
                // Check if we have a pending result to show
                if (hasPendingResult) {
                    showPendingResult();
                }
            } else {
                // Slow down rotation for ending effect
                float slowdownFactor = 1.0f - progress; // Gradually slow to 0
                tensionRotation += partialTick * 20f * slowdownFactor;
                if (tensionRotation > 360f) tensionRotation -= 360f;
                
                // Slow down reels
                tensionReelOffset += partialTick * 10f * slowdownFactor;
                if (tensionReelOffset > 32f) tensionReelOffset -= 32f;
            }
        }
        
        // Update animation
        if (isAnimating) {
            winAnimationProgress += partialTick * 0.02f;
            if (winAnimationProgress >= 1.0f) {
                isAnimating = false;
                winAnimationProgress = 0f;
            }
        }
        
        // Update particles
        particles.removeIf(p -> !p.isAlive());
        for (AnimatedParticle particle : particles) {
            particle.update();
        }
        
        // Apply screen shake for wins
        int shakeX = 0;
        int shakeY = 0;
        if (isAnimating && lastResultWon) {
            float shakeIntensity = (1.0f - winAnimationProgress) * 3f;
            shakeX = (int)(Math.sin(winAnimationProgress * 20) * shakeIntensity);
            shakeY = (int)(Math.cos(winAnimationProgress * 15) * shakeIntensity);
        }
        
        // Render background first
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Save matrix state and apply shake
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(shakeX, shakeY, 0);
        
        // Render custom BG
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        // Skip slot/item/widget rendering during animation overlays to prevent z-bleed
        if (!isTensionActive && !isEndingAnimation) {
            // Render only betting slot (index 0)
            net.minecraft.world.inventory.Slot bettingSlot = this.menu.slots.get(0);
            if (bettingSlot.isActive()) {
                int slotX = this.leftPos + bettingSlot.x;
                int slotY = this.topPos + bettingSlot.y;
                
                // Render slot background with glow effect when animating
                if (isAnimating && lastResultWon) {
                    float pulseSize = 2f + (float)Math.sin(winAnimationProgress * 10) * 2f;
                    int glowColor = 0x40FFD700; // Translucent gold
                    guiGraphics.fill(
                        (int)(slotX - pulseSize), 
                        (int)(slotY - pulseSize), 
                        (int)(slotX + 18 + pulseSize), 
                        (int)(slotY + 18 + pulseSize), 
                        glowColor
                    );
                }
                
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737);
                
                // Render item in slot if present
                if (bettingSlot.hasItem()) {
                    guiGraphics.renderItem(bettingSlot.getItem(), slotX + 1, slotY + 1);
                    guiGraphics.renderItemDecorations(this.font, bettingSlot.getItem(), slotX + 1, slotY + 1);
                }
                
                // Add label below the betting slot to indicate its purpose
                Component betLabel = Component.literal("Bet Item");
                int betLabelW = this.font.width(betLabel);
                guiGraphics.drawString(this.font, betLabel,
                    slotX + 9 - betLabelW / 2, slotY + 20, 0xFFAA00, true);
            }
            
            // Render inventory slots only when in game mode AND using items
            boolean shouldShowInventory = (currentMode != GameMode.MENU && currentMode != GameMode.STATS) && !useMoney;
            if (shouldShowInventory) {
                for (int i = 1; i < this.menu.slots.size(); i++) {
                    net.minecraft.world.inventory.Slot slot = this.menu.slots.get(i);
                    int slotX = this.leftPos + slot.x;
                    int slotY = this.topPos + slot.y;
                    
                    // Render slot background
                    guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                    guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737);
                    
                    // Render item in slot if present
                    if (slot.hasItem()) {
                        guiGraphics.renderItem(slot.getItem(), slotX + 1, slotY + 1);
                        guiGraphics.renderItemDecorations(this.font, slot.getItem(), slotX + 1, slotY + 1);
                    }
                }
            }
            
            // Render widgets (buttons, etc.)
            for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            
            // Update hoveredSlot for slot highlighting and tooltip support
            // (since we don't call super.render(), AbstractContainerScreen never sets this)
            this.hoveredSlot = null;
            for (int i = 0; i < this.menu.slots.size(); i++) {
                net.minecraft.world.inventory.Slot slot = this.menu.slots.get(i);
                if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    this.hoveredSlot = slot;
                    // Draw hover highlight centered on the 16x16 item area (inside the 18x18 slot border)
                    int hx = this.leftPos + slot.x + 1;
                    int hy = this.topPos + slot.y + 1;
                    guiGraphics.fill(hx, hy, hx + 16, hy + 16, 0x80FFFFFF);
                    break;
                }
            }
        }
        
        guiGraphics.pose().popPose();
        
        animationTime += partialTick * 0.05f;
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Skip rendering UI text/content when animation overlay is active to prevent z-bleed
        if (!isTensionActive && !isEndingAnimation) {
            // Title (centered in header)
            String title = currentMode == GameMode.MENU ? "MineStacks Casino" : getModeTitle();
            guiGraphics.drawCenteredString(this.font, Component.literal(title),
                centerX + this.imageWidth / 2, centerY + 10, 0xFFD700);
            
            // Balance display - positioned top-left, after admin dashboard button if present
            if (minecraft != null && minecraft.player != null) {
                double balance = menu.getPlayerBalance();
                // Fallback to ClientBankData if menu balance is 0 (shouldn't happen, but defensive)
                if (balance == 0.0) {
                    balance = com.servermanagement.client.ClientBankData.getBalance();
                }
                String balanceStr = "Balance: " + currencyFormat.format(balance);
                int balanceColor = balance >= 0 ? 0x55FF55 : 0xFF5555;
                // Position after admin dashboard button (20+5 = 25px) if admin, else at left edge
                boolean isAdmin = minecraft.player.hasPermissions(2);
                int balanceX = centerX + (isAdmin ? 30 : 5);
                guiGraphics.drawString(this.font, Component.literal(balanceStr),
                    balanceX, centerY + 10, balanceColor, true);
            }
            
            // Render mode-specific content
            switch (currentMode) {
                case MENU:
                    renderMainMenu(guiGraphics, centerX, centerY);
                    break;
                case STATS:
                    renderStats(guiGraphics, centerX, centerY);
                    break;
                default:
                    renderGameInfo(guiGraphics, centerX, centerY);
                    break;
            }
            
            // Render particles on top of everything
            for (AnimatedParticle particle : particles) {
                int size = (int)(particle.size * 3); // Use variable size from particle
                guiGraphics.fill(
                    (int)particle.x - size/2, 
                    (int)particle.y - size/2, 
                    (int)particle.x + size/2, 
                    (int)particle.y + size/2, 
                    particle.getAlphaColor()
                );
            }
        }
        
        // Show result message with enhanced animation (skip during animation overlays)
        if (!isTensionActive && !isEndingAnimation && !lastResult.isEmpty() && System.currentTimeMillis() - resultShowTime < 5000) {
            long elapsed = System.currentTimeMillis() - resultShowTime;
            float messageAlpha = Math.min(1.0f, elapsed / 300f); // Fade in over 300ms
            
            // Pulse effect for wins
            float scale = 1.0f;
            if (lastResultWon && elapsed < 1000) {
                scale = 1.0f + (float)Math.sin(elapsed / 100.0) * 0.1f;
            }
            
            // Draw with scale and glow
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(
                centerX + this.imageWidth / 2, 
                centerY + 180, 
                0
            );
            guiGraphics.pose().scale(scale, scale, 1.0f);
            
            // Glow effect for wins
            if (lastResultWon) {
                for (int i = 0; i < 3; i++) {
                    int offset = (i + 1) * 2;
                    guiGraphics.drawCenteredString(this.font, Component.literal(lastResult),
                        0, 0, 0x40FFD700);
                }
            }
            
            // Main text
            guiGraphics.drawCenteredString(this.font, Component.literal(lastResult),
                0, 0, 0xFFFFFFFF);
            
            guiGraphics.pose().popPose();
        }
        
        // Render tension animation overlay
        if (isTensionActive) {
            renderTensionAnimation(guiGraphics, centerX, centerY, partialTick);
        }
        
        // Render ending animation overlay
        if (isEndingAnimation) {
            renderEndingAnimation(guiGraphics, centerX, centerY, partialTick);
        }
        
        // Render dragged item on top of everything else
        this.renderFloatingItem(guiGraphics, mouseX, mouseY);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Renders the tension animation overlay based on game type
     */
    private void renderTensionAnimation(GuiGraphics guiGraphics, int centerX, int centerY, float partialTick) {
        long elapsed = System.currentTimeMillis() - tensionStartTime;
        float progress = elapsed / (float)TENSION_DURATION;
        
        // Elevate z-level above all widget text
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        
        // Dark overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        int animX = centerX + this.imageWidth / 2;
        int animY = centerY + 100;
        
        // Draw tension text
        guiGraphics.drawCenteredString(this.font,
            Component.literal("§6Rolling..."),
            animX, animY - 40, 0xFFD700);
        
        // Cache locally to avoid null between check and switch
        com.servermanagement.network.packet.GamblingTensionPacket.GameType gameType = this.tensionGameType;
        if (gameType != null) {
            switch (gameType) {
                case COIN_FLIP:
                    renderCoinFlipTension(guiGraphics, animX, animY, progress);
                    break;
                case DICE_ROLL:
                    renderDiceRollTension(guiGraphics, animX, animY, progress);
                    break;
                case SLOT_MACHINE:
                    renderSlotMachineTension(guiGraphics, animX, animY, progress);
                    break;
                case ROULETTE:
                    renderRouletteTension(guiGraphics, animX, animY, progress);
                    break;
            }
        }
        
        guiGraphics.pose().popPose();
    }
    
    private void renderCoinFlipTension(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent rotation overflow
        guiGraphics.enableScissor(x - 30, y - 30, x + 30, y + 30);
        
        // Spinning coin effect
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tensionRotation));
        
        // Draw coin as a rectangle that appears to flip
        float scale = Math.abs((float)Math.cos(Math.toRadians(tensionRotation)));
        int coinWidth = (int)(40 * scale);
        int coinHeight = 40;
        
        // Gold coin color
        guiGraphics.fill(-coinWidth/2, -coinHeight/2, coinWidth/2, coinHeight/2, 0xFFFFD700);
        guiGraphics.fill(-coinWidth/2 + 2, -coinHeight/2 + 2, coinWidth/2 - 2, coinHeight/2 - 2, 0xFFFFA500);
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        // Draw "FLIPPING..." text below (outside scissor)
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Flipping..."),
            x, y + 35, 0xFFFFFF);
    }
    
    private void renderDiceRollTension(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent rotation overflow
        guiGraphics.enableScissor(x - 25, y - 25, x + 25, y + 25);
        
        // Tumbling dice effect
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tensionRotation));
        
        // Draw dice
        int diceSize = 30;
        guiGraphics.fill(-diceSize/2, -diceSize/2, diceSize/2, diceSize/2, 0xFFFFFFFF);
        guiGraphics.fill(-diceSize/2 + 2, -diceSize/2 + 2, diceSize/2 - 2, diceSize/2 - 2, 0xFFFF0000);
        
        // Draw random dots
        int dotCount = 1 + ((int)(tensionRotation / 60) % 6);
        for (int i = 0; i < dotCount; i++) {
            int dotX = -10 + (i % 3) * 10;
            int dotY = -10 + (i / 3) * 10;
            guiGraphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFF000000);
        }
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Rolling..."),
            x, y + 35, 0xFFFFFF);
    }
    
    private void renderSlotMachineTension(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Spinning reels
        int reelWidth = 40;
        int reelHeight = 60;
        int reelSpacing = 10;
        
        String[] symbols = {"§c♥", "§b♦", "§6★", "§a7", "§e☀"};
        
        for (int i = 0; i < 3; i++) {
            int reelX = x - (reelWidth + reelSpacing) + i * (reelWidth + reelSpacing);
            
            // Reel background
            guiGraphics.fill(reelX - reelWidth/2, y - reelHeight/2, 
                           reelX + reelWidth/2, y + reelHeight/2, 0xFF333333);
            guiGraphics.fill(reelX - reelWidth/2 + 2, y - reelHeight/2 + 2, 
                           reelX + reelWidth/2 - 2, y + reelHeight/2 - 2, 0xFF111111);
            
            // Scissor clip each reel to prevent symbol overflow
            guiGraphics.enableScissor(reelX - reelWidth/2 + 2, y - reelHeight/2 + 2,
                                      reelX + reelWidth/2 - 2, y + reelHeight/2 - 2);
            
            // Spinning symbols
            int offset = (int)tensionReelOffset + i * 10;
            for (int j = -1; j <= 1; j++) {
                int symbolIndex = ((offset / 16) + j + 100) % symbols.length;
                int symbolY = y + (j * 20) - (offset % 16);
                
                guiGraphics.drawCenteredString(this.font,
                    Component.literal(symbols[symbolIndex]),
                    reelX, symbolY - 4, 0xFFFFFF);
            }
            
            guiGraphics.disableScissor();
        }
        
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Spinning..."),
            x, y + 45, 0xFFFFFF);
    }
    
    private void renderRouletteTension(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent wheel overflow
        guiGraphics.enableScissor(x - 45, y - 45, x + 45, y + 45);
        
        // Spinning roulette wheel
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        
        // Outer wheel
        int wheelRadius = 35;
        for (int i = 0; i < 36; i++) {
            float angle = (tensionRotation + i * 10) % 360;
            float rad = (float)Math.toRadians(angle);
            
            int segmentColor = (i % 2 == 0) ? 0xFFFF0000 : 0xFF000000;
            
            // Draw segment
            int x1 = (int)(Math.cos(rad) * wheelRadius);
            int y1 = (int)(Math.sin(rad) * wheelRadius);
            int x2 = (int)(Math.cos(rad + Math.toRadians(10)) * wheelRadius);
            int y2 = (int)(Math.sin(rad + Math.toRadians(10)) * wheelRadius);
            
            // Simple triangle approximation
            guiGraphics.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1,
                           Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, segmentColor);
        }
        
        // Center circle
        guiGraphics.fill(-5, -5, 5, 5, 0xFFFFD700);
        
        // Ball indicator at top
        guiGraphics.fill(-3, -wheelRadius - 5, 3, -wheelRadius + 5, 0xFFFFFFFF);
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Spinning..."),
            x, y + 50, 0xFFFFFF);
    }
    
    /**
     * Renders the ending animation (deceleration and landing)
     */
    private void renderEndingAnimation(GuiGraphics guiGraphics, int centerX, int centerY, float partialTick) {
        long elapsed = System.currentTimeMillis() - endingStartTime;
        float progress = elapsed / (float)ENDING_DURATION; // 0.0 to 1.0
        
        // Elevate z-level above all widget text
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
        
        // Dark overlay (slightly lighter than tension)
        guiGraphics.fill(0, 0, this.width, this.height, 0x60000000);
        
        int animX = centerX + this.imageWidth / 2;
        int animY = centerY + 100;
        
        // Cache locally to avoid null between check and switch
        com.servermanagement.network.packet.GamblingTensionPacket.GameType gameType = this.tensionGameType;
        if (gameType != null) {
            switch (gameType) {
                case COIN_FLIP:
                    renderCoinFlipEnding(guiGraphics, animX, animY, progress);
                    break;
                case DICE_ROLL:
                    renderDiceRollEnding(guiGraphics, animX, animY, progress);
                    break;
                case SLOT_MACHINE:
                    renderSlotMachineEnding(guiGraphics, animX, animY, progress);
                    break;
                case ROULETTE:
                    renderRouletteEnding(guiGraphics, animX, animY, progress);
                    break;
            }
        }
        
        guiGraphics.pose().popPose();
    }
    
    private void renderCoinFlipEnding(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent rotation overflow
        guiGraphics.enableScissor(x - 30, y - 30, x + 30, y + 30);
        
        // Coin slowing down and landing flat
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        
        // Slow rotation based on progress
        float rotation = tensionRotation * (1.0f - progress);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        
        // Draw coin getting flatter (less 3D effect as it lands)
        float scale = Math.abs((float)Math.cos(Math.toRadians(rotation)));
        scale = scale * (1.0f - progress * 0.5f) + progress * 0.5f; // Approach 1.0 (flat)
        int coinWidth = (int)(40 * scale);
        int coinHeight = 40;
        
        // Gold coin
        guiGraphics.fill(-coinWidth/2, -coinHeight/2, coinWidth/2, coinHeight/2, 0xFFFFD700);
        guiGraphics.fill(-coinWidth/2 + 2, -coinHeight/2 + 2, coinWidth/2 - 2, coinHeight/2 - 2, 0xFFFFA500);
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        // Fade text (outside scissor)
        int alpha = (int)(255 * (1.0f - progress));
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Landing..."),
            x, y + 35, 0xFFFFFF | (alpha << 24));
    }
    
    private void renderDiceRollEnding(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent rotation overflow
        guiGraphics.enableScissor(x - 25, y - 25, x + 25, y + 25);
        
        // Dice slowing and landing
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        
        // Slow rotation
        float rotation = tensionRotation * (1.0f - progress);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        
        // Draw dice
        int diceSize = 30;
        guiGraphics.fill(-diceSize/2, -diceSize/2, diceSize/2, diceSize/2, 0xFFFFFFFF);
        guiGraphics.fill(-diceSize/2 + 2, -diceSize/2 + 2, diceSize/2 - 2, diceSize/2 - 2, 0xFFFF0000);
        
        // Final dots appearing
        int finalDots = 6; // Will be replaced with actual result
        for (int i = 0; i < finalDots && progress > 0.5f; i++) {
            int dotX = -10 + (i % 3) * 10;
            int dotY = -10 + (i / 3) * 10;
            int dotAlpha = (int)(255 * (progress - 0.5f) * 2);
            guiGraphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFF000000 | (dotAlpha << 24));
        }
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        int alpha = (int)(255 * (1.0f - progress));
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Stopping..."),
            x, y + 35, 0xFFFFFF | (alpha << 24));
    }
    
    private void renderSlotMachineEnding(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Reels decelerating
        int reelWidth = 40;
        int reelHeight = 60;
        int reelSpacing = 10;
        
        String[] symbols = {"§c♥", "§b♦", "§6★", "§a7", "§e☀"};
        
        for (int i = 0; i < 3; i++) {
            int reelX = x - (reelWidth + reelSpacing) + i * (reelWidth + reelSpacing);
            
            // Reel background
            guiGraphics.fill(reelX - reelWidth/2, y - reelHeight/2, 
                           reelX + reelWidth/2, y + reelHeight/2, 0xFF333333);
            guiGraphics.fill(reelX - reelWidth/2 + 2, y - reelHeight/2 + 2, 
                           reelX + reelWidth/2 - 2, y + reelHeight/2 - 2, 0xFF111111);
            
            // Scissor clip each reel to prevent symbol overflow
            guiGraphics.enableScissor(reelX - reelWidth/2 + 2, y - reelHeight/2 + 2,
                                      reelX + reelWidth/2 - 2, y + reelHeight/2 - 2);
            
            // Slowing symbols - each reel stops at different times
            float reelProgress = Math.min(1.0f, progress + i * 0.2f);
            int offset = (int)(tensionReelOffset * (1.0f - reelProgress)) + i * 10;
            
            for (int j = -1; j <= 1; j++) {
                int symbolIndex = ((offset / 16) + j + 100) % symbols.length;
                int symbolY = y + (j * 20) - (offset % 16);
                
                guiGraphics.drawCenteredString(this.font,
                    Component.literal(symbols[symbolIndex]),
                    reelX, symbolY - 4, 0xFFFFFF);
            }
            
            guiGraphics.disableScissor();
        }
        
        int alpha = (int)(255 * (1.0f - progress));
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Stopping..."),
            x, y + 45, 0xFFFFFF | (alpha << 24));
    }
    
    private void renderRouletteEnding(GuiGraphics guiGraphics, int x, int y, float progress) {
        // Scissor clip to prevent wheel overflow
        guiGraphics.enableScissor(x - 45, y - 45, x + 45, y + 45);
        
        // Wheel slowing, ball settling
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        
        // Slow rotation
        float rotation = tensionRotation * (1.0f - progress * 0.8f); // Slower deceleration
        
        // Outer wheel
        int wheelRadius = 35;
        for (int i = 0; i < 36; i++) {
            float angle = (rotation + i * 10) % 360;
            float rad = (float)Math.toRadians(angle);
            
            int segmentColor = (i % 2 == 0) ? 0xFFFF0000 : 0xFF000000;
            
            // Draw segment
            int x1 = (int)(Math.cos(rad) * wheelRadius);
            int y1 = (int)(Math.sin(rad) * wheelRadius);
            int x2 = (int)(Math.cos(rad + Math.toRadians(10)) * wheelRadius);
            int y2 = (int)(Math.sin(rad + Math.toRadians(10)) * wheelRadius);
            
            guiGraphics.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1,
                           Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, segmentColor);
        }
        
        // Center circle
        guiGraphics.fill(-5, -5, 5, 5, 0xFFFFD700);
        
        // Ball moving toward a segment
        float ballAngle = progress * 20f; // Ball moves slower than wheel
        int ballRadius = (int)(wheelRadius - progress * 10); // Ball moves inward
        int ballX = (int)(Math.cos(Math.toRadians(ballAngle)) * ballRadius);
        int ballY = (int)(Math.sin(Math.toRadians(ballAngle)) * ballRadius);
        guiGraphics.fill(ballX - 3, ballY - 3, ballX + 3, ballY + 3, 0xFFFFFFFF);
        
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        
        int alpha = (int)(255 * (1.0f - progress));
        guiGraphics.drawCenteredString(this.font,
            Component.literal("Settling..."),
            x, y + 50, 0xFFFFFF | (alpha << 24));
    }
    
    private void renderMainMenu(GuiGraphics guiGraphics, int centerX, int centerY) {
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("Select a game to play"),
            centerX + this.imageWidth / 2, centerY + 155, 0xAAAAAA);
        
        guiGraphics.drawCenteredString(this.font, 
            Component.literal("Min bet: $10 | Max bet: $10,000"),
            centerX + this.imageWidth / 2, centerY + 168, 0x888888);
    }
    
    private void renderGameInfo(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (!useMoney) {
            ItemStack bettingItem = menu.getBettingItem();
            if (!bettingItem.isEmpty()) {
                // Use economy engine market price instead of hardcoded ItemValuation
                double marketPrice = com.servermanagement.client.ClientMarketData.getStackPrice(bettingItem);
                String value = String.format("$%.2f", marketPrice);
                // Show below the centered betting slot
                net.minecraft.world.inventory.Slot betSlot = this.menu.slots.get(0);
                int labelX = this.leftPos + betSlot.x + 9;
                guiGraphics.drawCenteredString(this.font, 
                    Component.literal("Value: " + value),
                    labelX, this.topPos + betSlot.y + 34, 0xFFAA00);
            }
        }
    }
    
    private void renderStats(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (minecraft == null || minecraft.player == null) return;
        
        // Read stats from client-side storage (synced from server)
        long totalBets = com.servermanagement.client.ClientGamblingData.getTotalBets();
        long totalWins = com.servermanagement.client.ClientGamblingData.getTotalWins();
        long totalLosses = com.servermanagement.client.ClientGamblingData.getTotalLosses();
        double totalWagered = com.servermanagement.client.ClientGamblingData.getTotalWagered();
        double winRate = com.servermanagement.client.ClientGamblingData.getWinRate();
        double netProfit = com.servermanagement.client.ClientGamblingData.getNetProfit();
        double biggestWin = com.servermanagement.client.ClientGamblingData.getBiggestWin();
        double biggestLoss = com.servermanagement.client.ClientGamblingData.getBiggestLoss();
        
        int infoY = centerY + 50;
        int lineHeight = 15;
        
        guiGraphics.drawString(this.font, Component.literal("Your Gambling Statistics:"),
            centerX + 20, infoY, 0xFFD700, true);
        
        infoY += 25;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Total Bets: %d", totalBets)),
            centerX + 30, infoY, 0xFFFFFF, true);
        infoY += lineHeight;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Wins: %d | Losses: %d", totalWins, totalLosses)),
            centerX + 30, infoY, 0xFFFFFF, true);
        infoY += lineHeight;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Win Rate: %.1f%%", winRate * 100)),
            centerX + 30, infoY, 0xFFAA00, true);
        infoY += lineHeight;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Total Wagered: $%.2f", totalWagered)),
            centerX + 30, infoY, 0xAAAAAA, true);
        infoY += lineHeight;
        
        int color = netProfit >= 0 ? 0x55FF55 : 0xFF5555;
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Net Profit: $%.2f", netProfit)),
            centerX + 30, infoY, color, true);
        infoY += lineHeight;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Biggest Win: $%.2f", biggestWin)),
            centerX + 30, infoY, 0x55FF55, true);
        infoY += lineHeight;
        
        guiGraphics.drawString(this.font, 
            Component.literal(String.format("Biggest Loss: $%.2f", biggestLoss)),
            centerX + 30, infoY, 0xFF5555, true);
    }
    
    /**
     * Render the item being dragged by the cursor
     */
    private void renderFloatingItem(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Get the carried item (item being dragged) — cache locally
        ItemStack carriedStack = this.menu.getCarried();
        if (carriedStack != null && !carriedStack.isEmpty()) {
            // Render the item centered on the cursor
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 232.0F); // Ensure it's on top
            
            guiGraphics.renderItem(carriedStack, mouseX - 8, mouseY - 8);
            guiGraphics.renderItemDecorations(this.font, carriedStack, mouseX - 8, mouseY - 8);
            
            guiGraphics.pose().popPose();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Block all mouse interactions during animations
        if (isTensionActive || isEndingAnimation) {
            return true; // Consume the event
        }
        // Let the parent class handle all slot clicks naturally
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow ESC key (256) even during ending animation to prevent trapping players
        if (isTensionActive) {
            return true; // Block all keys during tension phase
        }
        if (isEndingAnimation) {
            // Allow ESC to close, block everything else
            if (keyCode == 256) {
                isEndingAnimation = false;
                // Show any pending result immediately
                if (hasPendingResult) {
                    showPendingResult();
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true; // Block other keys
        }
        
        // Handle bet amount text box input when not animating
        if (betAmountBox != null && betAmountBox.isFocused() && betAmountBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        // Prevent closing GUI during tension phase only
        if (isTensionActive) {
            return; // Don't close during tension
        }
        // Clear particles to prevent memory leak
        particles.clear();
        super.onClose();
    }
    
    private String getModeTitle() {
        switch (currentMode) {
            case COIN_FLIP: return "Coin Flip";
            case DICE_ROLL: return "Dice Roll";
            case SLOT_MACHINE: return "Slot Machine";
            case ROULETTE: return "Roulette";
            case STATS: return "Statistics";
            default: return "MineStacks";
        }
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Check betAmountBox if it's visible and focused
        if (betAmountBox != null && betAmountBox.isFocused() && betAmountBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
