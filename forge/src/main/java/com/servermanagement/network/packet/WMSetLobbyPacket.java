package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record WMSetLobbyPacket(BlockPos pos, String dimensionId, long clientTick) implements IPacket {

    public WMSetLobbyPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf(256), buf.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(dimensionId, 256);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "lobby_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setLobbySpawn(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, dimensionId,
                        player.getYRot(), player.getXRot());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
