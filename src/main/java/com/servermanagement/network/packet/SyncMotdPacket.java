package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleMotdSync(motdText);
        });
        ctx.get().setPacketHandled(true);
    }

    public String getMotdText() {
        return motdText;
    }
}
