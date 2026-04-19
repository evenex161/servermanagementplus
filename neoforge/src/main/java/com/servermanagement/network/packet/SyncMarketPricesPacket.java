package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet sent from server to client to sync dynamic market pricing data.
 * Updates ClientMarketData cache so MineBay can display base prices and margins.
 * Includes supply/demand data so client prices match server prices.
 */
public class SyncMarketPricesPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncMarketPricesPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_market_prices"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncMarketPricesPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMarketPricesPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final double inflationMultiplier;
    private final double averageBalance;
    private final int totalPlayerCount;
    private final double starterMoney;
    private final Map<String, Long> supplyData;
    private final Map<String, Double> recipePrices;

    public SyncMarketPricesPacket(double inflationMultiplier, double averageBalance, int totalPlayerCount, double starterMoney, Map<String, Long> supplyData, Map<String, Double> recipePrices) {
        this.inflationMultiplier = inflationMultiplier;
        this.averageBalance = averageBalance;
        this.totalPlayerCount = totalPlayerCount;
        this.starterMoney = starterMoney;
        this.supplyData = supplyData != null ? supplyData : new HashMap<>();
        this.recipePrices = recipePrices != null ? recipePrices : new HashMap<>();
    }

    public SyncMarketPricesPacket(FriendlyByteBuf buf) {
        this.inflationMultiplier = buf.readDouble();
        this.averageBalance = buf.readDouble();
        this.totalPlayerCount = buf.readInt();
        this.starterMoney = buf.readDouble();
        int supplySize = buf.readVarInt();
        this.supplyData = new HashMap<>(supplySize);
        for (int i = 0; i < supplySize; i++) {
            String itemId = buf.readUtf(256);
            long count = buf.readLong();
            this.supplyData.put(itemId, count);
        }
        int recipeSize = buf.readVarInt();
        this.recipePrices = new HashMap<>(recipeSize);
        for (int i = 0; i < recipeSize; i++) {
            String itemId = buf.readUtf(256);
            double price = buf.readDouble();
            this.recipePrices.put(itemId, price);
        }
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

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            com.servermanagement.client.ClientMarketData.update(
                inflationMultiplier, averageBalance, totalPlayerCount, starterMoney, supplyData, recipePrices
            );
        });
        // packet handled
    }
}
