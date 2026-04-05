package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class WMTeleportToDimensionPacket implements IPacket {
    public static final CustomPacketPayload.Type<WMTeleportToDimensionPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "w_m_teleport_to_dimension_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMTeleportToDimensionPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMTeleportToDimensionPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }
        });
        
    }
}
