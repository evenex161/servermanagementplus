package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.PlayerDailyTasks;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import java.util.List;
import java.util.ArrayList;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim the free daily reward
 */
public record ClaimFreeRewardPacket() implements IPacket {

    public ClaimFreeRewardPacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        // No data to write
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            EconomyManager economyManager = EconomyManager.getInstance(player.server);
            if (economyManager == null) return;

            PlayerDailyTasks playerTasks = economyManager.getDailyTasksManager()
                .getOrCreatePlayerTasks(player.getUUID());
            
            if (playerTasks.isFreeRewardAvailable()) {
                // Use the admin-configurable value from template manager
                DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();
                int reward = templateManager != null
                    ? (int) templateManager.getFreeRewardAmount()
                    : playerTasks.getFreeRewardAmount();
                
                // Mark as claimed
                playerTasks.setFreeRewardClaimed(true);
                playerTasks.setLastFreeRewardClaimTime(System.currentTimeMillis());
                
                // Add money to player's bank account
                BankAccount account = economyManager.getOrCreateAccount(player.getUUID());
                account.deposit(reward);
                account.addTransaction(new Transaction(
                    TransactionType.FREE_REWARD, reward,
                    "Free daily reward"));
                
                // Get item rewards from template manager
                List<ItemStack> rewardItems = templateManager != null
                    ? templateManager.getFreeRewardItems()
                    : new ArrayList<>();
                
                if (!rewardItems.isEmpty()) {
                    StringBuilder itemsGivenStr = new StringBuilder();
                    for (int i = 0; i < rewardItems.size(); i++) {
                        ItemStack item = rewardItems.get(i).copy();
                        
                        boolean addedToInventory = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, item.copy());
                        if (!addedToInventory) {
                            economyManager.getBankInventory(player.getUUID()).addItem(
                                item.copy(), 
                                BankInventory.ItemSource.FREE_REWARD, 
                                "Free Daily Reward"
                            );
                            player.sendSystemMessage(Component.literal("§6[Reward] §eInventory full — " + item.getHoverName().getString() + " sent to Bank Inventory."));
                        }
                        
                        if (i > 0) itemsGivenStr.append(", ");
                        itemsGivenStr.append(item.getHoverName().getString()).append(" x").append(item.getCount());
                    }
                    
                    player.displayClientMessage(Component.literal(String.format(
                        "§a§l✓ §r§aClaimed daily reward: §6$%d §a+ §f%s",
                        reward, itemsGivenStr.toString())), true);
                } else {
                    // Send success message (money only)
                    player.displayClientMessage(Component.literal(String.format(
                        "§a§l✓ §r§aClaimed daily reward: §6$%d", reward)), true);
                }
                
                // Save data
                economyManager.save();
            } else {
                long timeUntilNext = playerTasks.getTimeUntilFreeReward();
                String timeStr = PlayerDailyTasks.formatTimeRemaining(timeUntilNext);
                player.sendSystemMessage(Component.literal("§cFree reward not available. Next reward in: " + timeStr));
            }
        });
        ctx.setPacketHandled(true);
    }
}
