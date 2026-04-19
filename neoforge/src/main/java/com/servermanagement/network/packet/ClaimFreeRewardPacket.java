package com.servermanagement.network.packet;

import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.BankInventory;
import com.servermanagement.features.economy.PlayerDailyTasks;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.Transaction;
import com.servermanagement.features.economy.TransactionType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to claim the free daily reward
 */
public record ClaimFreeRewardPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClaimFreeRewardPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "claim_free_reward"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClaimFreeRewardPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ClaimFreeRewardPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ClaimFreeRewardPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
        // No data to write
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
                        player.sendSystemMessage(Component.literal("§6[Reward] §eInventory full — item sent to Bank Inventory."));
                    }
                    
                    player.displayClientMessage(Component.literal(String.format(
                        "§a§l✓ §r§aClaimed daily reward: §6$%d §a+ §f%s x%d",
                        reward, rewardItem.getHoverName().getString(), rewardItem.getCount())), true);
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
        // packet handled
    }
}
