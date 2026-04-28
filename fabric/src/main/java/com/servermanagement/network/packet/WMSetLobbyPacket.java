package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import java.util.function.Supplier;

public record WMSetLobbyPacket(BlockPos pos, String dimensionId, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_set_lobby_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public WMSetLobbyPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf(256), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(dimensionId, 256);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "lobby_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setLobbySpawn(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, dimensionId,
                        player.getYRot(), player.getXRot());
                }
            }

}
}
