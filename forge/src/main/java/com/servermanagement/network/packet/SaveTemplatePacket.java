package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplate;
import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.TaskType;
import com.servermanagement.features.economy.TaskComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;

public record SaveTemplatePacket(String templateId, List<TaskComponent> components, int rewardAmount, List<ItemStack> rewardItems) implements IPacket {

    public SaveTemplatePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(64), readComponents(buf), buf.readInt(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
    }
    
    private static List<TaskComponent> readComponents(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<TaskComponent> list = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            list.add(new TaskComponent(TaskType.values()[buf.readInt()], buf.readInt(), buf.readUtf(100)));
        }
        return list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId != null ? templateId : "", 64);
        buf.writeInt(components.size());
        for (TaskComponent comp : components) {
            buf.writeInt(comp.getType().ordinal());
            buf.writeInt(comp.getTargetAmount());
            buf.writeUtf(comp.getCustomDescription() != null ? comp.getCustomDescription() : "", 100);
        }
        buf.writeInt(rewardAmount);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItems);
    }

    @Override
    public void handle(net.minecraftforge.event.network.CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            int safeRewardAmount = Math.max(0, Math.min(rewardAmount, 100000));
            
            List<TaskComponent> safeComponents = new ArrayList<>();
            for(TaskComponent c : components) {
                if (safeComponents.size() >= 10) break;
                safeComponents.add(new TaskComponent(c.getType(), Math.max(1, Math.min(c.getTargetAmount(), 10000)), c.getCustomDescription()));
            }

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            if (templateId == null || templateId.isEmpty()) {
                DailyTaskTemplate template = new DailyTaskTemplate(safeComponents, safeRewardAmount, rewardItems);
                templateManager.addTemplate(template);
            } else {
                DailyTaskTemplate existing = templateManager.getTemplate(templateId);
                if (existing != null) {
                    existing.setComponents(safeComponents);
                    existing.setRewardAmount(safeRewardAmount);
                    existing.setRewardItems(rewardItems);
                }
            }

            templateManager.save(server);
            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        context.setPacketHandled(true);
    }
}
