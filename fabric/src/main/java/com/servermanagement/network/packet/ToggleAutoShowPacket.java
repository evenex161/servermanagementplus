package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record ToggleAutoShowPacket(boolean autoShow) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "toggle_auto_show_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public ToggleAutoShowPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player != null) {
                // Save player preference
                com.servermanagement.config.PlayerPreferences.setAutoShow(player.getUUID(), autoShow);
            }

}

    public boolean isAutoShow() {
        return autoShow;
    }
}
