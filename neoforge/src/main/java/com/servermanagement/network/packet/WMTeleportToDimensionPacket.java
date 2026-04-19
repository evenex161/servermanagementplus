package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class WMTeleportToDimensionPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WMTeleportToDimensionPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "w_m_teleport_to_dimension"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMTeleportToDimensionPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMTeleportToDimensionPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }
        });
        // packet handled
    }
}
