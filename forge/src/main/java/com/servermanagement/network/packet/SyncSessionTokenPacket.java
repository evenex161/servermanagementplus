package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Server → Client: Transmits the generated session token to the client.
 */
public record SyncSessionTokenPacket(String token) implements IPacket {
    public SyncSessionTokenPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(token);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            ClientPacketHandler.handleSessionTokenSync(token);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
