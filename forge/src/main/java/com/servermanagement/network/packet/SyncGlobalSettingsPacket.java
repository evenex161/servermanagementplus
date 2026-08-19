package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public record SyncGlobalSettingsPacket(boolean chatIsolationEnabled, boolean tabIsolationEnabled) implements IPacket {

    public SyncGlobalSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(chatIsolationEnabled);
        buf.writeBoolean(tabIsolationEnabled);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            ClientPacketHandler.handleGlobalSettings(chatIsolationEnabled, tabIsolationEnabled);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
