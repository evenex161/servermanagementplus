package com.servermanagement.network;

import com.servermanagement.ServerManagementModFabric;
import com.servermanagement.network.packet.*;
import com.servermanagement.network.packet.minebay.*;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {

    public static void registerServerPackets() {
        // Register payload types for server-bound packets
        PayloadTypeRegistry.playC2S().register(CheckForUpdatesPacket.TYPE, CheckForUpdatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StartServerUpdatePacket.TYPE, StartServerUpdatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleFeaturePacket.TYPE, ToggleFeaturePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveEconomySettingsPacket.TYPE, SaveEconomySettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleAutoShowPacket.TYPE, ToggleAutoShowPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestAutoShowPacket.TYPE, RequestAutoShowPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMTogglePortalsPacket.TYPE, WMTogglePortalsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMSetTimerPacket.TYPE, WMSetTimerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMSetLobbyPacket.TYPE, WMSetLobbyPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMToggleChatIsolationPacket.TYPE, WMToggleChatIsolationPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMToggleTabIsolationPacket.TYPE, WMToggleTabIsolationPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WMTeleportToDimensionPacket.TYPE, WMTeleportToDimensionPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMSpectatePlayerPacket.TYPE, PMSpectatePlayerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMViewInventoryPacket.TYPE, PMViewInventoryPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMKickPlayerPacket.TYPE, PMKickPlayerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMBanPlayerPacket.TYPE, PMBanPlayerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMUnbanPlayerPacket.TYPE, PMUnbanPlayerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMWhitelistPacket.TYPE, PMWhitelistPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMWhitelistTogglePacket.TYPE, PMWhitelistTogglePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PMRequestPlayerListsPacket.TYPE, PMRequestPlayerListsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestWorldListPacket.TYPE, RequestWorldListPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ConsoleCommandPacket.TYPE, ConsoleCommandPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ConsoleSubscribePacket.TYPE, ConsoleSubscribePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveTemplatePacket.TYPE, SaveTemplatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteTemplatePacket.TYPE, DeleteTemplatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleTemplatePacket.TYPE, ToggleTemplatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveFreeRewardSettingsPacket.TYPE, SaveFreeRewardSettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimBankItemPacket.TYPE, ClaimBankItemPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimDailyTaskPacket.TYPE, ClaimDailyTaskPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimFreeRewardPacket.TYPE, ClaimFreeRewardPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestEconomyStatsPacket.TYPE, RequestEconomyStatsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SendMoneyRequestPacket.TYPE, SendMoneyRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RespondMoneyRequestPacket.TYPE, RespondMoneyRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PlaceGamblingBetPacket.TYPE, PlaceGamblingBetPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PlaceGamblingBetWithItemPacket.TYPE, PlaceGamblingBetWithItemPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ModFileRequestPacket.TYPE, ModFileRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePerformanceSettingPacket.TYPE, UpdatePerformanceSettingPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveMotdPacket.TYPE, SaveMotdPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SyncBettingSlotStatePacket.TYPE, SyncBettingSlotStatePacket.STREAM_CODEC);

        // MineBay server-bound
        PayloadTypeRegistry.playC2S().register(CreateListingPacket.TYPE, CreateListingPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PurchaseListingPacket.TYPE, PurchaseListingPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CancelListingPacket.TYPE, CancelListingPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteListingPacket.TYPE, DeleteListingPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CreateOfferPacket.TYPE, CreateOfferPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AcceptOfferPacket.TYPE, AcceptOfferPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RejectOfferPacket.TYPE, RejectOfferPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestListingOffersPacket.TYPE, RequestListingOffersPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(HoldItemPacket.TYPE, HoldItemPacket.STREAM_CODEC);

        // Register payload types for client-bound packets
        PayloadTypeRegistry.playS2C().register(SyncUpdateInfoPacket.TYPE, SyncUpdateInfoPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncAutoShowPacket.TYPE, SyncAutoShowPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFeatureStatesPacket.TYPE, SyncFeatureStatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PMSyncPlayerListsPacket.TYPE, PMSyncPlayerListsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncWorldListPacket.TYPE, SyncWorldListPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncWorldDetailPacket.TYPE, SyncWorldDetailPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConsoleResponsePacket.TYPE, ConsoleResponsePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGlobalSettingsPacket.TYPE, SyncGlobalSettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEconomyTemplatesPacket.TYPE, SyncEconomyTemplatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBankAccountPacket.TYPE, SyncBankAccountPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEconomySettingsPacket.TYPE, SyncEconomySettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBankInventoryPacket.TYPE, SyncBankInventoryPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDailyTasksPacket.TYPE, SyncDailyTasksPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncAchievementsPacket.TYPE, SyncAchievementsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEconomyStatsPacket.TYPE, SyncEconomyStatsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMarketPricesPacket.TYPE, SyncMarketPricesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMoneyRequestsPacket.TYPE, SyncMoneyRequestsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(GamblingResultPacket.TYPE, GamblingResultPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(GamblingTensionPacket.TYPE, GamblingTensionPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGamblingStatsPacket.TYPE, SyncGamblingStatsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VersionCheckPacket.TYPE, VersionCheckPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ModFileChunkPacket.TYPE, ModFileChunkPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ModFileCompletePacket.TYPE, ModFileCompletePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPerformanceSettingsPacket.TYPE, SyncPerformanceSettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMotdPacket.TYPE, SyncMotdPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMineBayListingsPacket.TYPE, SyncMineBayListingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncListingOffersPacket.TYPE, SyncListingOffersPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC);

        // Register server-side handlers
        ServerPlayNetworking.registerGlobalReceiver(CheckForUpdatesPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(StartServerUpdatePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ToggleFeaturePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ToggleAutoShowPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestAutoShowPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMTogglePortalsPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMSetTimerPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMSetLobbyPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMToggleChatIsolationPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMToggleTabIsolationPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(WMTeleportToDimensionPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMSpectatePlayerPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMViewInventoryPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMKickPlayerPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMBanPlayerPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMUnbanPlayerPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMWhitelistPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMWhitelistTogglePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PMRequestPlayerListsPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestWorldListPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(OpenGuiPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ConsoleCommandPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ConsoleSubscribePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveTemplatePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(DeleteTemplatePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ToggleTemplatePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveFreeRewardSettingsPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(BankTransferPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimBankItemPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimDailyTaskPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimFreeRewardPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestEconomyStatsPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(SendMoneyRequestPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RespondMoneyRequestPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PlaceGamblingBetPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PlaceGamblingBetWithItemPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(ModFileRequestPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(UpdatePerformanceSettingPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveMotdPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(SyncBettingSlotStatePacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });

        // MineBay server-bound handlers
        ServerPlayNetworking.registerGlobalReceiver(CreateListingPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(PurchaseListingPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(CancelListingPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(DeleteListingPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(CreateOfferPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(AcceptOfferPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RejectOfferPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestListingOffersPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
        ServerPlayNetworking.registerGlobalReceiver(HoldItemPacket.TYPE, (payload, context) -> { var p = context.player(); p.server.execute(() -> payload.handle(p)); });
    }

    public static void registerClientPackets() {
        // Client-side handlers are registered in the client entrypoint
        ClientPlayNetworking.registerGlobalReceiver(SyncUpdateInfoPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncAutoShowPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncFeatureStatesPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(PMSyncPlayerListsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldListPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldDetailPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(ConsoleResponsePacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncGlobalSettingsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomyTemplatesPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncBankAccountPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomySettingsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncBankInventoryPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncDailyTasksPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncAchievementsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomyStatsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncMarketPricesPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncMoneyRequestsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(GamblingResultPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(GamblingTensionPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncGamblingStatsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(VersionCheckPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(ModFileChunkPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(ModFileCompletePacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncPerformanceSettingsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncMotdPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncMineBayListingsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncListingOffersPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handleClient()));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    // Overload for Forge-style argument order (packet, player)
    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        var server = ServerManagementModFabric.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}


