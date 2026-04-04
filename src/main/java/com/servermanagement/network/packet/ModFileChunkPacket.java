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
public class ModFileChunkPacket implements IPacket {
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] chunkData;
    private final String fileHash; // Full file hash for verification
    
    public static final int CHUNK_SIZE = 32768; // 32 KB chunks
    
    public ModFileChunkPacket(int chunkIndex, int totalChunks, byte[] chunkData, String fileHash) {
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
        this.fileHash = fileHash;
    }
    
    public ModFileChunkPacket(FriendlyByteBuf buf) {
        this.chunkIndex = buf.readInt();
        this.totalChunks = buf.readInt();
        this.fileHash = buf.readUtf(128);
        int dataLength = buf.readInt();
        this.chunkData = new byte[dataLength];
        buf.readBytes(chunkData);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(fileHash, 128);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
    }
    
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
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public byte[] getChunkData() {
        return chunkData;
    }
    
    public String getFileHash() {
        return fileHash;
    }
}
