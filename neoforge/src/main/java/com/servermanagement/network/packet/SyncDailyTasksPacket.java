package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet to sync daily tasks from server to client
 */
public record SyncDailyTasksPacket(List<DailyTask> tasks, long resetTime, boolean freeRewardAvailable, int freeRewardAmount, long timeUntilFreeReward) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncDailyTasksPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_daily_tasks"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncDailyTasksPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncDailyTasksPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        this(decodeTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(tasks.size());
        
        for (DailyTask task : tasks) {
            buf.writeInt(task.getType().ordinal());
            buf.writeInt(task.getGoal());
            buf.writeInt(task.getProgress());
            buf.writeBoolean(task.isClaimed());
            buf.writeInt(task.getReward());
            buf.writeUtf(task.getDescription(), 256);
        }
        
        buf.writeLong(resetTime);
        buf.writeBoolean(freeRewardAvailable);
        buf.writeInt(freeRewardAmount);
        buf.writeLong(timeUntilFreeReward);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store daily tasks on client side for GUI display
            com.servermanagement.client.ClientDailyTasksData.setTasks(tasks);
            com.servermanagement.client.ClientDailyTasksData.setResetTime(resetTime);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAvailable(freeRewardAvailable);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAmount(freeRewardAmount);
            com.servermanagement.client.ClientDailyTasksData.setTimeUntilFreeReward(timeUntilFreeReward);
        });
        // packet handled
    }

    private static List<DailyTask> decodeTasks(FriendlyByteBuf buf) {
        int taskCount = buf.readInt();
        List<DailyTask> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            TaskType type = TaskType.values()[buf.readInt()];
            int goal = buf.readInt();
            int progress = buf.readInt();
            boolean claimed = buf.readBoolean();
            int reward = buf.readInt();
            String description = buf.readUtf(256);
            DailyTask task = new DailyTask(type, goal, reward, description);
            task.setProgress(progress);
            task.setClaimed(claimed);
            tasks.add(task);
        }
        return tasks;
    }
}
