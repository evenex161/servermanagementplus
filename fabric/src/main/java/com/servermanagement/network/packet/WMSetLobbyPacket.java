package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import java.util.function.Supplier;

public class WMSetLobbyPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMSetLobbyPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_set_lobby_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMSetLobbyPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMSetLobbyPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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
