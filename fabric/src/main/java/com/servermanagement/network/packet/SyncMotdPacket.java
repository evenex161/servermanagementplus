package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
/**
 * Server → Client: Syncs the current MOTD text before opening the editor.
 */
public class SyncMotdPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncMotdPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_motd_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMotdPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMotdPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String motdText;

    public SyncMotdPacket(String motdText) {
        this.motdText = motdText;
    }

    public SyncMotdPacket(FriendlyByteBuf buf) {
        this.motdText = buf.readUtf(32767);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            ClientPacketHandler.handleMotdSync(motdText);

}

    public String getMotdText() {
        return motdText;
    }
}
