package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

public class SyncAutoShowPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncAutoShowPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_auto_show_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAutoShowPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAutoShowPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final boolean autoShow;

    public SyncAutoShowPacket(boolean autoShow) {
        this.autoShow = autoShow;
    }

    public SyncAutoShowPacket(FriendlyByteBuf buf) {
        this.autoShow = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client - update UI
        });
        
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
