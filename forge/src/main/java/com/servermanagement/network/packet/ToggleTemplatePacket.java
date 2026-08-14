package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet for toggling a daily task template's enabled state
 */
public record ToggleTemplatePacket(String templateId) implements IPacket {

    public ToggleTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 32767);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
