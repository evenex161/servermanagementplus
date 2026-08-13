package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncEconomySettingsPacket(boolean showMarketValueTooltips, boolean minebayEnabled, boolean minestacksEnabled, String tradeBlacklist, double startingBalance) implements CustomPacketPayload {
    public static final Type<SyncEconomySettingsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_economy_settings_packet"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncEconomySettingsPacket> STREAM_CODEC = StreamCodec.ofMember(
        SyncEconomySettingsPacket::write, SyncEconomySettingsPacket::new
    );

    public SyncEconomySettingsPacket(FriendlyByteBuf buf) {
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
            ClientPacketHandler.setShowMarketValueTooltips(showMarketValueTooltips);
            ClientPacketHandler.setMinebayEnabled(minebayEnabled);
            ClientPacketHandler.setMinestacksEnabled(minestacksEnabled);
            ClientPacketHandler.setTradeBlacklist(tradeBlacklist);
            ClientPacketHandler.setStartingBalance(startingBalance);
        });
    }
}
