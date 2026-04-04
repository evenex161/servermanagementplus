package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to check mod version compatibility.
 * Triggers OTA update if versions don't match.
 */
public class VersionCheckPacket implements IPacket {
    private final String serverModVersion;
    private final int serverDataVersion;
    private final String serverModJarName;
    private final String serverModJarHash; // SHA-256 hash
    private final long serverModJarSize;
    
    public VersionCheckPacket(String serverModVersion, int serverDataVersion, 
                             String serverModJarName, String serverModJarHash, long serverModJarSize) {
        this.serverModVersion = serverModVersion;
        this.serverDataVersion = serverDataVersion;
        this.serverModJarName = serverModJarName;
        this.serverModJarHash = serverModJarHash;
        this.serverModJarSize = serverModJarSize;
    }
    
    public VersionCheckPacket(FriendlyByteBuf buf) {
        this.serverModVersion = buf.readUtf(64);
        this.serverDataVersion = buf.readInt();
        this.serverModJarName = buf.readUtf(256);
        this.serverModJarHash = buf.readUtf(128);
        this.serverModJarSize = buf.readLong();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(serverModVersion, 64);
        buf.writeInt(serverDataVersion);
        buf.writeUtf(serverModJarName, 256);
        buf.writeUtf(serverModJarHash, 128);
        buf.writeLong(serverModJarSize);
    }
    
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // This runs on the client
            com.servermanagement.ota.OTAVersion clientOTAVersion = 
                com.servermanagement.ota.OTAVersion.loadFromResources();
            com.servermanagement.ota.OTAVersion serverOTAVersion = 
                com.servermanagement.ota.OTAVersion.parseFromString(serverModVersion);
            
            ServerManagementMod.LOGGER.debug("Version check: client={} (build {}), server={} (build {})",
                clientOTAVersion.getVersion(), clientOTAVersion.getBuildNumber(),
                serverOTAVersion.getVersion(), serverOTAVersion.getBuildNumber());
            
            // Check if server version is newer
            if (serverOTAVersion.isNewerThan(clientOTAVersion)) {
                ServerManagementMod.LOGGER.info("Update available: client {} -> server {}",
                    clientOTAVersion.getFullVersion(), serverOTAVersion.getFullVersion());
                
                // Notify client and offer to download update
                if (context.getSender() == null) {
                    // We're on the client side
                    OTAUpdateManager.handleVersionMismatch(
                        clientOTAVersion.getFullVersion(), 
                        serverOTAVersion.getFullVersion(),
                        serverDataVersion,
                        serverModJarName,
                        serverModJarHash,
                        serverModJarSize
                    );
                } else {
                    ServerManagementMod.LOGGER.error("context.getSender() was not null on client! This shouldn't happen.");
                }
            } else {
                ServerManagementMod.LOGGER.debug("Client and server OTA versions match: {}", 
                    clientOTAVersion.getFullVersion());
            }
        });
        context.setPacketHandled(true);
    }
    
    public String getServerModVersion() {
        return serverModVersion;
    }
    
    public int getServerDataVersion() {
        return serverDataVersion;
    }
    
    public String getServerModJarName() {
        return serverModJarName;
    }
    
    public String getServerModJarHash() {
        return serverModJarHash;
    }
    
    public long getServerModJarSize() {
        return serverModJarSize;
    }
}
