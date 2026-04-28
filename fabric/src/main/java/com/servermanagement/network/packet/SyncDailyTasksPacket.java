package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to sync daily tasks from server to client
 */
public record SyncDailyTasksPacket(List<DailyTask> tasks, long resetTime, boolean freeRewardAvailable, int freeRewardAmount, long timeUntilFreeReward) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "sync_daily_tasks_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        this(decodeTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong());
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

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Store daily tasks on client side for GUI display
            com.servermanagement.client.ClientDailyTasksData.setTasks(tasks);
            com.servermanagement.client.ClientDailyTasksData.setResetTime(resetTime);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAvailable(freeRewardAvailable);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAmount(freeRewardAmount);
            com.servermanagement.client.ClientDailyTasksData.setTimeUntilFreeReward(timeUntilFreeReward);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}
}
