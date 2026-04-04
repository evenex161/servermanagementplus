package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet for saving free reward settings
 */
public class SaveFreeRewardSettingsPacket implements IPacket {
    private final int rewardAmount;
    private final int cooldownHours;
    private final ItemStack rewardItem;

    public SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, ItemStack rewardItem) {
        this.rewardAmount = rewardAmount;
        this.cooldownHours = cooldownHours;
        this.rewardItem = rewardItem != null ? rewardItem : ItemStack.EMPTY;
    }

    public SaveFreeRewardSettingsPacket(FriendlyByteBuf buf) {
        this.rewardAmount = buf.readInt();
        this.cooldownHours = buf.readInt();
        this.rewardItem = buf.readItem();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
        buf.writeItem(rewardItem);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            if (rewardAmount > 0) {
                templateManager.setFreeRewardAmount(rewardAmount);
            }
            if (cooldownHours > 0) {
                templateManager.setFreeRewardCooldownHours(cooldownHours);
            }
            templateManager.setFreeRewardItem(rewardItem);
            
            templateManager.save(server);
        });
        ctx.get().setPacketHandled(true);
    }
}
