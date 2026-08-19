package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMToggleChatIsolationPacket(String dimensionId, boolean enabled, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_toggle_chat_isolation_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public WMToggleChatIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
