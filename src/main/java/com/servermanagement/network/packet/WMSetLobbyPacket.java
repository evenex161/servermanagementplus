package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class WMSetLobbyPacket implements IPacket {
    private final BlockPos pos;
    private final String dimensionId;
    private final long clientTick;

    public WMSetLobbyPacket(BlockPos pos, String dimensionId, long clientTick) {
        this.pos = pos;
        this.dimensionId = dimensionId;
        this.clientTick = clientTick;
    }

    public WMSetLobbyPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.dimensionId = buf.readUtf(256);
        this.clientTick = buf.readLong();
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
                    com.servermanagement.features.worldmanager.WorldManager.setLobbySpawn(pos, dimensionId);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
