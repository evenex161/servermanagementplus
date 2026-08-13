package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import com.servermanagement.client.ClientPacketHandler;

public record SyncEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements IPacket {
    public SyncEconomySettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(32767), buf.readDouble());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(showMarketValueTooltips);
        buf.writeBoolean(minebayEnabled);
        buf.writeBoolean(minestacksEnabled);
        buf.writeUtf(tradeBlacklist, 32767);
        buf.writeDouble(startingBalance);
    }
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.setShowMarketValueTooltips(showMarketValueTooltips);
            ClientPacketHandler.setMinebayEnabled(minebayEnabled);
            ClientPacketHandler.setMinestacksEnabled(minestacksEnabled);
            ClientPacketHandler.setTradeBlacklist(tradeBlacklist);
            ClientPacketHandler.setStartingBalance(startingBalance);
        });
        ctx.setPacketHandled(true);
    }
}
