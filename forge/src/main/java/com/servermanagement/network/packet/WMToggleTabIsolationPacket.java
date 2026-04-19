package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record WMToggleTabIsolationPacket(boolean enabled, long clientTick) implements IPacket {

    public WMToggleTabIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                // Check if this packet should be processed (timestamp validation)
                String actionKey = "tab_isolation";
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    com.servermanagement.features.worldmanager.WorldManager.setTabIsolationEnabled(enabled);
                    // Immediately apply or restore tab isolation
                    var server = player.getServer();
                    if (server != null) {
                        com.servermanagement.features.worldmanager.TabListIsolationHandler.onIsolationToggled(server);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
