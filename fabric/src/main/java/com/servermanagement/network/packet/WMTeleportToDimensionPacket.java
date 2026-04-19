package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class WMTeleportToDimensionPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<WMTeleportToDimensionPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_teleport_to_dimension_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, WMTeleportToDimensionPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMTeleportToDimensionPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String dimensionId;

    public WMTeleportToDimensionPacket(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public WMTeleportToDimensionPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }

}
}
