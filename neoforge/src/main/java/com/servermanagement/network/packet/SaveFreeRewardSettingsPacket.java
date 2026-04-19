package com.servermanagement.network.packet;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * Client-to-server packet for saving free reward settings
 */
public record SaveFreeRewardSettingsPacket(int rewardAmount, int cooldownHours, ItemStack rewardItem) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveFreeRewardSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "save_free_reward_settings"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveFreeRewardSettingsPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveFreeRewardSettingsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }


    public SaveFreeRewardSettingsPacket {
        rewardItem = rewardItem != null ? rewardItem : ItemStack.EMPTY;
    }

    public SaveFreeRewardSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItem);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
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
        });
        // packet handled
    }
}
