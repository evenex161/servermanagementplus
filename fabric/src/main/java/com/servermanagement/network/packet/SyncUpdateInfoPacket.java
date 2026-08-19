package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.network.IPacket;

public record SyncUpdateInfoPacket(boolean hasUpdate, String version, String changelog, String downloadUrl, String date, boolean smartStartActive) implements IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_update_info_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }
    
    public SyncUpdateInfoPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(32767), buf.readBoolean());
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasUpdate);
        buf.writeUtf(version != null ? version : "", 32767);
        buf.writeUtf(changelog != null ? changelog : "", 32767);
        buf.writeUtf(downloadUrl != null ? downloadUrl : "", 32767);
        buf.writeUtf(date != null ? date : "", 32767);
        buf.writeBoolean(smartStartActive);
    }
    
    public void handle(ServerPlayer player) {
        // This is an S2C packet, so handle it on the client
        // The IPacket interface in fabric requires handle(ServerPlayer) which is for C2S.
        // For S2C, it is registered in ClientPlayNetworking.
    }
}
