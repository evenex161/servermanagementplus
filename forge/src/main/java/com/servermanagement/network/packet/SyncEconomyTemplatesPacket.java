package com.servermanagement.network.packet;

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
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs economy templates and free reward settings
 */
public record SyncEconomyTemplatesPacket(List<TemplateData> templates, int freeRewardAmount,
                                          int freeRewardCooldownHours) implements IPacket {

    public SyncEconomyTemplatesPacket(FriendlyByteBuf buf) {
        this(readTemplates(buf), buf.readInt(), buf.readInt());
    }

    private static List<TemplateData> readTemplates(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<TemplateData> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new TemplateData(
                buf.readUtf(64),
                buf.readInt(),
                buf.readUtf(100),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readItem()
            ));
        }
        return list;
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
            buf.writeItem(t.rewardItem);
        }
        buf.writeInt(freeRewardAmount);
        buf.writeInt(freeRewardCooldownHours);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleEconomyTemplates(templates, freeRewardAmount, freeRewardCooldownHours);
        });
        ctx.get().setPacketHandled(true);
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
