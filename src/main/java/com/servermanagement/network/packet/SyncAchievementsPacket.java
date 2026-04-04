package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet to sync achievements from server to client
 */
public class SyncAchievementsPacket implements IPacket {
    private final Set<String> earnedAchievements;
    private final int totalRewards;

    public SyncAchievementsPacket(Set<String> earnedAchievements, int totalRewards) {
        this.earnedAchievements = earnedAchievements;
        this.totalRewards = totalRewards;
    }

    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.earnedAchievements = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            this.earnedAchievements.add(buf.readUtf(128));
        }
        
        this.totalRewards = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(earnedAchievements.size());
        
        for (String achievementId : earnedAchievements) {
            buf.writeUtf(achievementId, 128);
        }
        
        buf.writeInt(totalRewards);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);
        });
        ctx.setPacketHandled(true);
    }
}
