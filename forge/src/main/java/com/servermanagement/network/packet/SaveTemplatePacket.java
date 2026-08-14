package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;
import java.util.List;

/**
 * Client-to-server packet for creating or updating a daily task template
 */
public record SaveTemplatePacket(String templateId, int taskTypeOrdinal, String description,
                                  int goal, int rewardAmount, List<ItemStack> rewardItems) implements IPacket {

    public SaveTemplatePacket {
        templateId = templateId != null ? templateId : "";
        rewardItems = rewardItems != null ? rewardItems : new java.util.ArrayList<>();
    }

    public SaveTemplatePacket(String templateId, TaskType taskType, String description, int goal, int rewardAmount, List<ItemStack> rewardItems) {
        this(templateId, taskType.ordinal(), description, goal, rewardAmount, rewardItems);
    }

    public SaveTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readInt(), buf.readUtf(100), buf.readInt(), buf.readInt(),
             ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
        buf.writeInt(taskTypeOrdinal);
        buf.writeUtf(description, 100);
        buf.writeInt(goal);
        buf.writeInt(rewardAmount);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItems);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            TaskType[] types = TaskType.values();
            if (taskTypeOrdinal < 0 || taskTypeOrdinal >= types.length) return;
            TaskType taskType = types[taskTypeOrdinal];

            // Validate bounds on integer fields
            int safeGoal = Math.max(1, Math.min(goal, 10000));
            int safeRewardAmount = Math.max(0, Math.min(rewardAmount, 100000));

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            if (templateId.isEmpty()) {
                // Create new
                DailyTaskTemplate template = new DailyTaskTemplate(taskType, safeGoal, safeRewardAmount, rewardItems, description);
                templateManager.addTemplate(template);
            } else {
                // Update existing
                DailyTaskTemplate existing = templateManager.getTemplate(templateId);
                if (existing != null) {
                    existing.setType(taskType);
                    existing.setCustomDescription(description);
                    existing.setTargetAmount(safeGoal);
                    existing.setRewardAmount(safeRewardAmount);
                    existing.setRewardItems(rewardItems);
                }
            }

            templateManager.save(server);

            // Sync updated list back to client
            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        ctx.setPacketHandled(true);
    }
}
