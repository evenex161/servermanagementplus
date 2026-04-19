package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server → Client: Syncs the current MOTD text before opening the editor.
 */
public class SyncMotdPacket implements IPacket {
    private final String motdText;

    public SyncMotdPacket(String motdText) {
        this.motdText = motdText;
    }

    public SyncMotdPacket(FriendlyByteBuf buf) {
        this.motdText = buf.readUtf(32767);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleMotdSync(motdText);
        });
        ctx.setPacketHandled(true);
    }

    public String getMotdText() {
        return motdText;
    }
}
