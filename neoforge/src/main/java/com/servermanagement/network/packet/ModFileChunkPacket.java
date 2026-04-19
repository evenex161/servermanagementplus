package com.servermanagement.network.packet;


import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.OTAUpdateManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Packet sent from server to client containing a chunk of the mod JAR file.
 * Uses chunked transfer to avoid packet size limits.
 */
public record ModFileChunkPacket(int chunkIndex, int totalChunks, String fileHash, byte[] chunkData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModFileChunkPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "mod_file_chunk"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ModFileChunkPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), ModFileChunkPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
// Full file hash for verification
    
    public static final int CHUNK_SIZE = 32768; // 32 KB chunks
    
    public ModFileChunkPacket(int chunkIndex, int totalChunks, byte[] chunkData, String fileHash) {
        this(chunkIndex, totalChunks, fileHash, chunkData);
    }
    
    public ModFileChunkPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readUtf(128), decodeChunkData(buf));
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(fileHash, 128);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
    }
    
    public void handle(IPayloadContext context) {
context.enqueueWork(() -> {
            // This runs on the client
            if ((context.player() instanceof ServerPlayer ? (ServerPlayer) context.player() : null) == null) {
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

    private static byte[] decodeChunkData(FriendlyByteBuf buf) {
        int dataLength = Math.min(buf.readInt(), CHUNK_SIZE + 1024);
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        return data;
    }
}
