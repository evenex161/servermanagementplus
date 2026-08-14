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
        // packet handled
    }
}
