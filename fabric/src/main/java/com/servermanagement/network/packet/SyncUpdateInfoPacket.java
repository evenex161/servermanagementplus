package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public class SyncUpdateInfoPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncUpdateInfoPacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.tryParse("servermanagement:syncupdateinfo_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncUpdateInfoPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncUpdateInfoPacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final com.servermanagement.updater.UpdateCheckResult result;
    private final boolean smartStartActive;

    public SyncUpdateInfoPacket(com.servermanagement.updater.UpdateCheckResult result, boolean smartStartActive) {
        this.result = result;
        this.smartStartActive = smartStartActive;
    }

    public com.servermanagement.updater.UpdateCheckResult getResult() { return result; }
    public boolean isSmartStartActive() { return smartStartActive; }

    public SyncUpdateInfoPacket(net.minecraft.network.FriendlyByteBuf buf) {
        this(readResult(buf), buf.readBoolean());
    }

    private static com.servermanagement.updater.UpdateCheckResult readResult(net.minecraft.network.FriendlyByteBuf buf) {
        boolean hasModrinth = buf.readBoolean();
        com.servermanagement.updater.UpdateInfo modrinth = null;
        if (hasModrinth) {
            modrinth = new com.servermanagement.updater.UpdateInfo(
                buf.readUtf(64), buf.readUtf(512), buf.readUtf(32767), buf.readUtf(64), buf.readUtf(32)
            );
        }
        boolean hasCurse = buf.readBoolean();
        com.servermanagement.updater.UpdateInfo curse = null;
        if (hasCurse) {
            curse = new com.servermanagement.updater.UpdateInfo(
                buf.readUtf(64), buf.readUtf(512), buf.readUtf(32767), buf.readUtf(64), buf.readUtf(32)
            );
        }
        String cv = buf.readUtf(64);
        return new com.servermanagement.updater.UpdateCheckResult(modrinth, curse, cv);
    }
    
    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBoolean(result != null && result.modrinth() != null);
        if (result != null && result.modrinth() != null) {
            buf.writeUtf(result.modrinth().version() != null ? result.modrinth().version() : "", 64);
            buf.writeUtf(result.modrinth().downloadUrl() != null ? result.modrinth().downloadUrl() : "", 512);
            buf.writeUtf(result.modrinth().changelog() != null ? result.modrinth().changelog() : "", 32767);
            buf.writeUtf(result.modrinth().releaseDate() != null ? result.modrinth().releaseDate() : "", 64);
            buf.writeUtf(result.modrinth().source() != null ? result.modrinth().source() : "", 32);
        }
        buf.writeBoolean(result != null && result.curseforge() != null);
        if (result != null && result.curseforge() != null) {
            buf.writeUtf(result.curseforge().version() != null ? result.curseforge().version() : "", 64);
            buf.writeUtf(result.curseforge().downloadUrl() != null ? result.curseforge().downloadUrl() : "", 512);
            buf.writeUtf(result.curseforge().changelog() != null ? result.curseforge().changelog() : "", 32767);
            buf.writeUtf(result.curseforge().releaseDate() != null ? result.curseforge().releaseDate() : "", 64);
            buf.writeUtf(result.curseforge().source() != null ? result.curseforge().source() : "", 32);
        }
        buf.writeUtf(result != null && result.currentVersion() != null ? result.currentVersion() : "", 64);
        
        buf.writeBoolean(smartStartActive);
    }

    public void handle(net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context ctx) {
        ctx.client().execute(() -> {
            com.servermanagement.client.ClientUpdateManager.receiveUpdateInfo(this);
        });
    }
}

