package com.servermanagement.network.packet;

import com.servermanagement.network.IPacket;
import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record CheckForUpdatesPacket() implements IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "check_for_updates_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }
    
    public CheckForUpdatesPacket(FriendlyByteBuf buf) {
        this();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
    }
    
    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            // Check for updates asynchronously
            UpdateManager.checkForUpdates("2.1.2-b1", "fabric", "1.20.1").thenAccept(optInfo -> {
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
