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
            String currentVersion = com.servermanagement.platform.Services.PLATFORM.getModVersion();
            String loader = com.servermanagement.platform.Services.PLATFORM.getPlatformName().toLowerCase();
            String mcVersion = net.minecraft.SharedConstants.getCurrentVersion().getName();
            
            UpdateManager.checkAllUpdates(currentVersion, loader, mcVersion).thenAccept(result -> {
                if (player.hasDisconnected()) return;
                
                boolean smartStartActive = Boolean.parseBoolean(System.getProperty("servermanagement.smartstart", "false"));
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncUpdateInfoPacket(result, smartStartActive),
                    player
                );
            });
        }
    }
}

