package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTask;
import com.servermanagement.features.economy.TaskType;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.function.Supplier;

/**
 * Packet to sync daily tasks from server to client
 */
public record SyncDailyTasksPacket(List<DailyTask> tasks, long resetTime, boolean freeRewardAvailable, int freeRewardAmount, long timeUntilFreeReward, List<ItemStack> freeRewardItems) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SyncDailyTasksPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "sync_daily_tasks_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncDailyTasksPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncDailyTasksPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        this(decodeTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buf));
    }

    private static List<DailyTask> decodeTasks(FriendlyByteBuf buf) {
        int taskCount = buf.readInt();
        List<DailyTask> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            String id = buf.readUtf(64);
            int compCount = buf.readInt();
            List<com.servermanagement.features.economy.TaskComponent> components = new java.util.ArrayList<>();
            for (int j = 0; j < compCount; j++) {
                components.add(new com.servermanagement.features.economy.TaskComponent(
                    com.servermanagement.features.economy.TaskType.values()[buf.readInt()],
                    buf.readInt(),
                    buf.readUtf(100)
                ));
            }
            int currentStep = buf.readInt();
            int progress = buf.readInt();
            boolean claimed = buf.readBoolean();
            double reward = buf.readDouble();
            List<net.minecraft.world.item.ItemStack> rewardItems = net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buf);
            
            DailyTask task = new DailyTask(id, components, reward, rewardItems);
            task.setCurrentStep(currentStep);
            task.setProgress(progress);
            task.setClaimed(claimed);
            tasks.add(task);
        }
        return tasks;
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(tasks.size());
        
        for (DailyTask task : tasks) {
            buf.writeUtf(task.getId() != null ? task.getId() : "", 64);
            buf.writeInt(task.getComponents().size());
            for (com.servermanagement.features.economy.TaskComponent comp : task.getComponents()) {
                buf.writeInt(comp.getType().ordinal());
                buf.writeInt(comp.getTargetAmount());
                buf.writeUtf(comp.getCustomDescription() != null ? comp.getCustomDescription() : "", 100);
            }
            buf.writeInt(task.getCurrentStep());
            buf.writeInt(task.getProgress());
            buf.writeBoolean(task.isClaimed());
            buf.writeDouble(task.getRewardAmount());
            net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buf, task.getRewardItems());
        }
        
        buf.writeLong(resetTime);
        buf.writeBoolean(freeRewardAvailable);
        buf.writeInt(freeRewardAmount);
        buf.writeLong(timeUntilFreeReward);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buf, freeRewardItems);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            // Store daily tasks on client side for GUI display
            com.servermanagement.client.ClientDailyTasksData.setTasks(tasks);
            com.servermanagement.client.ClientDailyTasksData.setResetTime(resetTime);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAvailable(freeRewardAvailable);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardAmount(freeRewardAmount);
            com.servermanagement.client.ClientDailyTasksData.setTimeUntilFreeReward(timeUntilFreeReward);
            com.servermanagement.client.ClientDailyTasksData.setFreeRewardItems(freeRewardItems);
            com.servermanagement.client.ClientPacketHandler.refreshOpenScreen();

}
}
