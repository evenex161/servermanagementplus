package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from server to client to sync dynamic market pricing data.
 * Updates ClientMarketData cache so MineBay can display base prices and margins.
 */
public class SyncMarketPricesPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncMarketPricesPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_market_prices"));

    public static final StreamCodec<FriendlyByteBuf, SyncMarketPricesPacket> STREAM_CODEC =
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMarketPricesPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final double inflationMultiplier;
    private final double averageBalance;
    private final int totalPlayerCount;
    private final double starterMoney;

    public SyncMarketPricesPacket(double inflationMultiplier, double averageBalance, int totalPlayerCount, double starterMoney) {
        this.inflationMultiplier = inflationMultiplier;
        this.averageBalance = averageBalance;
        this.totalPlayerCount = totalPlayerCount;
        this.starterMoney = starterMoney;
    }

    public SyncMarketPricesPacket(FriendlyByteBuf buf) {
        this.inflationMultiplier = buf.readDouble();
        this.averageBalance = buf.readDouble();
        this.totalPlayerCount = buf.readInt();
        this.starterMoney = buf.readDouble();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(inflationMultiplier);
        buf.writeDouble(averageBalance);
        buf.writeInt(totalPlayerCount);
        buf.writeDouble(starterMoney);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            com.servermanagement.client.ClientMarketData.update(
                inflationMultiplier, averageBalance, totalPlayerCount, starterMoney
            );
        });
    }
}
