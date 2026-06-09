package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
/**
 * Server → Client: Syncs the current MOTD text before opening the editor.
 */
public record SyncMotdPacket(String motdText) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_motd_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncMotdPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
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
