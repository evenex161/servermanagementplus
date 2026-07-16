package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.packet.*;
import com.servermanagement.network.packet.minebay.*;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
        PayloadRegistrar registrar = event.registrar(ServerManagementMod.MOD_ID).versioned("1");

        // Server-bound packets (client → server)
        registrar.playToServer(ToggleFeaturePacket.TYPE, ToggleFeaturePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ToggleAutoShowPacket.TYPE, ToggleAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RequestAutoShowPacket.TYPE, RequestAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMTogglePortalsPacket.TYPE, WMTogglePortalsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMSetTimerPacket.TYPE, WMSetTimerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMSetLobbyPacket.TYPE, WMSetLobbyPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMToggleChatIsolationPacket.TYPE, WMToggleChatIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMToggleTabIsolationPacket.TYPE, WMToggleTabIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(WMTeleportToDimensionPacket.TYPE, WMTeleportToDimensionPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMSpectatePlayerPacket.TYPE, PMSpectatePlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMViewInventoryPacket.TYPE, PMViewInventoryPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMKickPlayerPacket.TYPE, PMKickPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMBanPlayerPacket.TYPE, PMBanPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMUnbanPlayerPacket.TYPE, PMUnbanPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMWhitelistPacket.TYPE, PMWhitelistPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMRequestPlayerListsPacket.TYPE, PMRequestPlayerListsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PMWhitelistTogglePacket.TYPE, PMWhitelistTogglePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RequestWorldListPacket.TYPE, RequestWorldListPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ConsoleCommandPacket.TYPE, ConsoleCommandPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ConsoleSubscribePacket.TYPE, ConsoleSubscribePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(SaveTemplatePacket.TYPE, SaveTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(DeleteTemplatePacket.TYPE, DeleteTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ToggleTemplatePacket.TYPE, ToggleTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(SaveFreeRewardSettingsPacket.TYPE, SaveFreeRewardSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ClaimDailyTaskPacket.TYPE, ClaimDailyTaskPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ClaimFreeRewardPacket.TYPE, ClaimFreeRewardPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ClaimBankItemPacket.TYPE, ClaimBankItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(ModFileRequestPacket.TYPE, ModFileRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PlaceGamblingBetPacket.TYPE, PlaceGamblingBetPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PlaceGamblingBetWithItemPacket.TYPE, PlaceGamblingBetWithItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(SyncBettingSlotStatePacket.TYPE, SyncBettingSlotStatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(SendMoneyRequestPacket.TYPE, SendMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RespondMoneyRequestPacket.TYPE, RespondMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(UpdatePerformanceSettingPacket.TYPE, UpdatePerformanceSettingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(SaveMotdPacket.TYPE, SaveMotdPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RequestEconomyStatsPacket.TYPE, RequestEconomyStatsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // MineBay server-bound packets
        registrar.playToServer(HoldItemPacket.TYPE, HoldItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(CreateListingPacket.TYPE, CreateListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(CancelListingPacket.TYPE, CancelListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(PurchaseListingPacket.TYPE, PurchaseListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(CreateOfferPacket.TYPE, CreateOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(AcceptOfferPacket.TYPE, AcceptOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RejectOfferPacket.TYPE, RejectOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(RequestListingOffersPacket.TYPE, RequestListingOffersPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(DeleteListingPacket.TYPE, DeleteListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // Client-bound packets (server → client)
        registrar.playToClient(SyncAutoShowPacket.TYPE, SyncAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncFeatureStatesPacket.TYPE, SyncFeatureStatesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(PMSyncPlayerListsPacket.TYPE, PMSyncPlayerListsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncWorldListPacket.TYPE, SyncWorldListPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncWorldDetailPacket.TYPE, SyncWorldDetailPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(ConsoleResponsePacket.TYPE, ConsoleResponsePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncGlobalSettingsPacket.TYPE, SyncGlobalSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncEconomyTemplatesPacket.TYPE, SyncEconomyTemplatesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncBankAccountPacket.TYPE, SyncBankAccountPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncDailyTasksPacket.TYPE, SyncDailyTasksPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncAchievementsPacket.TYPE, SyncAchievementsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncBankInventoryPacket.TYPE, SyncBankInventoryPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(VersionCheckPacket.TYPE, VersionCheckPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(ModFileChunkPacket.TYPE, ModFileChunkPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(ModFileCompletePacket.TYPE, ModFileCompletePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(GamblingResultPacket.TYPE, GamblingResultPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(GamblingTensionPacket.TYPE, GamblingTensionPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncGamblingStatsPacket.TYPE, SyncGamblingStatsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncMoneyRequestsPacket.TYPE, SyncMoneyRequestsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncPerformanceSettingsPacket.TYPE, SyncPerformanceSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncMarketPricesPacket.TYPE, SyncMarketPricesPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncMotdPacket.TYPE, SyncMotdPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncEconomyStatsPacket.TYPE, SyncEconomyStatsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // MineBay client-bound packets
        registrar.playToClient(SyncMineBayListingsPacket.TYPE, SyncMineBayListingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncListingOffersPacket.TYPE, SyncListingOffersPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        ServerManagementMod.LOGGER.info("Registered 72 network packets");
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.debug("Client-side packet handlers ready");
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAllPlayers(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}

