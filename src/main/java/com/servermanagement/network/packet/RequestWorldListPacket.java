package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class RequestWorldListPacket implements IPacket {
    public static final CustomPacketPayload.Type<RequestWorldListPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "request_world_list_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, RequestWorldListPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), RequestWorldListPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public RequestWorldListPacket() {
    }

    public RequestWorldListPacket(FriendlyByteBuf buf) {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                // Build world list and send back
                var worlds = com.servermanagement.features.worldmanager.WorldManager.getInstance()
                    .buildWorldListForClient(player.server);
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncWorldListPacket(worlds), player
                );
            }
        });
        
    }
}
