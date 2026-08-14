package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.function.Supplier;

/**
 * Packet to sync daily tasks from server to client
 */
public record SyncDailyTasksPacket(List<DailyTask> tasks, long resetTime, boolean freeRewardAvailable, int freeRewardAmount, long timeUntilFreeReward, List<ItemStack> freeRewardItems) implements IPacket {

    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        this(readTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buf));
    }

    private static List<DailyTask> readTasks(FriendlyByteBuf buf) {
        int taskCount = buf.readInt();
        List<DailyTask> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            TaskType type = TaskType.values()[buf.readInt()];
            int goal = buf.readInt();
            int progress = buf.readInt();
            boolean claimed = buf.readBoolean();
            int reward = buf.readInt();
            String description = buf.readUtf(256);
            List<ItemStack> rewardItems = ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buf);
            DailyTask task = new DailyTask(type, goal, reward, description);
            task.setRewardItems(rewardItems);
            task.setProgress(progress);
            task.setClaimed(claimed);
            tasks.add(task);
        }
        return tasks;
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
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buf, task.getRewardItems());
        }
        
        buf.writeLong(resetTime);
        buf.writeBoolean(freeRewardAvailable);
        buf.writeInt(freeRewardAmount);
        buf.writeLong(timeUntilFreeReward);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buf, freeRewardItems);
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
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardItems(freeRewardItems);
        });
        ctx.setPacketHandled(true);
    }
}
