package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet for deleting a daily task template
 */
public record DeleteTemplatePacket(String templateId) implements IPacket {

    public DeleteTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId, 64);
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
            
            templateManager.deleteTemplate(templateId);
            templateManager.save(server);

            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        ctx.setPacketHandled(true);
    }
}
