package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMToggleTabIsolationPacket(boolean enabled, long clientTick) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_toggle_tab_isolation_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public WMToggleTabIsolationPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeLong(clientTick);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
