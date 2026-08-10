package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import com.servermanagement.config.ModConfig;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;

public record SaveEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist) implements IPacket {
    public SaveEconomySettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(32767));
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(showMarketValueTooltips);
        buf.writeBoolean(minebayEnabled);
        buf.writeBoolean(minestacksEnabled);
        buf.writeUtf(tradeBlacklist, 32767);
    }
    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.set(showMarketValueTooltips);
                ModConfig.MINEBAY_ENABLED.set(minebayEnabled);
                ModConfig.MINESTACKS_ENABLED.set(minestacksEnabled);
                ModConfig.TRADE_BLACKLIST.set(tradeBlacklist);
                ModConfig.SPEC.save();
                
                // Broadcast to all clients
                ModNetworking.sendToAllPlayers(new SyncEconomySettingsPacket(showMarketValueTooltips, minebayEnabled, minestacksEnabled, tradeBlacklist));
            }
        });
        ctx.setPacketHandled(true);
    }
}


