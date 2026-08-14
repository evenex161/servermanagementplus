package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.PlayerDailyTasks;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to claim a daily task reward
 */
public record ClaimDailyTaskPacket(int taskIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimDailyTaskPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "claim_daily_task"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimDailyTaskPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimDailyTaskPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ClaimDailyTaskPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(taskIndex);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
                
                // Give item rewards if present
                List<ItemStack> rewardItems = task.getRewardItems();
                if (rewardItems != null && !rewardItems.isEmpty()) {
                    StringBuilder itemsString = new StringBuilder();
                    boolean first = true;
                    for (ItemStack rewardItem : rewardItems) {
                        if (rewardItem.isEmpty()) continue;
                        if (!first) itemsString.append(", ");
                        itemsString.append(rewardItem.getHoverName().getString()).append(" x").append(rewardItem.getCount());
                        first = false;

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
                    }
                    
                    if (itemsString.length() > 0) {
                        player.sendSystemMessage(Component.literal("§a✓ Claimed $" + reward + " + " + 
                            itemsString.toString() + " for completing task #" + (taskIndex + 1)));
                    } else {
                        player.sendSystemMessage(Component.literal("§a✓ Claimed $" + reward + " for completing task #" + (taskIndex + 1)));
                    }
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
        // packet handled
    }
}
