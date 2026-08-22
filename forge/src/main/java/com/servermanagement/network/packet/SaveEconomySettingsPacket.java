package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.servermanagement.config.ModConfig;
import com.servermanagement.network.ModNetworking;

import java.util.function.Supplier;

/**
 * Client-to-server packet for saving economy settings (tooltips, minebay, minestacks, blacklist, starting balance).
 */
public record SaveEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements IPacket {
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

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.set(showMarketValueTooltips);
                ModConfig.MINEBAY_ENABLED.set(minebayEnabled);
                ModConfig.MINESTACKS_ENABLED.set(minestacksEnabled);
                ModConfig.TRADE_BLACKLIST.set(tradeBlacklist);
                ModConfig.STARTING_BALANCE.set(startingBalance);
                ModConfig.SPEC.save();

                // Force-clear all caches so subsequent .get() calls read the updated values
                // rather than potentially stale cached data from ForgeConfigSpec's autoreload
                ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.clearCache();
                ModConfig.MINEBAY_ENABLED.clearCache();
                ModConfig.MINESTACKS_ENABLED.clearCache();
                ModConfig.TRADE_BLACKLIST.clearCache();
                ModConfig.STARTING_BALANCE.clearCache();

                // Broadcast updated settings to all connected clients
                ModNetworking.sendToAllPlayers(new SyncEconomySettingsPacket(showMarketValueTooltips, minebayEnabled, minestacksEnabled, tradeBlacklist, startingBalance));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
