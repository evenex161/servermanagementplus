package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.network.ModNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Client-to-server packet for creating or updating a daily task template
 */
public class SaveTemplatePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveTemplatePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "save_template"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveTemplatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveTemplatePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final String templateId; // empty for new template
    private final int taskTypeOrdinal;
    private final String description;
    private final int goal;
    private final int rewardAmount;
    private final ItemStack rewardItem;

    public SaveTemplatePacket(String templateId, TaskType taskType, String description, int goal, int rewardAmount, ItemStack rewardItem) {
        this.templateId = templateId != null ? templateId : "";
        this.taskTypeOrdinal = taskType.ordinal();
        this.description = description;
        this.goal = goal;
        this.rewardAmount = rewardAmount;
        this.rewardItem = rewardItem != null ? rewardItem : ItemStack.EMPTY;
    }

    public SaveTemplatePacket(FriendlyByteBuf buf) {
        this.templateId = buf.readUtf(64);
        this.taskTypeOrdinal = buf.readInt();
        this.description = buf.readUtf(100);
        this.goal = buf.readInt();
        this.rewardAmount = buf.readInt();
        this.rewardItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
        buf.writeInt(taskTypeOrdinal);
        buf.writeUtf(description, 100);
        buf.writeInt(goal);
        buf.writeInt(rewardAmount);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItem);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
                DailyTaskTemplate template = new DailyTaskTemplate(taskType, safeGoal, safeRewardAmount, rewardItem, description);
                templateManager.addTemplate(template);
            } else {
                // Update existing
                DailyTaskTemplate existing = templateManager.getTemplate(templateId);
                if (existing != null) {
                    existing.setType(taskType);
                    existing.setCustomDescription(description);
                    existing.setTargetAmount(safeGoal);
                    existing.setRewardAmount(safeRewardAmount);
                    existing.setRewardItem(rewardItem);
                }
            }

            templateManager.save(server);

            // Sync updated list back to client
            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        // packet handled
    }
}
