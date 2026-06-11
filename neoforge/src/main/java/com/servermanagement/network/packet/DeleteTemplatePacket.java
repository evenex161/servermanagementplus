package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Client-to-server packet for deleting a daily task template
 */
public record DeleteTemplatePacket(String templateId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeleteTemplatePacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "delete_template"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, DeleteTemplatePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), DeleteTemplatePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public DeleteTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null || !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) return;

            var server = player.level().getServer();
            if (server == null) return;

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();
            
            templateManager.deleteTemplate(templateId);
            templateManager.save(server);

            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        // packet handled
    }
}
