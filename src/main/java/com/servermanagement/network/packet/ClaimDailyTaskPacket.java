package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.PlayerDailyTasks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to claim a daily task reward
 */
public class ClaimDailyTaskPacket implements IPacket {
    public static final CustomPacketPayload.Type<ClaimDailyTaskPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "claim_daily_task_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimDailyTaskPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimDailyTaskPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final int taskIndex;

    public ClaimDailyTaskPacket(int taskIndex) {
        this.taskIndex = taskIndex;
    }

    public ClaimDailyTaskPacket(FriendlyByteBuf buf) {
        this.taskIndex = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(taskIndex);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
                    boolean addedToInventory = player.getInventory().add(rewardItem.copy());
                    
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
        
    }
}
