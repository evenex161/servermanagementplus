package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class SyncAutoShowPacket implements IPacket {
    private final boolean autoShow;

    public SyncAutoShowPacket(boolean autoShow) {
        this.autoShow = autoShow;
    }

    public SyncAutoShowPacket(FriendlyByteBuf buf) {
        this.autoShow = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoShow);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client - update UI
        });
        ctx.setPacketHandled(true);
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
