package com.servermanagement.network.packet;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class ToggleAutoShowPacket implements IPacket {
    public static final CustomPacketPayload.Type<ToggleAutoShowPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "toggle_auto_show_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleAutoShowPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ToggleAutoShowPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final boolean autoShow;

    public ToggleAutoShowPacket(boolean autoShow) {
        this.autoShow = autoShow;
    }

    public ToggleAutoShowPacket(FriendlyByteBuf buf) {
        this.autoShow = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                // Save player preference
                com.servermanagement.config.PlayerPreferences.setAutoShow(player.getUUID(), autoShow);
            }
        });
        
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
