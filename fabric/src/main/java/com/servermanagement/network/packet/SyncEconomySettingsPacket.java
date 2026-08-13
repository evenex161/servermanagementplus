package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import com.servermanagement.client.ClientPacketHandler;

public record SyncEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncEconomySettingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_economy_settings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncEconomySettingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncEconomySettingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncEconomySettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(32767), buf.readDouble());
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(showMarketValueTooltips);
        buf.writeBoolean(minebayEnabled);
        buf.writeBoolean(minestacksEnabled);
        buf.writeUtf(tradeBlacklist, 32767);
        buf.writeDouble(startingBalance);
    }
    public void handle(net.minecraft.world.entity.player.Player player) {
        ClientPacketHandler.setShowMarketValueTooltips(showMarketValueTooltips);
        ClientPacketHandler.setMinebayEnabled(minebayEnabled);
        ClientPacketHandler.setMinestacksEnabled(minestacksEnabled);
        ClientPacketHandler.setTradeBlacklist(tradeBlacklist);
        ClientPacketHandler.setStartingBalance(startingBalance);
    }
}
