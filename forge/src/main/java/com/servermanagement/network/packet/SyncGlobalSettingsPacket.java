package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs global settings (chat/tab isolation)
 */
public class SyncGlobalSettingsPacket implements IPacket {
    private final boolean chatIsolationEnabled;
    private final boolean tabIsolationEnabled;

    public SyncGlobalSettingsPacket(boolean chatIsolationEnabled, boolean tabIsolationEnabled) {
        this.chatIsolationEnabled = chatIsolationEnabled;
        this.tabIsolationEnabled = tabIsolationEnabled;
    }

    public SyncGlobalSettingsPacket(FriendlyByteBuf buf) {
        this.chatIsolationEnabled = buf.readBoolean();
        this.tabIsolationEnabled = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(chatIsolationEnabled);
        buf.writeBoolean(tabIsolationEnabled);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleGlobalSettings(chatIsolationEnabled, tabIsolationEnabled);
        });
        ctx.get().setPacketHandled(true);
    }
}
