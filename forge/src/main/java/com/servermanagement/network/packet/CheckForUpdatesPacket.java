package com.servermanagement.network.packet;

import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

public record CheckForUpdatesPacket() implements IPacket {
    
    public CheckForUpdatesPacket(FriendlyByteBuf buf) {
        this();
    }
    
    @Override
    public void encode(FriendlyByteBuf buf) {
    }
    
    @Override
    public void handle(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check for updates asynchronously
                UpdateManager.checkForUpdates("2.1.1-b01", "forge", "1.21.1").thenAccept(optInfo -> {
                    if (player.hasDisconnected()) return;
                    
                    if (optInfo.isPresent()) {
                        var info = optInfo.get();
                        boolean smartStartActive = Boolean.parseBoolean(System.getProperty("servermanagement.smartstart", "false"));
                        com.servermanagement.network.ModNetworking.sendToPlayer(
                            new SyncUpdateInfoPacket(true, info.version(), info.changelog(), info.downloadUrl(), info.releaseDate(), smartStartActive),
                            player
                        );
                    } else {
                        boolean smartStartActive = Boolean.parseBoolean(System.getProperty("servermanagement.smartstart", "false"));
                        com.servermanagement.network.ModNetworking.sendToPlayer(
                            new SyncUpdateInfoPacket(false, "", "", "", "", smartStartActive),
                            player
                        );
                    }
                });
            }
        });
        ctx.setPacketHandled(true);
    }
}


