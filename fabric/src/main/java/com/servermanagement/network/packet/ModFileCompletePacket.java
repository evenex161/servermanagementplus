package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client when mod file transfer is complete.
 * Signals client to verify and install the update.
 */
public record ModFileCompletePacket(String fileHash, long fileSize, String version, boolean success, String message) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ModFileCompletePacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "mod_file_complete_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileCompletePacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileCompletePacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public ModFileCompletePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readLong(), buf.readUtf(32767), buf.readBoolean(), buf.readUtf(32767));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileHash, 32767);
        buf.writeLong(fileSize);
        buf.writeUtf(version, 32767);
        buf.writeBoolean(success);
        buf.writeUtf(message, 32767);
    }
    
    public void handle(net.minecraft.server.level.ServerPlayer player) {
            // This runs on the client
            if (player == null) {
                // We're on the client side
                if (success) {
                    ServerManagementMod.LOGGER.info("Mod file transfer completed successfully");
                    ServerManagementMod.LOGGER.info("Version: {}, Size: {} bytes, Hash: {}", 
                        version, fileSize, fileHash);
                    
                    OTAUpdateManager.handleTransferComplete(fileHash, fileSize, version);
                } else {
                    ServerManagementMod.LOGGER.error("Mod file transfer failed: {}", message);
                    OTAUpdateManager.handleTransferFailed(message);
                }
            }
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getVersion() {
        return version;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
}
