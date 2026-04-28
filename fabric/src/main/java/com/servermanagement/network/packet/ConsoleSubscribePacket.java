package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
/**
 * Client-to-server packet to subscribe/unsubscribe from server log streaming.
 */
public record ConsoleSubscribePacket(boolean subscribe) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "console_subscribe_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ConsoleSubscribePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(subscribe);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                var manager = com.servermanagement.server.ServerConsoleManager.getInstance();
                if (subscribe) {
                    manager.subscribe(player);
                } else {
                    manager.unsubscribe(player.getUUID());
                }
            }

}
}
