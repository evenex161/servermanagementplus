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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs economy templates and free reward settings
 */
public record SyncEconomyTemplatesPacket(List<TemplateData> templates, int freeRewardAmount, int freeRewardCooldownHours, List<ItemStack> freeRewardItems) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncEconomyTemplatesPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_economy_templates_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncEconomyTemplatesPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncEconomyTemplatesPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncEconomyTemplatesPacket(FriendlyByteBuf buf) {
        this(decodeTemplates(buf), buf.readInt(), buf.readInt(), decodeItems(buf));
    }

    private static List<ItemStack> decodeItems(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        }
        return items;
    }

    private static List<TemplateData> decodeTemplates(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<TemplateData> templates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf(32767);
            int compCount = buf.readInt();
            java.util.List<com.servermanagement.features.economy.TaskComponent> components = new java.util.ArrayList<>();
            for(int j = 0; j < compCount; j++) {
                components.add(new com.servermanagement.features.economy.TaskComponent(
                    com.servermanagement.features.economy.TaskType.values()[buf.readInt()],
                    buf.readInt(),
                    buf.readUtf(32767)
                ));
            }
            templates.add(new TemplateData(
                id,
                components,
                buf.readInt(),
                buf.readBoolean(),
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf)
            ));
        }
        return templates;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(templates.size());
        for (TemplateData t : templates) {
            buf.writeUtf(t.id, 32767);
            buf.writeInt(t.components.size());
            for (com.servermanagement.features.economy.TaskComponent comp : t.components) {
                buf.writeInt(comp.getType().ordinal());
                buf.writeInt(comp.getTargetAmount());
                buf.writeUtf(comp.getCustomDescription() != null ? comp.getCustomDescription() : "", 32767);
            }
            buf.writeInt(t.rewardAmount);
            buf.writeBoolean(t.enabled);
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, t.rewardItems);
        }
        buf.writeInt(freeRewardAmount);
                buf.writeInt(freeRewardCooldownHours);
        buf.writeInt(freeRewardItems.size());
        for (ItemStack item : freeRewardItems) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, item);
        }
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientPacketHandler.handleEconomyTemplates(templates, freeRewardAmount, freeRewardCooldownHours, freeRewardItems);

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
                t.getComponents(),
                (int) t.getRewardAmount(),
                t.isEnabled(),
                t.getRewardItems()
            ));
        }

        ModNetworking.sendToPlayer(
            new SyncEconomyTemplatesPacket(
                data,
                (int) templateManager.getFreeRewardAmount(),
                templateManager.getFreeRewardCooldownHours(),
                templateManager.getFreeRewardItems()
            ),
            player
        );
    }

    /**
     * Lightweight data class for template info over the network
     */
    public record TemplateData(
        String id,
        List<com.servermanagement.features.economy.TaskComponent> components,
        int rewardAmount,
        boolean enabled,
        List<ItemStack> rewardItems
    ) {
        public com.servermanagement.features.economy.TaskType getTaskType() {
            return components != null && !components.isEmpty() ? components.get(0).getType() : com.servermanagement.features.economy.TaskType.BREAK_BLOCKS;
        }
    }
}
