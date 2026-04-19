package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record RequestWorldListPacket() implements IPacket {

    public RequestWorldListPacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null) {
                // Build world list and send back
                var worlds = com.servermanagement.features.worldmanager.WorldManager.getInstance()
                    .buildWorldListForClient(player.server);
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncWorldListPacket(worlds), player
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
