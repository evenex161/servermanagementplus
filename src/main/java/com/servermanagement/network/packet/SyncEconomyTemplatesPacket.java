package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs economy templates and free reward settings
 */
public class SyncEconomyTemplatesPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncEconomyTemplatesPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_economy_templates_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncEconomyTemplatesPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncEconomyTemplatesPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final List<TemplateData> templates;
    private final int freeRewardAmount;
    private final int freeRewardCooldownHours;

    public SyncEconomyTemplatesPacket(List<TemplateData> templates, int freeRewardAmount, int freeRewardCooldownHours) {
        this.templates = templates;
        this.freeRewardAmount = freeRewardAmount;
        this.freeRewardCooldownHours = freeRewardCooldownHours;
    }

    public SyncEconomyTemplatesPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        templates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            templates.add(new TemplateData(
                buf.readUtf(64),
                buf.readInt(),
                buf.readUtf(100),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf)
            ));
        }
        this.freeRewardAmount = buf.readInt();
        this.freeRewardCooldownHours = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(templates.size());
        for (TemplateData t : templates) {
            buf.writeUtf(t.id, 64);
            buf.writeInt(t.taskTypeOrdinal);
            buf.writeUtf(t.description, 100);
            buf.writeInt(t.goal);
            buf.writeInt(t.rewardAmount);
            buf.writeBoolean(t.enabled);
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, t.rewardItem);
        }
        buf.writeInt(freeRewardAmount);
        buf.writeInt(freeRewardCooldownHours);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleEconomyTemplates(templates, freeRewardAmount, freeRewardCooldownHours);
        });
        
    }

    /**
     * Helper to sync templates to a specific player
     */
    public static void syncToPlayer(ServerPlayer player, MinecraftServer server) {
        var economyManager = EconomyManager.getInstance(server);
        DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();
        List<DailyTaskTemplate> allTemplates = templateManager.getAllTemplates();

        List<TemplateData> data = new ArrayList<>();
        for (DailyTaskTemplate t : allTemplates) {
            data.add(new TemplateData(
                t.getId(),
                t.getType().ordinal(),
                t.getDescription(),
                t.getTargetAmount(),
                (int) t.getRewardAmount(),
                t.isEnabled(),
                t.getRewardItem()
            ));
        }

        ModNetworking.sendToPlayer(
            new SyncEconomyTemplatesPacket(
                data,
                (int) templateManager.getFreeRewardAmount(),
                templateManager.getFreeRewardCooldownHours()
            ),
            player
        );
    }

    /**
     * Lightweight data class for template info over the network
     */
    public record TemplateData(
        String id,
        int taskTypeOrdinal,
        String description,
        int goal,
        int rewardAmount,
        boolean enabled,
        ItemStack rewardItem
    ) {
        public TaskType getTaskType() {
            TaskType[] types = TaskType.values();
            if (taskTypeOrdinal >= 0 && taskTypeOrdinal < types.length) {
                return types[taskTypeOrdinal];
            }
            return TaskType.BREAK_BLOCKS;
        }
    }
}
