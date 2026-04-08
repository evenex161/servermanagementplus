package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.packet.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        ServerManagementMod.LOGGER.info("Registering network packets");
        
        // Config packets (bidirectional)
        registrar.playBidirectional(ToggleFeaturePacket.TYPE, ToggleFeaturePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ToggleAutoShowPacket.TYPE, ToggleAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(RequestAutoShowPacket.TYPE, RequestAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncAutoShowPacket.TYPE, SyncAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncFeatureStatesPacket.TYPE, SyncFeatureStatesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // WorldManager packets
        registrar.playBidirectional(WMTogglePortalsPacket.TYPE, WMTogglePortalsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(WMSetTimerPacket.TYPE, WMSetTimerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(WMSetLobbyPacket.TYPE, WMSetLobbyPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(WMToggleChatIsolationPacket.TYPE, WMToggleChatIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(WMToggleTabIsolationPacket.TYPE, WMToggleTabIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(WMTeleportToDimensionPacket.TYPE, WMTeleportToDimensionPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // PlayerManager packets
        registrar.playBidirectional(PMSpectatePlayerPacket.TYPE, PMSpectatePlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(PMViewInventoryPacket.TYPE, PMViewInventoryPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // GUI data sync
        registrar.playBidirectional(SyncWorldListPacket.TYPE, SyncWorldListPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(RequestWorldListPacket.TYPE, RequestWorldListPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncWorldDetailPacket.TYPE, SyncWorldDetailPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ConsoleCommandPacket.TYPE, ConsoleCommandPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ConsoleResponsePacket.TYPE, ConsoleResponsePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncGlobalSettingsPacket.TYPE, SyncGlobalSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Economy Management admin packets
        registrar.playBidirectional(SaveTemplatePacket.TYPE, SaveTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(DeleteTemplatePacket.TYPE, DeleteTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ToggleTemplatePacket.TYPE, ToggleTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SaveFreeRewardSettingsPacket.TYPE, SaveFreeRewardSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncEconomyTemplatesPacket.TYPE, SyncEconomyTemplatesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncBankAccountPacket.TYPE, SyncBankAccountPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncDailyTasksPacket.TYPE, SyncDailyTasksPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncAchievementsPacket.TYPE, SyncAchievementsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ClaimDailyTaskPacket.TYPE, ClaimDailyTaskPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ClaimFreeRewardPacket.TYPE, ClaimFreeRewardPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // MineBay packets
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.HoldItemPacket.TYPE, com.servermanagement.network.packet.minebay.HoldItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.CreateListingPacket.TYPE, com.servermanagement.network.packet.minebay.CreateListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.CancelListingPacket.TYPE, com.servermanagement.network.packet.minebay.CancelListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket.TYPE, com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.PurchaseListingPacket.TYPE, com.servermanagement.network.packet.minebay.PurchaseListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.CreateOfferPacket.TYPE, com.servermanagement.network.packet.minebay.CreateOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.AcceptOfferPacket.TYPE, com.servermanagement.network.packet.minebay.AcceptOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.RejectOfferPacket.TYPE, com.servermanagement.network.packet.minebay.RejectOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.minebay.DeleteListingPacket.TYPE, com.servermanagement.network.packet.minebay.DeleteListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Bank inventory packets
        registrar.playBidirectional(SyncBankInventoryPacket.TYPE, SyncBankInventoryPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ClaimBankItemPacket.TYPE, ClaimBankItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // OTA Update packets
        registrar.playBidirectional(VersionCheckPacket.TYPE, VersionCheckPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ModFileRequestPacket.TYPE, ModFileRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ModFileChunkPacket.TYPE, ModFileChunkPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(ModFileCompletePacket.TYPE, ModFileCompletePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Gambling packets
        registrar.playBidirectional(PlaceGamblingBetPacket.TYPE, PlaceGamblingBetPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(PlaceGamblingBetWithItemPacket.TYPE, PlaceGamblingBetWithItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(GamblingResultPacket.TYPE, GamblingResultPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncBettingSlotStatePacket.TYPE, SyncBettingSlotStatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncGamblingStatsPacket.TYPE, SyncGamblingStatsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(GamblingTensionPacket.TYPE, GamblingTensionPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Money Request packets
        registrar.playBidirectional(SendMoneyRequestPacket.TYPE, SendMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(RespondMoneyRequestPacket.TYPE, RespondMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(SyncMoneyRequestsPacket.TYPE, SyncMoneyRequestsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Market pricing packets
        registrar.playBidirectional(com.servermanagement.network.packet.SyncMarketPricesPacket.TYPE, com.servermanagement.network.packet.SyncMarketPricesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        // Performance settings packets
        registrar.playBidirectional(com.servermanagement.network.packet.SyncPerformanceSettingsPacket.TYPE, com.servermanagement.network.packet.SyncPerformanceSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playBidirectional(com.servermanagement.network.packet.UpdatePerformanceSettingPacket.TYPE, com.servermanagement.network.packet.UpdatePerformanceSettingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        
        ServerManagementMod.LOGGER.info("Registered network packets");
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.info("Client-side packet handlers ready");
    }
    
    public static void sendToServer(IPacket packet) {
        PacketDistributor.sendToServer(packet);
    }
    
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToAllPlayers(IPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}
