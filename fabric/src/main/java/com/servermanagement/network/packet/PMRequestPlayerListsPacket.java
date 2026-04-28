package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
/**
 * Client-to-server packet requesting the banned/whitelisted player lists.
 */
public record PMRequestPlayerListsPacket() implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "p_m_request_player_lists_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public PMRequestPlayerListsPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
        // No data needed
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.playermanager.PlayerManagerSingleton.sendPlayerLists(player);
            }

}
}
