package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
/**
 * Client-to-server packet to toggle whitelist enforcement on/off.
 */
public class PMWhitelistTogglePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<PMWhitelistTogglePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "p_m_whitelist_toggle_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, PMWhitelistTogglePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMWhitelistTogglePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean enabled;

    public PMWhitelistTogglePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public PMWhitelistTogglePacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
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
