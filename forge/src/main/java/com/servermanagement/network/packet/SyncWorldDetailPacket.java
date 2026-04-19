package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class SyncWorldDetailPacket implements IPacket {
    private final String dimensionId;
    private final boolean netherPortalsEnabled;
    private final boolean endPortalsEnabled;
    private final boolean hasTimer;
    private final int timerSeconds;
    private final boolean chatConnected;
    private final String timerPortalType;

    public SyncWorldDetailPacket(String dimensionId, boolean netherPortalsEnabled, boolean endPortalsEnabled,
                                 boolean hasTimer, int timerSeconds, boolean chatConnected, String timerPortalType) {
        this.dimensionId = dimensionId;
        this.netherPortalsEnabled = netherPortalsEnabled;
        this.endPortalsEnabled = endPortalsEnabled;
        this.hasTimer = hasTimer;
        this.timerSeconds = timerSeconds;
        this.chatConnected = chatConnected;
        this.timerPortalType = timerPortalType;
    }

    public SyncWorldDetailPacket(FriendlyByteBuf buf) {
        this.dimensionId = buf.readUtf(256);
        this.netherPortalsEnabled = buf.readBoolean();
        this.endPortalsEnabled = buf.readBoolean();
        this.hasTimer = buf.readBoolean();
        this.timerSeconds = buf.readInt();
        this.chatConnected = buf.readBoolean();
        this.timerPortalType = buf.readUtf(32);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimensionId, 256);
        buf.writeBoolean(netherPortalsEnabled);
        buf.writeBoolean(endPortalsEnabled);
        buf.writeBoolean(hasTimer);
        buf.writeInt(timerSeconds);
        buf.writeBoolean(chatConnected);
        buf.writeUtf(timerPortalType, 32);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client
            com.servermanagement.client.ClientPacketHandler.handleWorldDetail(
                dimensionId, netherPortalsEnabled, endPortalsEnabled,
                hasTimer, timerSeconds, chatConnected, timerPortalType
            );
        });
        ctx.setPacketHandled(true);
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public boolean isNetherPortalsEnabled() {
        return netherPortalsEnabled;
    }

    public boolean isEndPortalsEnabled() {
        return endPortalsEnabled;
    }

    public boolean hasTimer() {
        return hasTimer;
    }

    public int getTimerSeconds() {
        return timerSeconds;
    }

    public boolean isChatConnected() {
        return chatConnected;
    }

    public String getTimerPortalType() {
        return timerPortalType;
    }
}
