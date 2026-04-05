package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Client-to-server packet for creating or updating a daily task template
 */
public class SaveTemplatePacket implements IPacket {
    public static final CustomPacketPayload.Type<SaveTemplatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "save_template_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveTemplatePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveTemplatePacket::new);
    
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

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
        buf.writeInt(taskTypeOrdinal);
        buf.writeUtf(description, 100);
        buf.writeInt(goal);
        buf.writeInt(rewardAmount);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItem);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            TaskType[] types = TaskType.values();
            if (taskTypeOrdinal < 0 || taskTypeOrdinal >= types.length) return;
            TaskType taskType = types[taskTypeOrdinal];

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            if (templateId.isEmpty()) {
                // Create new
                DailyTaskTemplate template = new DailyTaskTemplate(taskType, goal, rewardAmount, rewardItem, description);
                templateManager.addTemplate(template);
            } else {
                // Update existing
                DailyTaskTemplate existing = templateManager.getTemplate(templateId);
                if (existing != null) {
                    existing.setType(taskType);
                    existing.setCustomDescription(description);
                    existing.setTargetAmount(goal);
                    existing.setRewardAmount(rewardAmount);
                    existing.setRewardItem(rewardItem);
                }
            }

            templateManager.save(server);

            // Sync updated list back to client
            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        
    }
}
