package com.servermanagement.network.packet;

import com.servermanagement.network.packet.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from server to client to sync dynamic market pricing data.
 * Updates ClientMarketData cache so MineBay can display base prices and margins.
 */
public class SyncMarketPricesPacket implements IPacket {
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
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            com.servermanagement.client.ClientMarketData.update(
                inflationMultiplier, averageBalance, totalPlayerCount, starterMoney
            );
        });
        ctx.setPacketHandled(true);
    }
}
