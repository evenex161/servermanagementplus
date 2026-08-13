package com.servermanagement.network.packet;

import com.servermanagement.config.ModConfig;
import com.servermanagement.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements CustomPacketPayload {
    public static final Type<SaveEconomySettingsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "save_economy_settings_packet"));
    
    public static final StreamCodec<FriendlyByteBuf, SaveEconomySettingsPacket> STREAM_CODEC = StreamCodec.ofMember(
        SaveEconomySettingsPacket::write, SaveEconomySettingsPacket::new
    );

    public SaveEconomySettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(32767), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(showMarketValueTooltips);
        buf.writeBoolean(minebayEnabled);
        buf.writeBoolean(minestacksEnabled);
        buf.writeUtf(tradeBlacklist, 32767);
        buf.writeDouble(startingBalance);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                ModConfig.SHOW_MARKET_VALUE_TOOLTIPS.set(showMarketValueTooltips);
                ModConfig.MINEBAY_ENABLED.set(minebayEnabled);
                ModConfig.MINESTACKS_ENABLED.set(minestacksEnabled);
                ModConfig.TRADE_BLACKLIST.set(tradeBlacklist);
                ModConfig.STARTING_BALANCE.set(startingBalance);
                ModConfig.SPEC.save();
                
                ModNetworking.sendToAllPlayers(new SyncEconomySettingsPacket(showMarketValueTooltips, minebayEnabled, minestacksEnabled, tradeBlacklist, startingBalance));
            }
        });
    }
}
