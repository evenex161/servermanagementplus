package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

public record WMTeleportToDimensionPacket(String dimensionId) implements IPacket {

    public WMTeleportToDimensionPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
