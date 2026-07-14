package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to check mod version compatibility.
 * Triggers OTA update if versions don't match.
 */
public record VersionCheckPacket(String serverModVersion, int serverDataVersion, String serverModJarName,
                                  String serverModJarHash, long serverModJarSize, String serverMinecraftVersion,
                                  String serverModLoader) implements IPacket {

    public VersionCheckPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readInt(), buf.readUtf(256), buf.readUtf(128), buf.readLong(), buf.readUtf(32), buf.readUtf(32));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(serverModVersion, 64);
        buf.writeInt(serverDataVersion);
        buf.writeUtf(serverModJarName, 256);
        buf.writeUtf(serverModJarHash, 128);
        buf.writeLong(serverModJarSize);
        buf.writeUtf(serverMinecraftVersion, 32);
        buf.writeUtf(serverModLoader, 32);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
                if (ctx.get().getSender() == null) {
                    // We're on the client side, disconnect them with the prompt
                    ctx.get().getNetworkManager().disconnect(
                        Component.literal("ServerManagement+ is outdated! Please restart your game to trigger the automatic update.")
                    );
                } else {
                    ServerManagementMod.LOGGER.error("ctx.get().getSender() was not null on client! This shouldn't happen.");
                }
            } else {
                ServerManagementMod.LOGGER.debug("Client and server OTA versions match: {}", 
                    clientOTAVersion.getDisplayVersion());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
