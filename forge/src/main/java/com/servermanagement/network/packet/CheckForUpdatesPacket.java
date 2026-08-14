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
        });
        ctx.setPacketHandled(true);
    }
}


