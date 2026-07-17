package com.servermanagement.network.compression;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.ByteBuffer;
import java.util.List;

public class ZstdPacketCompressor {

    public static class Encoder extends MessageToMessageEncoder<ByteBuf> {
        private final int compressionLevel;

        public Encoder(int compressionLevel) {
            this.compressionLevel = compressionLevel;
        }

        public Encoder() {
            this(3); // Default Zstd compression level
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            int readableBytes = msg.readableBytes();
            if (readableBytes == 0) {
                out.add(msg.retain());
                return;
            }

            long maxCompressedSize = Zstd.compressBound(readableBytes);
            ByteBuf outBuf = ctx.alloc().directBuffer((int) maxCompressedSize);

            ByteBuffer inNio = msg.nioBuffer();
            ByteBuffer outNio = outBuf.nioBuffer(0, (int) maxCompressedSize);

            int compressedSize = Zstd.compress(outNio, inNio, compressionLevel);

            outBuf.writerIndex(outBuf.writerIndex() + compressedSize);
            out.add(outBuf);
        }
    }

    public static class Decoder extends MessageToMessageDecoder<ByteBuf> {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            int readableBytes = msg.readableBytes();
            if (readableBytes == 0) {
                out.add(msg.retain());
                return;
            }

            // For safe decompression we either need the original size sent over the network,
            // or we use Zstd.decompressedSize (which requires a frame). Zstd-jni supports this.
            ByteBuffer inNio = msg.nioBuffer();
            long decompressedSize = Zstd.decompressedSize(inNio);
            
            if (decompressedSize == 0) {
                throw new IllegalStateException("Zstd frame does not contain decompressed size or is invalid.");
            }

            ByteBuf outBuf = ctx.alloc().directBuffer((int) decompressedSize);
            ByteBuffer outNio = outBuf.nioBuffer(0, (int) decompressedSize);

            Zstd.decompress(outNio, inNio);

            outBuf.writerIndex(outBuf.writerIndex() + (int) decompressedSize);
            out.add(outBuf);
        }
    }
}
