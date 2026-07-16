package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;

public record SyncUpdateInfoPacket(boolean hasUpdate, String version, String changelog, String downloadUrl, String date, boolean smartStartActive) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncUpdateInfoPacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "syncupdateinfo_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncUpdateInfoPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncUpdateInfoPacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public SyncUpdateInfoPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readUtf(64), buf.readUtf(32767), buf.readUtf(512), buf.readUtf(64), buf.readBoolean());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasUpdate);
        buf.writeUtf(version != null ? version : "", 64);
        buf.writeUtf(changelog != null ? changelog : "", 32767);
        buf.writeUtf(downloadUrl != null ? downloadUrl : "", 512);
        buf.writeUtf(date != null ? date : "", 64);
        buf.writeBoolean(smartStartActive);
    }
    
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            com.servermanagement.client.ClientUpdateManager.receiveUpdateInfo(this);
        });
        // packet handled
    }
}




