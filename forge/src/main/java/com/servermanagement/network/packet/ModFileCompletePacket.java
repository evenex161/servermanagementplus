package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client when mod file transfer is complete.
 * Signals client to verify and install the update.
 */
public record ModFileCompletePacket(String fileHash, long fileSize, String version, boolean success, String message) implements IPacket {

    public ModFileCompletePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(128), buf.readLong(), buf.readUtf(64), buf.readBoolean(), buf.readUtf(256));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileHash, 128);
        buf.writeLong(fileSize);
        buf.writeUtf(version, 64);
        buf.writeBoolean(success);
        buf.writeUtf(message, 256);
    }
    
    public void handle(CustomPayloadEvent.Context contextSupplier) {
        CustomPayloadEvent.Context context = contextSupplier;
        context.enqueueWork(() -> {
            // This runs on the client
            if (context.getSender() == null) {
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
        context.setPacketHandled(true);
    }
}
