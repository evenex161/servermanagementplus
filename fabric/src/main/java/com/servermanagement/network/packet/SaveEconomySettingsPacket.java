package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.config.ModConfig;
import com.servermanagement.network.ModNetworking;

/**
 * Client-to-server packet for saving economy settings (tooltips, minebay, minestacks, blacklist, starting balance).
 */
public record SaveEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements com.servermanagement.network.IPacket {
    public static final ResourceLocation ID = new ResourceLocation("servermanagement", "save_economy_settings_packet");

    @Override
    public ResourceLocation id() { return ID; }

    public SaveEconomySettingsPacket(FriendlyByteBuf buf) {
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

    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.set(showMarketValueTooltips);
            ModConfig.MINEBAY_ENABLED.set(minebayEnabled);
            ModConfig.MINESTACKS_ENABLED.set(minestacksEnabled);
            ModConfig.TRADE_BLACKLIST.set(tradeBlacklist);
            ModConfig.STARTING_BALANCE.set(startingBalance);
            ModConfig.saveConfig();

            // Broadcast updated settings to all connected clients
            ModNetworking.sendToAllPlayers(new SyncEconomySettingsPacket(showMarketValueTooltips, minebayEnabled, minestacksEnabled, tradeBlacklist, startingBalance));
        }
    }
}
