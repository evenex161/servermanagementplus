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
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
                
                // Get item reward from template manager
                ItemStack rewardItem = templateManager != null
                    ? templateManager.getFreeRewardItem()
                    : ItemStack.EMPTY;
                
                // Give item reward if present
                if (!rewardItem.isEmpty()) {
                    boolean addedToInventory = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, rewardItem.copy());
                    
                    if (!addedToInventory) {
                        // Inventory full, add to bank inventory
                        economyManager.getBankInventory(player.getUUID()).addItem(
                            rewardItem.copy(), 
                            BankInventory.ItemSource.FREE_REWARD, 
                            "Free Daily Reward"
                        );
                        player.sendSystemMessage(Component.literal("┬º6[Reward] ┬ºeInventory full ÔÇö item sent to Bank Inventory."));
                    }
                    
                    player.displayClientMessage(Component.literal(String.format(
                        "┬ºa┬ºlÔ£ô ┬ºr┬ºaClaimed daily reward: ┬º6$%d ┬ºa+ ┬ºf%s x%d",
                        reward, rewardItem.getHoverName().getString(), rewardItem.getCount())), true);
                } else {
                    // Send success message (money only)
                    player.displayClientMessage(Component.literal(String.format(
                        "┬ºa┬ºlÔ£ô ┬ºr┬ºaClaimed daily reward: ┬º6$%d", reward)), true);
                }
                
                // Save data
                economyManager.save();
            } else {
                long timeUntilNext = playerTasks.getTimeUntilFreeReward();
                String timeStr = PlayerDailyTasks.formatTimeRemaining(timeUntilNext);
                player.sendSystemMessage(Component.literal("┬ºcFree reward not available. Next reward in: " + timeStr));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
