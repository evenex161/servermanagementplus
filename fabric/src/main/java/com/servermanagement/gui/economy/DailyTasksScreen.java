package com.servermanagement.gui.economy;


import com.servermanagement.gui.ScalableContainerScreen;
import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.gui.widgets.ModernButton;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.ClaimDailyTaskPacket;
import com.servermanagement.network.packet.ClaimFreeRewardPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Professional Daily Tasks GUI with Comprehensive Animations
 * Features:
 * - Smooth progress bar animations
 * - Celebrate animation on claim
 * - Particle effects on reward claim
 * - Smooth transitions and fades
 * - Pulsing effects for claimable rewards
 */
public class DailyTasksScreen extends ScalableContainerScreen<DailyTasksMenu> {
    
    private static final int TASK_HEIGHT = 65;
    private static final int TASK_PADDING = 10;
    private static final int FREE_REWARD_HEIGHT = 60;
    
    // Animation states
    private static final int MAX_TASKS = 3; // Maximum number of displayable tasks
    private float[] progressAnimations = new float[MAX_TASKS]; // Smooth progress bar animation per task
    private float[] claimAnimations = new float[MAX_TASKS];    // Claim celebration animation per task
    private float[] pulseAnimations = new float[MAX_TASKS];    // Pulse animation for claimable tasks
    private boolean[] isClaiming = new boolean[MAX_TASKS];     // Track if task is being claimed
    private float freeRewardPulse = 0f;                // Pulse animation for free reward
    private boolean isClaimingFreeReward = false;      // Track if free reward is being claimed
    private int celebrationTimer = 0;                  // Global celebration timer
    private String celebrationMessage = "";            // Message to display during celebration
    
    // Animation parameters
    private static final float PROGRESS_ANIMATION_SPEED = 0.05f;
    private static final float PULSE_SPEED = 0.08f;
    private static final int CELEBRATION_DURATION = 60; // 3 seconds at 20 TPS
    
    /** Dynamic task slot height computed from available screen space */
    private int taskSlotHeight = TASK_HEIGHT + TASK_PADDING;
    /** Dynamic task card height (slot minus padding) */
    private int taskCardHeight = TASK_HEIGHT;
    
    public DailyTasksScreen(DailyTasksMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 400, 430);
        this.imageHeight = 430; // Tall enough to fit 3 tasks + free reward section
        this.imageWidth = 400;
        
        // Initialize animations
        for (int i = 0; i < 3; i++) {
            progressAnimations[i] = 0f;
            claimAnimations[i] = 0f;
            pulseAnimations[i] = 0f;
            isClaiming[i] = false;
        }
    }
    
    @Override
    protected void init() {
        // Dynamically compute task slot height so free reward section does not overlap
        int availableForTasks = this.imageHeight - 70 - FREE_REWARD_HEIGHT - 25;
        this.taskSlotHeight = availableForTasks / MAX_TASKS;
        this.taskCardHeight = Math.max(TASK_HEIGHT, this.taskSlotHeight - TASK_PADDING);
        
        super.init();
        // Pull fresh task list + reset time from the cache on every init() so a
        // late SyncDailyTasksPacket triggering refreshOpenScreen() reflects the
        // new state instead of the snapshot latched in the menu constructor.
        this.menu.reloadFromClientCache();
        this.clearWidgets(); // Clear widgets to prevent accumulation
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Back to Bank button for ALL players (removed admin check)
        this.addRenderableWidget(new ModernButton(
            centerX + 10, centerY + 10, 100, 20,
            Component.literal("← Bank"),
            button -> ModNetworking.sendToServer(new OpenGuiPacket(OpenGuiPacket.GuiType.BANK)),
            ModernButton.ButtonStyle.SECONDARY
        ));
        
        // Close button
        this.addRenderableWidget(new ModernButton(
            centerX + this.imageWidth - 90, centerY + 10, 80, 20,
            Component.literal("Close"),
            button -> this.onClose(),
            ModernButton.ButtonStyle.DANGER
        ));
        
        // Claim buttons for each task
        List<DailyTask> tasks = menu.getTasks();
        for (int i = 0; i < tasks.size() && i < MAX_TASKS; i++) {
            DailyTask task = tasks.get(i);
            int taskY = centerY + 70 + (i * taskSlotHeight);
            
            if (task.isCompleted() && !task.isClaimed()) {
                final int taskIndex = i;
                this.addRenderableWidget(new ModernButton(
                    centerX + this.imageWidth - 100, taskY + taskCardHeight - 27, 80, 20,
                    Component.literal("Claim $" + task.getReward()),
                    button -> claimReward(taskIndex),
                    ModernButton.ButtonStyle.PRIMARY
                ));
            }
        }
        
        // Free reward button
        boolean freeRewardAvailable = com.servermanagement.client.ClientDailyTasksData.isFreeRewardAvailable();
        int freeRewardAmount = com.servermanagement.client.ClientDailyTasksData.getFreeRewardAmount();
        int freeRewardY = centerY + this.imageHeight - FREE_REWARD_HEIGHT - 15;
        
        if (freeRewardAvailable) {
            this.addRenderableWidget(new ModernButton(
                centerX + this.imageWidth - 120, freeRewardY + 25, 100, 20,
                Component.literal("Claim $" + freeRewardAmount),
                button -> claimFreeReward(),
                ModernButton.ButtonStyle.SUCCESS
            ));
        }
    }
    
    private void claimFreeReward() {
        // Send packet to server to claim free reward
        ModNetworking.sendToServer(new ClaimFreeRewardPacket());
        
        // Start celebration animation
        isClaimingFreeReward = true;
        int freeRewardAmount = com.servermanagement.client.ClientDailyTasksData.getFreeRewardAmount();
        celebrationTimer = CELEBRATION_DURATION;
        celebrationMessage = "§6+$" + freeRewardAmount + " Free Reward!";
        
        // Mark as unavailable locally for immediate visual feedback
        com.servermanagement.client.ClientDailyTasksData.setFreeRewardAvailable(false);
        
        // Refresh the screen to update buttons
        this.rebuildWidgets();
    }
    
    private void claimReward(int taskIndex) {
        List<DailyTask> tasks = menu.getTasks();
        if (taskIndex < 0 || taskIndex >= tasks.size() || taskIndex >= MAX_TASKS) return;
        
        DailyTask task = tasks.get(taskIndex);
        if (!task.isCompleted() || task.isClaimed()) return;
        
        // Send packet to server to claim reward
        ModNetworking.sendToServer(new ClaimDailyTaskPacket(taskIndex));
        
        // Start celebration animation
        isClaiming[taskIndex] = true;
        claimAnimations[taskIndex] = 0f;
        celebrationTimer = CELEBRATION_DURATION;
        celebrationMessage = "§6+$" + task.getReward() + " Claimed!";
        
        // Mark as claimed locally for immediate visual feedback
        task.setClaimed(true);
        
        // Refresh the screen to update buttons
        this.rebuildWidgets();
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        List<DailyTask> tasks = menu.getTasks();
        
        // Update animations
        for (int i = 0; i < tasks.size() && i < 3; i++) {
            DailyTask task = tasks.get(i);
            float targetProgress = task.getProgressPercentage() / 100f;
            
            // Smooth progress bar animation
            if (progressAnimations[i] < targetProgress) {
                progressAnimations[i] = Math.min(progressAnimations[i] + PROGRESS_ANIMATION_SPEED, targetProgress);
            }
            
            // Claim celebration animation
            if (isClaiming[i]) {
                claimAnimations[i] += 0.05f;
                if (claimAnimations[i] >= 1f) {
                    isClaiming[i] = false;
                    claimAnimations[i] = 0f;
                }
            }
            
            // Pulse animation for claimable tasks
            if (task.isCompleted() && !task.isClaimed()) {
                pulseAnimations[i] += PULSE_SPEED;
                if (pulseAnimations[i] > (float) (Math.PI * 2)) {
                    pulseAnimations[i] -= (float) (Math.PI * 2);
                }
            } else {
                pulseAnimations[i] = 0f;
            }
        }
        
        // Free reward pulse animation
        boolean freeRewardAvailable = com.servermanagement.client.ClientDailyTasksData.isFreeRewardAvailable();
        if (freeRewardAvailable) {
            freeRewardPulse += PULSE_SPEED;
            if (freeRewardPulse > (float) (Math.PI * 2)) {
                freeRewardPulse -= (float) (Math.PI * 2);
            }
        } else {
            freeRewardPulse = 0f;
        }
        
        // Update celebration timer
        if (celebrationTimer > 0) {
            celebrationTimer--;
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Main dark background with subtle animation
        int bgAlpha = celebrationTimer > 0 ? 0xE0 + (int)(Math.sin(celebrationTimer * 0.2) * 10) : 0xE0;
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + this.imageHeight, 
            (bgAlpha << 24) | 0x101010);
        
        // Header bar
        guiGraphics.fill(centerX, centerY, centerX + this.imageWidth, centerY + 40, 0xA0202020);
        
        // Task backgrounds with animation
        List<DailyTask> tasks = menu.getTasks();
        for (int i = 0; i < tasks.size() && i < MAX_TASKS; i++) {
            int taskY = centerY + 70 + (i * taskSlotHeight);
            
            // Add glow effect for claimable tasks
            if (tasks.get(i).isCompleted() && !tasks.get(i).isClaimed()) {
                float pulse = (float) Math.sin(pulseAnimations[i]) * 0.3f + 0.7f;
                int glowAlpha = (int) (pulse * 50);
                guiGraphics.fill(centerX + 8, taskY - 2, centerX + this.imageWidth - 8, 
                    taskY + taskCardHeight + 2, (glowAlpha << 24) | 0xFFD700);
            }
            
            guiGraphics.fill(centerX + 10, taskY, centerX + this.imageWidth - 10, 
                taskY + taskCardHeight, 0xA01A1A1A);
        }
        
        // Free reward background with animation
        int freeRewardY = centerY + this.imageHeight - FREE_REWARD_HEIGHT - 15;
        boolean freeRewardAvailable = com.servermanagement.client.ClientDailyTasksData.isFreeRewardAvailable();
        
        if (freeRewardAvailable) {
            // Golden glow for available free reward
            float pulse = (float) Math.sin(freeRewardPulse) * 0.3f + 0.7f;
            int glowAlpha = (int) (pulse * 60);
            guiGraphics.fill(centerX + 8, freeRewardY - 2, centerX + this.imageWidth - 8, 
                freeRewardY + FREE_REWARD_HEIGHT + 2, (glowAlpha << 24) | 0x55FF55);
        }
        
        // Free reward box
        guiGraphics.fill(centerX + 10, freeRewardY, centerX + this.imageWidth - 10, 
            freeRewardY + FREE_REWARD_HEIGHT, 0xA01A1A1A);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        // Title
        Component titleText = Component.literal("Daily Tasks");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, 
            centerX + (this.imageWidth - titleWidth) / 2, 
            centerY + 16, 
            0xFFFFFF, false);
        
        // Reset timer
        renderResetTimer(guiGraphics, centerX, centerY);
        
        // Render tasks with animations
        renderTasks(guiGraphics, centerX, centerY, partialTick);
        
        // Render free reward section
        renderFreeReward(guiGraphics, centerX, centerY);
        
        // Celebration message
        if (celebrationTimer > 0) {
            float alpha = celebrationTimer < 20 ? celebrationTimer / 20f : 1f;
            int alphaInt = (int) (alpha * 255);
            
            int msgWidth = this.font.width(celebrationMessage);
            int scale = celebrationTimer > 40 ? 2 : 1;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX + this.imageWidth / 2f, centerY + 40, 0);
            guiGraphics.pose().scale(scale, scale, 1);
            guiGraphics.drawString(this.font, Component.literal(celebrationMessage),
                -msgWidth / 2, 0, (alphaInt << 24) | 0xFFD700, true);
            guiGraphics.pose().popPose();
        }
    }
    
    private void renderResetTimer(GuiGraphics guiGraphics, int centerX, int centerY) {
        long resetTime = menu.getResetTime();
        long currentTime = System.currentTimeMillis();
        long timeUntilReset = resetTime - currentTime;
        
        if (timeUntilReset > 0) {
            long hours = TimeUnit.MILLISECONDS.toHours(timeUntilReset);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilReset) % 60;
            
            String timeStr = String.format("Resets in: %dh %dm", hours, minutes);
            int timeWidth = this.font.width(timeStr);
            guiGraphics.drawString(this.font, Component.literal(timeStr),
                centerX + this.imageWidth - timeWidth - 15, 
                centerY + 50, 
                0xFFAA00, false);
        } else {
            String timeStr = "Ready to reset!";
            int timeWidth = this.font.width(timeStr);
            guiGraphics.drawString(this.font, Component.literal(timeStr),
                centerX + this.imageWidth - timeWidth - 15, 
                centerY + 50, 
                0x55FF55, false);
        }
    }
    
    private void renderTasks(GuiGraphics guiGraphics, int centerX, int centerY, float partialTick) {
        List<DailyTask> tasks = menu.getTasks();
        
        if (tasks.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("No daily tasks available"),
                centerX + 20, centerY + 80, 0x808080, false);
            return;
        }
        
        for (int i = 0; i < tasks.size() && i < MAX_TASKS; i++) {
            DailyTask task = tasks.get(i);
            int taskY = centerY + 70 + (i * taskSlotHeight);
            
            renderTask(guiGraphics, task, centerX + 15, taskY, i, partialTick);

            // Divider between tasks (not after the last visible one).
            int displayed = Math.min(tasks.size(), MAX_TASKS);
            if (i < displayed - 1) {
                int dividerY = taskY + taskSlotHeight - 4;
                int dx0 = centerX + 20;
                int dx1 = centerX + this.imageWidth - 20;
                guiGraphics.fill(dx0, dividerY, dx1, dividerY + 1, 0x40FFFFFF);
            }
        }
    }
    
    private void renderTask(GuiGraphics guiGraphics, DailyTask task, int x, int y, int index, float partialTick) {
        // Task number and icon with bounce animation during claim
        float bounceOffset = 0f;
        if (isClaiming[index]) {
            bounceOffset = (float) Math.sin(claimAnimations[index] * Math.PI * 2) * 3f;
        }
        
        String taskNumber = "#" + (index + 1);
        guiGraphics.drawString(this.font, Component.literal(taskNumber),
            x, (int) (y + 5 + bounceOffset), 0xFFAA00, false);
        
        String icon = task.getType().getIcon();
        guiGraphics.drawString(this.font, Component.literal(icon),
            x + 25, (int) (y + 5 + bounceOffset), 0xFFFFFF, false);
        
        // Task description
        String description = task.getDescription();
        guiGraphics.drawString(this.font, Component.literal(description),
            x + 45, y + 5, 0xCCCCCC, false);
        
        // Progress text
        String progressText = task.getProgressString();
        guiGraphics.drawString(this.font, Component.literal(progressText),
            x, y + 22, 0xAAAAAA, false);
        
        // Progress percentage with animation
        int percentage = task.getProgressPercentage();
        float animatedPercentage = Mth.lerp(partialTick, progressAnimations[index] * 100, 
            Math.min(progressAnimations[index] * 100 + PROGRESS_ANIMATION_SPEED * 100, percentage));
        
        String percentStr = (int) animatedPercentage + "%";
        int percentWidth = this.font.width(percentStr);
        int percentColor = animatedPercentage >= 100 ? 0x55FF55 : 0xFFFFFF;
        
        // Add glow effect when reaching 100%
        if (animatedPercentage >= 99 && animatedPercentage < 100) {
            percentColor = 0xFFD700; // Gold color for completion moment
        }
        
        guiGraphics.drawString(this.font, Component.literal(percentStr),
            x + this.imageWidth - percentWidth - 140, y + 22, 
            percentColor, animatedPercentage >= 100);
        
        // Animated progress bar
        int barX = x;
        int barY = y + 38;
        int barWidth = this.imageWidth - 120;
        int barHeight = 10;
        
        // Background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2A2A2A);
        
        // Progress fill with smooth animation
        float fillProgress = progressAnimations[index];
        int fillWidth = (int) (barWidth * fillProgress);
        
        int fillColor;
        if (task.isCompleted()) {
            // Pulsing gold color for completed tasks
            if (task.isClaimed()) {
                fillColor = 0xFF888888; // Gray for claimed
            } else {
                float pulse = (float) Math.sin(pulseAnimations[index]) * 0.2f + 0.8f;
                int goldPulse = (int) (pulse * 255);
                fillColor = 0xFF000000 | (goldPulse << 16) | (goldPulse << 8) | 0;
            }
        } else {
            fillColor = 0xFF4A9EFF; // Blue for in-progress
        }
        
        guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        
        // Border
        guiGraphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF555555);
        guiGraphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFF555555);
        guiGraphics.fill(barX, barY, barX + 1, barY + barHeight, 0xFF555555);
        guiGraphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFF555555);
        
        // Reward display with shine effect
        String rewardText = "Reward: $" + task.getReward();
        int rewardColor = 0x55FF55;
        
        if (isClaiming[index]) {
            // Rainbow shine effect during claim
            float hue = claimAnimations[index];
            rewardColor = java.awt.Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
        }
        
        guiGraphics.drawString(this.font, Component.literal(rewardText),
            x, y + 52, rewardColor, false);
        
        // Status badge
        if (task.isClaimed()) {
            guiGraphics.drawString(this.font, Component.literal("✓ CLAIMED"),
                x + this.imageWidth - 120, y + 52, 0x888888, false);
        } else if (task.isCompleted()) {
            // Pulsing "COMPLETE" badge
            float pulse = (float) Math.sin(pulseAnimations[index]) * 0.3f + 0.7f;
            int alpha = (int) (pulse * 255);
            int completeColor = (alpha << 24) | 0x55FF55;
            guiGraphics.drawString(this.font, Component.literal("✓ COMPLETE"),
                x + this.imageWidth - 120, y + 52, completeColor, true);
        } else {
            guiGraphics.drawString(this.font, Component.literal("IN PROGRESS"),
                x + this.imageWidth - 120, y + 52, 0xFFAA00, false);
        }
    }
    
    private void renderFreeReward(GuiGraphics guiGraphics, int centerX, int centerY) {
        int freeRewardY = centerY + this.imageHeight - FREE_REWARD_HEIGHT - 15;
        boolean freeRewardAvailable = com.servermanagement.client.ClientDailyTasksData.isFreeRewardAvailable();
        int freeRewardAmount = com.servermanagement.client.ClientDailyTasksData.getFreeRewardAmount();
        long timeUntilFree = com.servermanagement.client.ClientDailyTasksData.getTimeUntilFreeReward();
        
        // Title
        String title = "💎 FREE DAILY REWARD";
        guiGraphics.drawString(this.font, Component.literal(title),
            centerX + 20, freeRewardY + 8, 0x55FF55, true);
        
        if (freeRewardAvailable) {
            // Show reward amount with pulse
            float pulse = (float) Math.sin(freeRewardPulse) * 0.3f + 0.7f;
            int alpha = (int) (pulse * 255);
            int color = (alpha << 24) | 0xFFD700;
            
            String amountText = "$" + freeRewardAmount + " Available!";
            guiGraphics.drawString(this.font, Component.literal(amountText),
                centerX + 20, freeRewardY + 28, color, true);
        } else {
            // Show cooldown timer
            String cooldownText = "Next reward in: " + 
                com.servermanagement.features.economy.PlayerDailyTasks.formatTimeRemaining(timeUntilFree);
            guiGraphics.drawString(this.font, Component.literal(cooldownText),
                centerX + 20, freeRewardY + 28, 0x888888, false);
            
            guiGraphics.drawString(this.font, Component.literal("⏰ Come back later!"),
                centerX + 20, freeRewardY + 42, 0x666666, false);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels
    }
}
