package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record RequestAutoShowPacket() implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "request_auto_show_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public RequestAutoShowPacket(FriendlyByteBuf buf) {
        this();
    }

        public void encode(FriendlyByteBuf buf) {
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                boolean autoShow = com.servermanagement.config.PlayerPreferences.getAutoShow(player.getUUID());
                // Send response back - stub for now
                com.servermanagement.network.ModNetworking.sendToPlayer(player, new SyncAutoShowPacket(autoShow));
            }

}
}
