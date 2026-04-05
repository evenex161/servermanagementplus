package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Packet sent from server to client when mod file transfer is complete.
 * Signals client to verify and install the update.
 */
public class ModFileCompletePacket implements IPacket {
    public static final CustomPacketPayload.Type<ModFileCompletePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "mod_file_complete_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileCompletePacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileCompletePacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String fileHash;
    private final long fileSize;
    private final String version;
    private final boolean success;
    private final String message;
    
    public ModFileCompletePacket(String fileHash, long fileSize, String version, boolean success, String message) {
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.version = version;
        this.success = success;
        this.message = message;
    }
    
    public ModFileCompletePacket(FriendlyByteBuf buf) {
        this.fileHash = buf.readUtf(128);
        this.fileSize = buf.readLong();
        this.version = buf.readUtf(64);
        this.success = buf.readBoolean();
        this.message = buf.readUtf(256);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileHash, 128);
        buf.writeLong(fileSize);
        buf.writeUtf(version, 64);
        buf.writeBoolean(success);
        buf.writeUtf(message, 256);
    }
    
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // This runs on the client
            if (context.player() == null) {
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
        });
        
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
