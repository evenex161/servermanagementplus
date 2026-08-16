package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet for saving free reward settings
 */
public record SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, List<ItemStack> rewardItems) implements IPacket {

    public SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, List<ItemStack> rewardItems) {
        this.rewardAmount = rewardAmount;
        this.cooldownHours = cooldownHours;
        this.rewardItems = rewardItems != null ? rewardItems : new ArrayList<>();
    }

    public SaveFreeRewardSettingsPacket(FriendlyByteBuf buf) {
                this(buf.readInt(), buf.readInt(), decodeItems(buf));
    }
    private static List<ItemStack> decodeItems(net.minecraft.network.FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        }
        return items;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
                buf.writeInt(rewardItems.size());
        for (ItemStack item : rewardItems) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, item);
        }
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            var server = player.getServer();
            if (server == null) return;

            var economyManager = EconomyManager.getInstance(server);
            DailyTaskTemplateManager templateManager = economyManager.getTemplateManager();

            if (rewardAmount > 0) {
                templateManager.setFreeRewardAmount(Math.min(rewardAmount, 100000));
            }
            if (cooldownHours > 0) {
                templateManager.setFreeRewardCooldownHours(Math.min(cooldownHours, 720));
            }
            templateManager.setFreeRewardItems(rewardItems);
            
            templateManager.save(server);
            SyncEconomyTemplatesPacket.syncToPlayer(player, server);
        });
        ctx.setPacketHandled(true);
    }
}
