package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.server.ModFileTransferManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to request the mod JAR file for OTA update.
 */
public class ModFileRequestPacket implements IPacket {
    private final String requestedVersion;
    private final String clientVersion;
    private final String clientMinecraftVersion;
    
    public ModFileRequestPacket(String requestedVersion, String clientVersion, String clientMinecraftVersion) {
        this.requestedVersion = requestedVersion;
        this.clientVersion = clientVersion;
        this.clientMinecraftVersion = clientMinecraftVersion;
    }
    
    public ModFileRequestPacket(FriendlyByteBuf buf) {
        this.requestedVersion = buf.readUtf(64);
        this.clientVersion = buf.readUtf(64);
        this.clientMinecraftVersion = buf.readUtf(32);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(requestedVersion, 64);
        buf.writeUtf(clientVersion, 64);
        buf.writeUtf(clientMinecraftVersion, 32);
    }
    
    public void handle(CustomPayloadEvent.Context contextSupplier) {
        CustomPayloadEvent.Context context = contextSupplier;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
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
        context.setPacketHandled(true);
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
