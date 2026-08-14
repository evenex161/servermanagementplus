package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Client-to-server packet for toggling a daily task template's enabled state
 */
public record ToggleTemplatePacket(String templateId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleTemplatePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_template"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleTemplatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleTemplatePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public ToggleTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 32767);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            DailyTaskTemplate template = templateManager.getTemplate(templateId);
            if (template != null) {
                template.setEnabled(!template.isEnabled());
                templateManager.save(server);
            }

            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        // packet handled
    }
}
