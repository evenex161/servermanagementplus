package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

/**
 * Client-to-server packet for toggling a daily task template's enabled state
 */
public class ToggleTemplatePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ToggleTemplatePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "toggle_template_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleTemplatePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleTemplatePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String templateId;

    public ToggleTemplatePacket(String templateId) {
        this.templateId = templateId;
    }

    public ToggleTemplatePacket(FriendlyByteBuf buf) {
        this.templateId = buf.readUtf(64);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
