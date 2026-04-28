package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WMTeleportToDimensionPacket implements IPacket {
    private final String dimensionId;

    public WMTeleportToDimensionPacket(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public WMTeleportToDimensionPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
