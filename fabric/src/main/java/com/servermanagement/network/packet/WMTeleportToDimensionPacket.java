package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record WMTeleportToDimensionPacket(String dimensionId) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "w_m_teleport_to_dimension_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public WMTeleportToDimensionPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null && player.hasPermissions(2)) {
                com.servermanagement.features.worldmanager.WorldManager.teleportToDimension(player, dimensionId);
            }

}
}
