package com.servermanagement.mixin;

import com.servermanagement.network.compression.ZstdPacketCompressor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow private Channel channel;

    @Inject(method = "setupCompression", at = @At("RETURN"))
    private void onSetupCompression(int threshold, boolean validateDecompressed, CallbackInfo ci) {
        // We inject our Zstandard compressor AFTER the vanilla compressor if applicable,
        // or just into the pipeline.
        // Actually, replacing vanilla compression is more complex, but we can just add ours.
        // Let's add it before the encoder and after the decoder.
        ChannelPipeline pipeline = this.channel.pipeline();
        
        if (pipeline.get("zstd_decoder") == null) {
            pipeline.addBefore("decoder", "zstd_decoder", new ZstdPacketCompressor.Decoder());
        }
        if (pipeline.get("zstd_encoder") == null) {
            pipeline.addBefore("encoder", "zstd_encoder", new ZstdPacketCompressor.Encoder(3));
        }
    }
}
