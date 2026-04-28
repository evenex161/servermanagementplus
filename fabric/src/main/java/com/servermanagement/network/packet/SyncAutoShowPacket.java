package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public record SyncAutoShowPacket(boolean autoShow) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_auto_show_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncAutoShowPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Handle on client - update UI

}

    public boolean isAutoShow() {
        return autoShow;
    }
}
