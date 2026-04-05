package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.servermanagement.ServerManagementMod;

import com.servermanagement.features.economy.DailyTaskTemplateManager;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Client-to-server packet for saving free reward settings
 */
public class SaveFreeRewardSettingsPacket implements IPacket {
    public static final CustomPacketPayload.Type<SaveFreeRewardSettingsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "save_free_reward_settings_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveFreeRewardSettingsPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveFreeRewardSettingsPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
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
        this.rewardItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(rewardAmount);
        buf.writeInt(cooldownHours);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, rewardItem);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
        
    }
}
