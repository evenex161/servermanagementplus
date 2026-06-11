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
public record SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, ItemStack rewardItem) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SaveFreeRewardSettingsPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "save_free_reward_settings_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveFreeRewardSettingsPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveFreeRewardSettingsPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public SaveFreeRewardSettingsPacket {
        if (rewardItem == null) rewardItem = ItemStack.EMPTY;
    }
    public SaveFreeRewardSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItem);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null || !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR)) return;

            var server = player.level().getServer();
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
