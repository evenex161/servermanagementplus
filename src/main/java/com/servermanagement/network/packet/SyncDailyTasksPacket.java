package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to sync daily tasks from server to client
 */
public class SyncDailyTasksPacket implements IPacket {
    private final List<DailyTask> tasks;
    private final long resetTime;
    private final boolean freeRewardAvailable;
    private final int freeRewardAmount;
    private final long timeUntilFreeReward;

    public SyncDailyTasksPacket(List<DailyTask> tasks, long resetTime, boolean freeRewardAvailable, 
                                int freeRewardAmount, long timeUntilFreeReward) {
        this.tasks = tasks;
        this.resetTime = resetTime;
        this.freeRewardAvailable = freeRewardAvailable;
        this.freeRewardAmount = freeRewardAmount;
        this.timeUntilFreeReward = timeUntilFreeReward;
    }

    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        int taskCount = buf.readInt();
        this.tasks = new ArrayList<>();
        
        for (int i = 0; i < taskCount; i++) {
            // Read task type
            TaskType type = TaskType.values()[buf.readInt()];
            int goal = buf.readInt();
            int progress = buf.readInt();
            boolean claimed = buf.readBoolean();
            int reward = buf.readInt();
            String description = buf.readUtf(256);
            
            // Create task
            DailyTask task = new DailyTask(type, goal, reward, description);
            task.setProgress(progress);
            task.setClaimed(claimed);
            
            this.tasks.add(task);
        }
        
        this.resetTime = buf.readLong();
        this.freeRewardAvailable = buf.readBoolean();
        this.freeRewardAmount = buf.readInt();
        this.timeUntilFreeReward = buf.readLong();
    }

    @Override
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

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Store daily tasks on client side for GUI display
            com.servermanagement.client.ClientDailyTasksData.setTasks(tasks);
            com.servermanagement.client.ClientDailyTasksData.setResetTime(resetTime);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAvailable(freeRewardAvailable);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAmount(freeRewardAmount);
            com.servermanagement.client.ClientDailyTasksData.setTimeUntilFreeReward(timeUntilFreeReward);
        });
        ctx.setPacketHandled(true);
    }
}
