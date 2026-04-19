package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.PlayerDailyTasks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim a daily task reward
 */
public record ClaimDailyTaskPacket(int taskIndex) implements IPacket {

    public ClaimDailyTaskPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(taskIndex);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            EconomyManager economyManager = EconomyManager.getInstance(player.server);
            if (economyManager == null) return;

            // Get player tasks and the specific task
            PlayerDailyTasks playerTasks = economyManager.getDailyTasksManager().getPlayerTasks(player.getUUID());
            if (playerTasks == null || taskIndex < 0 || taskIndex >= playerTasks.getTasks().size()) {
                player.sendSystemMessage(Component.literal("§cInvalid task!"));
                return;
            }
            
            DailyTask task = playerTasks.getTasks().get(taskIndex);
            
            // Claim the task reward
            int reward = economyManager.getDailyTasksManager().claimTaskReward(player.getUUID(), taskIndex);
            
            if (reward > 0) {
                // Add money to player's bank account
                BankAccount account = economyManager.getOrCreateAccount(player.getUUID());
                account.deposit(reward);
                
                // Give item reward if present
                ItemStack rewardItem = task.getRewardItem();
                if (!rewardItem.isEmpty()) {
                    boolean addedToInventory = com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, rewardItem.copy());
                    
                    if (!addedToInventory) {
                        // Inventory full, add to bank inventory
                        economyManager.getBankInventory(player.getUUID()).addItem(
                            rewardItem.copy(), 
                            BankInventory.ItemSource.DAILY_TASK, 
                            "Daily Task #" + (taskIndex + 1)
                        );
                        player.sendSystemMessage(Component.literal("§e⚠ Inventory full! Item sent to Bank Inventory."));
                    }
                    
                    player.sendSystemMessage(Component.literal("§a✓ Claimed $" + reward + " + " + 
                        rewardItem.getHoverName().getString() + " x" + rewardItem.getCount() + 
                        " for completing task #" + (taskIndex + 1)));
                } else {
                    // Send success message (money only)
                    player.sendSystemMessage(Component.literal("§a✓ Claimed $" + reward + " for completing task #" + (taskIndex + 1)));
                }
                
                // Save data
                economyManager.save();
            } else {
                player.sendSystemMessage(Component.literal("§cTask is not completed or already claimed!"));
            }
        });
        ctx.setPacketHandled(true);
    }
}
