package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class PMSpectatePlayerPacket implements IPacket {
    public static final CustomPacketPayload.Type<PMSpectatePlayerPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "p_m_spectate_player_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PMSpectatePlayerPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), PMSpectatePlayerPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String playerName;

    public PMSpectatePlayerPacket(String playerName) {
        this.playerName = playerName;
    }

    public PMSpectatePlayerPacket(FriendlyByteBuf buf) {
        this.playerName = buf.readUtf(16);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerName, 16);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasPermissions(2)) {
                if (playerName == null || playerName.length() > 16 || !playerName.matches("[a-zA-Z0-9_]+")) {
                    return;
                }
                com.servermanagement.features.playermanager.PlayerManagerSingleton.spectatePlayer(player, playerName);
            }
        });
        
    }
}
