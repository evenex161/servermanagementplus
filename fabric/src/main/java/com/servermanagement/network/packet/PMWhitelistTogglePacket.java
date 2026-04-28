package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
/**
 * Client-to-server packet to toggle whitelist enforcement on/off.
 */
public record PMWhitelistTogglePacket(boolean enabled) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "p_m_whitelist_toggle_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public PMWhitelistTogglePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                var server = player.getServer();
                if (server != null) {
                    server.getPlayerList().setUsingWhiteList(enabled);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        enabled ? "\u00a7aWhitelist enforcement enabled" : "\u00a7cWhitelist enforcement disabled"));
                    com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
                }
            }

}
}
