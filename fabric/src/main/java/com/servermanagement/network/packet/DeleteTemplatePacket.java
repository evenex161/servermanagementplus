package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

/**
 * Client-to-server packet for deleting a daily task template
 */
public record DeleteTemplatePacket(String templateId) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "delete_template_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public DeleteTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64));
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
