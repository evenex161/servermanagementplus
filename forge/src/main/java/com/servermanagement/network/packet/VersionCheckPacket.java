package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to check mod version compatibility.
 * Triggers OTA update if versions don't match.
 */
public record VersionCheckPacket(String serverModVersion, int serverDataVersion, String serverModJarName,
                                  String serverModJarHash, long serverModJarSize, String serverMinecraftVersion,
                                  String serverModLoader) implements IPacket {

    public VersionCheckPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readInt(), buf.readUtf(32767), buf.readUtf(32767), buf.readLong(), buf.readUtf(32767), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(serverModVersion, 32767);
        buf.writeInt(serverDataVersion);
        buf.writeUtf(serverModJarName, 32767);
        buf.writeUtf(serverModJarHash, 32767);
        buf.writeLong(serverModJarSize);
        buf.writeUtf(serverMinecraftVersion, 32767);
        buf.writeUtf(serverModLoader, 32767);
    }
    
    public void handle(CustomPayloadEvent.Context contextSupplier) {
        CustomPayloadEvent.Context context = contextSupplier;
        context.enqueueWork(() -> {
            // This runs on the client
            com.servermanagement.ota.OTAVersion clientOTAVersion = 
                com.servermanagement.ota.OTAVersion.loadFromResources();
            com.servermanagement.ota.OTAVersion serverOTAVersion = 
                com.servermanagement.ota.OTAVersion.parseFromString(serverModVersion);
            
            ServerManagementMod.LOGGER.debug("Version check: client={} (build {}, MC {}, loader {}), server={} (build {}, MC {}, loader {})",
                clientOTAVersion.getVersion(), clientOTAVersion.getBuildNumber(), clientOTAVersion.getMinecraftVersion(), clientOTAVersion.getModLoader(),
                serverOTAVersion.getVersion(), serverOTAVersion.getBuildNumber(), serverMinecraftVersion, serverModLoader);
            
            // Verify Minecraft version compatibility
            if (!clientOTAVersion.getMinecraftVersion().equals(serverMinecraftVersion)) {
                ServerManagementMod.LOGGER.error("Minecraft version mismatch! Client MC {} vs Server MC {}. OTA update blocked.",
                    clientOTAVersion.getMinecraftVersion(), serverMinecraftVersion);
                return;
            }
            
            // Verify mod loader compatibility
            if (!"unknown".equals(clientOTAVersion.getModLoader()) && !"unknown".equals(serverModLoader)
                    && !clientOTAVersion.getModLoader().equals(serverModLoader)) {
                ServerManagementMod.LOGGER.error("Mod loader mismatch! Client {} vs Server {}. OTA update blocked.",
                    clientOTAVersion.getModLoader(), serverModLoader);
                return;
            }
            
            // Check if server version is newer
            if (serverOTAVersion.isNewerThan(clientOTAVersion)) {
                ServerManagementMod.LOGGER.info("Update available: client {} -> server {}",
                    clientOTAVersion.getDisplayVersion(), serverOTAVersion.getDisplayVersion());
                
                // Notify client and offer to download update
                if (context.getSender() == null) {
                    // We're on the client side
                    OTAUpdateManager.handleVersionMismatch(
                        clientOTAVersion.getDisplayVersion(), 
                        serverOTAVersion.getDisplayVersion(),
                        serverDataVersion,
                        serverModJarName,
                        serverModJarHash,
                        serverModJarSize,
                        serverMinecraftVersion,
                        serverModLoader
                    );
                } else {
                    ServerManagementMod.LOGGER.error("context.getSender() was not null on client! This shouldn't happen.");
                }
            } else {
                ServerManagementMod.LOGGER.debug("Client and server OTA versions match: {}", 
                    clientOTAVersion.getDisplayVersion());
            }
        });
        context.setPacketHandled(true);
    }
}
