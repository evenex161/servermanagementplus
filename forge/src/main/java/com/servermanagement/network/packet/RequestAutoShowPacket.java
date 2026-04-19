package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record RequestAutoShowPacket() implements IPacket {

    public RequestAutoShowPacket(FriendlyByteBuf buf) {
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
                boolean autoShow = com.servermanagement.config.PlayerPreferences.getAutoShow(player.getUUID());
                // Send response back - stub for now
                com.servermanagement.network.ModNetworking.sendToPlayer(new SyncAutoShowPacket(autoShow), player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
