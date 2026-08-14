package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;

public record SaveTemplatePacket(String templateId, int taskTypeOrdinal, String description, int goal, int rewardAmount, List<ItemStack> rewardItems) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SaveTemplatePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "save_template_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveTemplatePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveTemplatePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public SaveTemplatePacket(String templateId, TaskType taskType, String description, int goal, int rewardAmount, List<ItemStack> rewardItems) {
        this(templateId != null ? templateId : "", taskType.ordinal(), description, goal, rewardAmount, rewardItems != null ? rewardItems : new ArrayList<>());
    }

    public SaveTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readInt(), buf.readUtf(100), buf.readInt(), buf.readInt(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
        buf.writeInt(taskTypeOrdinal);
        buf.writeUtf(description, 100);
        buf.writeInt(goal);
        buf.writeInt(rewardAmount);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItems);
    }

    public void handle(net.minecraft.server.level.ServerPlayer player) {
        if (player == null || !player.hasPermissions(2)) return;

        var server = player.getServer();
        if (server == null) return;

        TaskType[] types = TaskType.values();
        if (taskTypeOrdinal < 0 || taskTypeOrdinal >= types.length) return;
        TaskType taskType = types[taskTypeOrdinal];

        int safeGoal = Math.max(1, Math.min(goal, 10000));
        int safeRewardAmount = Math.max(0, Math.min(rewardAmount, 100000));

        var economyManager = EconomyManager.getInstance(server);
        DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

        if (templateId.isEmpty()) {
            DailyTaskTemplate template = new DailyTaskTemplate(taskType, safeGoal, safeRewardAmount, rewardItems, description);
            templateManager.addTemplate(template);
        } else {
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
        SyncEconomyTemplatesPacket.syncToPlayer(player, server);
    }
}
