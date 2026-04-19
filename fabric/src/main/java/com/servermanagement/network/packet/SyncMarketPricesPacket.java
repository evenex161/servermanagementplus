package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;
import java.util.Map;

/**
 * Packet sent from server to client to sync dynamic market pricing data.
 * Updates ClientMarketData cache so MineBay can display base prices and margins.
 * Includes supply/demand data so client prices match server prices.
 */
public record SyncMarketPricesPacket(double inflationMultiplier, double averageBalance, int totalPlayerCount, double starterMoney, Map<String, Long> supplyData, Map<String, Double> recipePrices) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncMarketPricesPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_market_prices_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMarketPricesPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMarketPricesPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncMarketPricesPacket {
        if (supplyData == null) supplyData = new HashMap<>();
        if (recipePrices == null) recipePrices = new HashMap<>();
    }
    public SyncMarketPricesPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readDouble(),
             decodeSupplyData(buf), decodeRecipePrices(buf));
    }

    private static Map<String, Long> decodeSupplyData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(256), buf.readLong());
        }
        return map;
    }

    private static Map<String, Double> decodeRecipePrices(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Double> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(256), buf.readDouble());
        }
        return map;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(inflationMultiplier);
        buf.writeDouble(averageBalance);
        buf.writeInt(totalPlayerCount);
        buf.writeDouble(starterMoney);
        buf.writeVarInt(supplyData.size());
        for (Map.Entry<String, Long> entry : supplyData.entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeLong(entry.getValue());
        }
        buf.writeVarInt(recipePrices.size());
        for (Map.Entry<String, Double> entry : recipePrices.entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeDouble(entry.getValue());
        }
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            com.servermanagement.client.ClientMarketData.update(
                inflationMultiplier, averageBalance, totalPlayerCount, starterMoney, supplyData, recipePrices
            );

}
}
