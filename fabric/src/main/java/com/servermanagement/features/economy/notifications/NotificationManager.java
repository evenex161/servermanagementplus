package com.servermanagement.features.economy.notifications;

import com.servermanagement.ServerManagementMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages player notifications with clickable chat components
 */
public class NotificationManager {

    /**
     * Send admin ServerManagement dashboard notification
     */
    public static void sendAdminDashboardNotification(ServerPlayer player) {
        MutableComponent header = Component.literal("═══════════════════════════════════════")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        
        MutableComponent title = Component.literal("\n")
                .append(Component.literal("⚙ ServerManagement Dashboard")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        
        MutableComponent message = Component.literal("\n")
                .append(Component.literal("Welcome back, Admin! ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("[Open Dashboard]")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/servermanagement dashboard"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to open ServerManagement Dashboard")
                                                .withStyle(ChatFormatting.YELLOW)))));
        
        MutableComponent footer = Component.literal("\n")
                .append(Component.literal("═══════════════════════════════════════")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        player.sendSystemMessage(header);
        player.sendSystemMessage(title);
        player.sendSystemMessage(message);
        player.sendSystemMessage(footer);
    }

    /**
     * Send unified daily tasks notification (free reward + tasks)
     */
    public static void sendUnifiedDailyTasksNotification(ServerPlayer player, boolean hasFreeReward, 
                                                         int unfinishedCount, int unclaimedCount) {
        MutableComponent message = Component.literal("💰 ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Daily Tasks & Rewards: ")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        // Build description based on what's available
        java.util.List<String> available = new java.util.ArrayList<>();
        
        if (hasFreeReward) {
            available.add("§aFREE REWARD");
        }
        if (unclaimedCount > 0) {
            available.add("§e" + unclaimedCount + " Task" + (unclaimedCount > 1 ? "s" : "") + " to Claim");
        }
        if (unfinishedCount > 0) {
            available.add("§6" + unfinishedCount + " Task" + (unfinishedCount > 1 ? "s" : "") + " in Progress");
        }

        if (!available.isEmpty()) {
            message.append(Component.literal(String.join(" §8| ", available))
                    .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" ")
                            .withStyle(ChatFormatting.WHITE));
        }

        message.append(Component.literal("[Open Daily Tasks]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/bank dailies"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to view tasks and claim rewards")
                                        .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send free reward available notification (DEPRECATED - use sendUnifiedDailyTasksNotification)
     */
    @Deprecated
    public static void sendFreeRewardNotification(ServerPlayer player) {
        MutableComponent message = Component.literal("💰 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("You have a ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("FREE REWARD")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" available! ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("[Claim Now]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank dailies"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to open Daily Tasks")
                                                .withStyle(ChatFormatting.YELLOW)))));
        
        player.sendSystemMessage(message);
    }

    /**
     * Send daily tasks notification (unclaimed or unfinished)
     */
    public static void sendDailyTasksNotification(ServerPlayer player, int unfinishedCount, int unclaimedCount) {
        if (unfinishedCount == 0 && unclaimedCount == 0) {
            return;
        }

        MutableComponent message = Component.literal("📋 ")
                .withStyle(ChatFormatting.AQUA);

        if (unclaimedCount > 0) {
            message.append(Component.literal("You have ")
                    .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(unclaimedCount))
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                    .append(Component.literal(" completed task" + (unclaimedCount > 1 ? "s" : "") + " to claim! ")
                            .withStyle(ChatFormatting.WHITE));
        } else if (unfinishedCount > 0) {
            message.append(Component.literal("You have ")
                    .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(unfinishedCount))
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                    .append(Component.literal(" daily task" + (unfinishedCount > 1 ? "s" : "") + " available! ")
                            .withStyle(ChatFormatting.WHITE));
        }

        message.append(Component.literal("[View Tasks]")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/bank dailies"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to view daily tasks")
                                        .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send bank balance update notification
     */
    public static void sendBalanceUpdateNotification(ServerPlayer player, double oldBalance, double newBalance, String reason) {
        double change = newBalance - oldBalance;
        boolean isPositive = change > 0;

        MutableComponent message = Component.literal("💳 ")
                .withStyle(ChatFormatting.GOLD);

        if (isPositive) {
            message.append(Component.literal("+ $" + String.format("%.2f", change))
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            message.append(Component.literal("- $" + String.format("%.2f", Math.abs(change)))
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        message.append(Component.literal(" | " + reason)
                .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" [View Balance]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank balance"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to view your bank account")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send deposit notification
     */
    public static void sendDepositNotification(ServerPlayer player, double amount) {
        MutableComponent message = Component.literal("💰 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Deposited: $" + String.format("%.2f", amount))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[View Balance]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank balance"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to view your bank account")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send payment notification
     */
    public static void sendPaymentNotification(ServerPlayer player, String recipientName, double amount) {
        MutableComponent message = Component.literal("💸 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Paid $" + String.format("%.2f", amount))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to " + recipientName)
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[View Balance]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank balance"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to view your bank account")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send received payment notification
     */
    public static void sendReceivedPaymentNotification(ServerPlayer player, String senderName, double amount) {
        MutableComponent message = Component.literal("💰 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Received $" + String.format("%.2f", amount))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" from " + senderName)
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[View Balance]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank balance"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to view your bank account")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send task completed notification
     */
    public static void sendTaskCompletedNotification(ServerPlayer player, String taskDescription) {
        MutableComponent message = Component.literal("✓ ")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("Task Complete: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(taskDescription)
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[Claim Reward]")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank dailies"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to claim your reward")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }

    /**
     * Send reward claimed notification
     */
    public static void sendRewardClaimedNotification(ServerPlayer player, double amount) {
        MutableComponent message = Component.literal("🎁 ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Reward Claimed: $" + String.format("%.2f", amount))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("[View Balance]")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand("/bank balance"))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to view your bank account")
                                                .withStyle(ChatFormatting.YELLOW)))));

        player.sendSystemMessage(message);
    }
}
