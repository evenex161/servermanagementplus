package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class WMToggleChatIsolationPacket implements IPacket {
    private final String dimensionId;
    private final boolean enabled;
    private final long clientTick;

    public WMToggleChatIsolationPacket(String dimensionId, boolean enabled, long clientTick) {
        this.dimensionId = dimensionId;
        this.enabled = enabled;
        this.clientTick = clientTick;
    }

    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.enabled = buf.readBoolean();
        this.clientTick = buf.readLong();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
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
