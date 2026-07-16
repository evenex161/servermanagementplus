package com.servermanagement.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record CheckForUpdatesPacket() implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<CheckForUpdatesPacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "check_for_updates_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, CheckForUpdatesPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), CheckForUpdatesPacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public CheckForUpdatesPacket(FriendlyByteBuf buf) {
        this();
    }
    
    public void encode(FriendlyByteBuf buf) {
    }
    
    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            // Check for updates asynchronously
            UpdateManager.checkForUpdates("2.1.1-b01", "fabric", "1.20.1").thenAccept(optInfo -> {
                if (player.hasDisconnected()) return;
                
                boolean smartStartActive = Boolean.parseBoolean(System.getProperty("servermanagement.smartstart", "false"));
                if (optInfo.isPresent()) {
                    var info = optInfo.get();
                    com.servermanagement.network.ModNetworking.sendToPlayer(
                        new SyncUpdateInfoPacket(true, info.version(), info.changelog(), info.downloadUrl(), info.releaseDate(), smartStartActive),
                        player
                    );
                } else {
                    com.servermanagement.network.ModNetworking.sendToPlayer(
                        new SyncUpdateInfoPacket(false, "", "", "", "", smartStartActive),
                        player
                    );
                }
            });
        }
    }
}

