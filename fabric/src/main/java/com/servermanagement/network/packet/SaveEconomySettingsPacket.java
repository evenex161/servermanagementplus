package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import com.servermanagement.config.ModConfig;
import com.servermanagement.network.ModNetworking;

public record SaveEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SaveEconomySettingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "save_economy_settings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveEconomySettingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveEconomySettingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SaveEconomySettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(32767));
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(showMarketValueTooltips);
        buf.writeBoolean(minebayEnabled);
        buf.writeBoolean(minestacksEnabled);
        buf.writeUtf(tradeBlacklist, 32767);
    }
    public void handle(net.minecraft.server.level.ServerPlayer player) {
        if (player.hasPermissions(2)) {
            ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.set(showMarketValueTooltips);
            ModConfig.MINEBAY_ENABLED.set(minebayEnabled);
            ModConfig.MINESTACKS_ENABLED.set(minestacksEnabled);
            ModConfig.TRADE_BLACKLIST.set(tradeBlacklist);
            
            // Broadcast to all clients
            ModNetworking.sendToAllPlayers(new SyncEconomySettingsPacket(showMarketValueTooltips, minebayEnabled, minestacksEnabled, tradeBlacklist));
        }
    }
}


