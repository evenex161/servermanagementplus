package com.servermanagement.network.packet;

import com.servermanagement.updater.UpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;

public record CheckForUpdatesPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CheckForUpdatesPacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "checkforupdates_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, CheckForUpdatesPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), CheckForUpdatesPacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public CheckForUpdatesPacket(FriendlyByteBuf buf) {
        this();
    }
    
    public void encode(FriendlyByteBuf buf) {
    }
    
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((ctx.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) ctx.player() : null);
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
        // packet handled
    }
}




