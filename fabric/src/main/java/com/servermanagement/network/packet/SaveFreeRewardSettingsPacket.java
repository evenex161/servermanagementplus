package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/**
 * Client-to-server packet for saving free reward settings
 */
public record SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, ItemStack rewardItem) implements com.servermanagement.network.IPacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "save_free_reward_settings_packet");

    @Override
    public net.minecraft.resources.ResourceLocation id() { return ID; }


    public SaveFreeRewardSettingsPacket {
        if (rewardItem == null) rewardItem = ItemStack.EMPTY;
    }
    public SaveFreeRewardSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readItem());
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
        buf.writeItem(rewardItem);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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
            templateManager.setFreeRewardItem(rewardItem);
            
            templateManager.save(server);

}
}
