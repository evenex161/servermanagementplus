package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record SyncUpdateInfoPacket(boolean hasUpdate, String version, String changelog, String downloadUrl, String date, boolean smartStartActive) implements IPacket {
    
    public SyncUpdateInfoPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readUtf(64), buf.readUtf(32767), buf.readUtf(512), buf.readUtf(64), buf.readBoolean());
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasUpdate);
        buf.writeUtf(version != null ? version : "", 64);
        buf.writeUtf(changelog != null ? changelog : "", 32767);
        buf.writeUtf(downloadUrl != null ? downloadUrl : "", 512);
        buf.writeUtf(date != null ? date : "", 64);
        buf.writeBoolean(smartStartActive);
    }
    
    @Override
    public void handle(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            com.servermanagement.client.ClientUpdateManager.receiveUpdateInfo(this);
        });
        ctx.setPacketHandled(true);
    }
}


