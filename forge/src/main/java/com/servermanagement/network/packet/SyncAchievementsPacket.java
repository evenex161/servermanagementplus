package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet to sync achievements from server to client
 */
public record SyncAchievementsPacket(Set<String> earnedAchievements, int totalRewards) implements IPacket {

    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        this(readAchievements(buf), buf.readInt());
    }

    private static Set<String> readAchievements(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(buf.readUtf(32767));
        }
        return set;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(earnedAchievements.size());
        
        for (String achievementId : earnedAchievements) {
            buf.writeUtf(achievementId, 32767);
        }
        
        buf.writeInt(totalRewards);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);
        });
        ctx.get().setPacketHandled(true);
    }
}
