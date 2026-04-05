package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Client-to-server packet for toggling a daily task template's enabled state
 */
public class ToggleTemplatePacket implements IPacket {
    public static final CustomPacketPayload.Type<ToggleTemplatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "toggle_template_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleTemplatePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleTemplatePacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String templateId;

    public ToggleTemplatePacket(String templateId) {
        this.templateId = templateId;
    }

    public ToggleTemplatePacket(FriendlyByteBuf buf) {
        this.templateId = buf.readUtf(64);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
        
    }
}
