package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class WMSetLobbyPacket implements IPacket {
    public static final CustomPacketPayload.Type<WMSetLobbyPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "w_m_set_lobby_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, WMSetLobbyPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), WMSetLobbyPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "lobby_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setLobbySpawn(pos, dimensionId);
                }
            }
        });
        
    }
}
