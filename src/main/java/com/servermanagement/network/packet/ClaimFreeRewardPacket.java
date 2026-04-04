package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.PlayerDailyTasks;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim the free daily reward
 */
public class ClaimFreeRewardPacket implements IPacket {

    public ClaimFreeRewardPacket() {
    }

    public ClaimFreeRewardPacket(FriendlyByteBuf buf) {
        // No data to read
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
                
                // Get item reward from template manager
                ItemStack rewardItem = templateManager != null
                    ? templateManager.getFreeRewardItem()
                    : ItemStack.EMPTY;
                
                // Give item reward if present
                if (!rewardItem.isEmpty()) {
                    boolean addedToInventory = player.getInventory().add(rewardItem.copy());
                    
                    if (!addedToInventory) {
                        // Inventory full, add to bank inventory
                        economyManager.getBankInventory(player.getUUID()).addItem(
                            rewardItem.copy(), 
                            BankInventory.ItemSource.FREE_REWARD, 
                            "Free Daily Reward"
                        );
                        player.sendSystemMessage(Component.literal("§e⚠ Inventory full! Item sent to Bank Inventory."));
                    }
                    
                    player.sendSystemMessage(Component.literal("§a✓ Claimed free daily reward: $" + reward + " + " + 
                        rewardItem.getHoverName().getString() + " x" + rewardItem.getCount()));
                } else {
                    // Send success message (money only)
                    player.sendSystemMessage(Component.literal("§a✓ Claimed free daily reward: $" + reward));
                }
                
                // Save data
                economyManager.save();
            } else {
                long timeUntilNext = playerTasks.getTimeUntilFreeReward();
                String timeStr = PlayerDailyTasks.formatTimeRemaining(timeUntilNext);
                player.sendSystemMessage(Component.literal("§cFree reward not available. Next reward in: " + timeStr));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
