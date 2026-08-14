package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public record WMToggleChatIsolationPacket(String dimensionId, boolean enabled, long clientTick) implements IPacket {

    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                String actionKey = "chat_isolation_" + dimensionId;
                if (com.servermanagement.network.PacketTimestampTracker.shouldProcessPacket(player, actionKey, clientTick)) {
                    var worldManager = com.servermanagement.features.worldmanager.WorldManager.getInstance();
                    if (dimensionId.isEmpty()) {
                        // Global master toggle (from Global Settings screen)
                        worldManager.getData().setChatIsolationEnabled(enabled);
                    } else {
                        // Per-dimension chat connection toggle (from World Detail screen)
                        worldManager.getData().setDimensionChatConnected(dimensionId, enabled);
                    }
                    worldManager.save();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
