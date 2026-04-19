package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class ToggleAutoShowPacket implements IPacket {
    private final boolean autoShow;

    public ToggleAutoShowPacket(boolean autoShow) {
        this.autoShow = autoShow;
    }

    public ToggleAutoShowPacket(FriendlyByteBuf buf) {
        this.autoShow = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null) {
                // Save player preference
                com.servermanagement.config.PlayerPreferences.setAutoShow(player.getUUID(), autoShow);
            }
        });
        ctx.setPacketHandled(true);
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
