package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.server.ModFileTransferManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to request the mod JAR file for OTA update.
 */
public class ModFileRequestPacket implements IPacket {
    public static final CustomPacketPayload.Type<ModFileRequestPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "mod_file_request_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileRequestPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileRequestPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
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
