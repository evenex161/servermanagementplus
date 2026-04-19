package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

/**
 * Client-to-server packet for deleting a daily task template
 */
public class DeleteTemplatePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<DeleteTemplatePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "delete_template_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, DeleteTemplatePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), DeleteTemplatePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String templateId;

    public DeleteTemplatePacket(String templateId) {
        this.templateId = templateId;
    }

    public DeleteTemplatePacket(FriendlyByteBuf buf) {
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
            
            templateManager.deleteTemplate(templateId);
            templateManager.save(server);

            SyncEconomyTemplatesPacket.syncToPlayer(player, server);

}
}
