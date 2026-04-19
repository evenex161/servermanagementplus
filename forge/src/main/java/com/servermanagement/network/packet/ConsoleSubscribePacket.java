package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client-to-server packet to subscribe/unsubscribe from server log streaming.
 */
public record ConsoleSubscribePacket(boolean subscribe) implements IPacket {

    public ConsoleSubscribePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(subscribe);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                var manager = com.servermanagement.server.ServerConsoleManager.getInstance();
                if (subscribe) {
                    manager.subscribe(player);
                } else {
                    manager.unsubscribe(player.getUUID());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
