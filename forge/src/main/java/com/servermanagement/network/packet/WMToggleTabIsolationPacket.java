package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WMToggleTabIsolationPacket implements IPacket {
    private final boolean enabled;
    private final long clientTick;

    public WMToggleTabIsolationPacket(boolean enabled, long clientTick) {
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public WMToggleTabIsolationPacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "tab_isolation";
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setTabIsolationEnabled(enabled);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
