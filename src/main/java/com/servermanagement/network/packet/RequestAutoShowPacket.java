package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class RequestAutoShowPacket implements IPacket {
    public static final CustomPacketPayload.Type<RequestAutoShowPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "request_auto_show_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestAutoShowPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestAutoShowPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public RequestAutoShowPacket() {
    }

    public RequestAutoShowPacket(FriendlyByteBuf buf) {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                boolean autoShow = com.servermanagement.config.PlayerPreferences.getAutoShow(player.getUUID());
                // Send response back - stub for now
                com.servermanagement.network.ModNetworking.sendToPlayer(new SyncAutoShowPacket(autoShow), player);
            }
        });
        
    }
}
