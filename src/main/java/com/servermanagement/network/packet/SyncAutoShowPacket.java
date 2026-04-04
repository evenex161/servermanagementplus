package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client - update UI
        });
        ctx.get().setPacketHandled(true);
    }

    public boolean isAutoShow() {
        return autoShow;
    }
}
