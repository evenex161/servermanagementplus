package com.servermanagement.network.packet;

import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet sent from server to client to sync dynamic market pricing data.
 * Updates ClientMarketData cache so MineBay can display base prices and margins.
 * Includes supply/demand data so client prices match server prices.
 */
public record SyncMarketPricesPacket(double inflationMultiplier, double averageBalance, int totalPlayerCount,
                                      double starterMoney, Map<String, Long> supplyData,
                                      Map<String, Double> recipePrices) implements IPacket {

    public SyncMarketPricesPacket {
        supplyData = supplyData != null ? supplyData : new HashMap<>();
        recipePrices = recipePrices != null ? recipePrices : new HashMap<>();
    }

    public SyncMarketPricesPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readDouble(),
             readSupplyData(buf), readRecipePrices(buf));
    }

    private static Map<String, Long> readSupplyData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(32767), buf.readLong());
        }
        return map;
    }

    private static Map<String, Double> readRecipePrices(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Double> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(32767), buf.readDouble());
        }
        return map;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(inflationMultiplier);
        buf.writeDouble(averageBalance);
        buf.writeInt(totalPlayerCount);
        buf.writeDouble(starterMoney);
        buf.writeVarInt(supplyData.size());
        for (Map.Entry<String, Long> entry : supplyData.entrySet()) {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeLong(entry.getValue());
        }
        buf.writeVarInt(recipePrices.size());
        for (Map.Entry<String, Double> entry : recipePrices.entrySet()) {
            buf.writeUtf(entry.getKey(), 32767);
            buf.writeDouble(entry.getValue());
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.servermanagement.client.ClientMarketData.update(
                inflationMultiplier, averageBalance, totalPlayerCount, starterMoney, supplyData, recipePrices
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
