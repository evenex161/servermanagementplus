package com.servermanagement.network.packet;


import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from server to client to check mod version compatibility.
 * Triggers OTA update if versions don't match.
 */
public record VersionCheckPacket(String serverModVersion, int serverDataVersion, String serverModJarName, String serverModJarHash, long serverModJarSize, String serverMinecraftVersion, String serverModLoader) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VersionCheckPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "version_check"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, VersionCheckPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), VersionCheckPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
// SHA-256 hash

    
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
    
    public void handle(IPayloadContext context) {
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
                if ((context.player() instanceof ServerPlayer ? (ServerPlayer) context.player() : null) == null) {
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
                    ServerManagementMod.LOGGER.error("(context.player() instanceof ServerPlayer ? (ServerPlayer) context.player() : null) was not null on client! This shouldn't happen.");
                }
            } else {
                ServerManagementMod.LOGGER.debug("Client and server OTA versions match: {}", 
                    clientOTAVersion.getDisplayVersion());
            }
        });
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
    
    public String getServerMinecraftVersion() {
        return serverMinecraftVersion;
    }
    
    public String getServerModLoader() {
        return serverModLoader;
    }
}
