package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet to sync achievements from server to client
 */
public class SyncAchievementsPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncAchievementsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_achievements_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncAchievementsPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncAchievementsPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Store achievements on client side for GUI display
            com.servermanagement.client.ClientAchievementsData.setEarnedAchievements(earnedAchievements);
            com.servermanagement.client.ClientAchievementsData.setTotalRewardsEarned(totalRewards);
        });
        
    }
}
