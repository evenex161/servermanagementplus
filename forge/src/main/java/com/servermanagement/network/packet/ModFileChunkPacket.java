package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client containing a chunk of the mod JAR file.
 * Uses chunked transfer to avoid packet size limits.
 */
public record ModFileChunkPacket(int chunkIndex, int totalChunks, String fileHash, byte[] chunkData) implements IPacket {
    
    public static final int CHUNK_SIZE = 32768; // 32 KB chunks
    
    public ModFileChunkPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readUtf(32767), readChunkBytes(buf));
    }

    private static byte[] readChunkBytes(FriendlyByteBuf buf) {
        int dataLength = Math.min(buf.readInt(), CHUNK_SIZE + 1024);
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        return data;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(fileHash, 32767);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
    }
    
    @Override
    public void handle(CustomPayloadEvent.Context contextSupplier) {
        CustomPayloadEvent.Context context = contextSupplier;
        context.enqueueWork(() -> {
            // This runs on the client
            if (context.getSender() == null) {
                // We're on the client side
                OTAUpdateManager.handleModFileChunk(chunkIndex, totalChunks, chunkData, fileHash);
                
                // Log progress periodically
                if (chunkIndex % 10 == 0 || chunkIndex == totalChunks - 1) {
                    double progress = ((chunkIndex + 1) * 100.0) / totalChunks;
                    ServerManagementMod.LOGGER.info("Download progress: {}/{} chunks ({:.1f}%)", 
                        chunkIndex + 1, totalChunks, progress);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
