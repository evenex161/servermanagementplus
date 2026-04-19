package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
