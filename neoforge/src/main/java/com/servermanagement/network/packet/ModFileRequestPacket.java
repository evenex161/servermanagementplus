package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.server.ModFileTransferManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from client to server to request the mod JAR file for OTA update.
 */
public record ModFileRequestPacket(String requestedVersion, String clientVersion, String clientMinecraftVersion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModFileRequestPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "mod_file_request"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileRequestPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileRequestPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    
    public ModFileRequestPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readUtf(64), buf.readUtf(32));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(requestedVersion, 64);
        buf.writeUtf(clientVersion, 64);
        buf.writeUtf(clientMinecraftVersion, 32);
    }
    
    public void handle(IPayloadContext context) {
context.enqueueWork(() -> {
            ServerPlayer player = (context.player() instanceof ServerPlayer ? (ServerPlayer) context.player() : null);
            if (player != null) {
                // Rate limit: one transfer per player per session (prevent DoS/bandwidth abuse)
                if (ModFileTransferManager.hasActiveOrCompletedTransfer(player)) {
                    ServerManagementMod.LOGGER.warn("Player {} already has an active or completed transfer, rejecting request",
                        player.getName().getString());
                    return;
                }
                
                ServerManagementMod.LOGGER.info("Player {} requested mod update from {} to {} (MC {})", 
                    player.getName().getString(), clientVersion, requestedVersion, clientMinecraftVersion);
                
                // Verify MC version matches before transferring
                String serverMcVersion = com.servermanagement.ota.OTAVersion.loadFromResources().getMinecraftVersion();
                if (!serverMcVersion.equals(clientMinecraftVersion)) {
                    ServerManagementMod.LOGGER.error("MC version mismatch! Server: {}, Client: {}. Refusing transfer.",
                        serverMcVersion, clientMinecraftVersion);
                    return;
                }
                
                // Start file transfer on server side
                ModFileTransferManager.startTransfer(player, requestedVersion);
            }
        });
}
    
    public String getRequestedVersion() {
        return requestedVersion;
    }
    
    public String getClientVersion() {
        return clientVersion;
    }
    
    public String getClientMinecraftVersion() {
        return clientMinecraftVersion;
    }
}
