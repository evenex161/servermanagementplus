package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Server → Client: Syncs the current MOTD text before opening the editor.
 */
public record SyncMotdPacket(String motdText) implements IPacket {

    public SyncMotdPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
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
}
