package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.servermanagement.client.ClientPacketHandler;

import java.util.function.Supplier;

/**
 * Server-to-client packet that syncs economy settings (tooltips, minebay, minestacks, blacklist, starting balance).
 */
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
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            ClientPacketHandler.setShowMarketValueTooltips(showMarketValueTooltips);
            ClientPacketHandler.setMinebayEnabled(minebayEnabled);
            ClientPacketHandler.setMinestacksEnabled(minestacksEnabled);
            ClientPacketHandler.setTradeBlacklist(tradeBlacklist);
            ClientPacketHandler.setStartingBalance(startingBalance);
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
