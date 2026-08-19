package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;

public record SyncWorldDetailPacket(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled,
                                     boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) implements IPacket {

    public SyncWorldDetailPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readUtf(32767));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 32767);
        buf.writeBoolean(netherPortalsEnabled);
        buf.writeBoolean(endPortalsEnabled);
        buf.writeBoolean(hasTimer);
        buf.writeInt(timerSeconds);
        buf.writeBoolean(chatConnected);
        buf.writeUtf(timerPortalType, 32767);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            // Handle on client
            com.servermanagement.client.ClientPacketHandler.handleWorldDetail(
                dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType
            );
            // If the open screen is the WorldDetailScreen for this dimension,
            // rebuild widgets immediately so the new timer/portal state is
            // reflected without waiting for the user to close & reopen.
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.servermanagement.gui.screen.WorldDetailScreen wds) {
                wds.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
