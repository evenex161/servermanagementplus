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
public record ToggleTemplatePacket(String templateId) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "toggle_template_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ToggleTemplatePacket(FriendlyByteBuf buf) {
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

            DailyTaskTemplate template = templateManager.getTemplate(templateId);
            if (template != null) {
                template.setEnabled(!template.isEnabled());
                templateManager.save(server);
            }

            SyncEconomyTemplatesPacket.syncToPlayer(player, server);

}
}
