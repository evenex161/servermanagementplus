package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

/**
 * Packet sent from server to client containing a chunk of the mod JAR file.
 * Uses chunked transfer to avoid packet size limits.
 */
public record ModFileChunkPacket(int chunkIndex, int totalChunks, byte[] chunkData, String fileHash) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ModFileChunkPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "mod_file_chunk_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileChunkPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileChunkPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }// Full file hash for verification
    
    public static final int CHUNK_SIZE = 32768; // 32 KB chunks
    public ModFileChunkPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), decodeChunkPayload(buf));
    }

    private ModFileChunkPacket(int chunkIndex, int totalChunks, Object[] payload) {
        this(chunkIndex, totalChunks, (byte[]) payload[0], (String) payload[1]);
    }

    private static Object[] decodeChunkPayload(FriendlyByteBuf buf) {
        String fileHash = buf.readUtf(128);
        int dataLength = Math.min(buf.readInt(), CHUNK_SIZE + 1024);
        byte[] chunkData = new byte[dataLength];
        buf.readBytes(chunkData);
        return new Object[]{chunkData, fileHash};
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(fileHash, 128);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
    }
    
    public void handle(net.minecraft.server.level.ServerPlayer player) {
            // This runs on the client
            if (player == null) {
                // We're on the client side
                OTAUpdateManager.handleModFileChunk(chunkIndex, totalChunks, chunkData, fileHash);
                
                // Log progress periodically
                if (chunkIndex % 10 == 0 || chunkIndex == totalChunks - 1) {
                    double progress = ((chunkIndex + 1) * 100.0) / totalChunks;
                    ServerManagementMod.LOGGER.info("Download progress: {}/{} chunks ({:.1f}%)", 
                        chunkIndex + 1, totalChunks, progress);
                }
            }
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
