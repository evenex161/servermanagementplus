package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncUpdateInfoPacket(boolean hasUpdate, String version, String changelog, String downloadUrl, String date, boolean smartStartActive) implements IPacket {
    
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
    
    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            com.servermanagement.client.ClientUpdateManager.receiveUpdateInfo(this);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
