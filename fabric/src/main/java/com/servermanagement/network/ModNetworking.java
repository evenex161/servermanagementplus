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

    private static final java.util.Map<net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?>, net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload>> CODECS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?>, java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.minecraft.server.level.ServerPlayer>> HANDLERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> getC2SCodec(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        return CODECS.get(type);
    }

    @SuppressWarnings("unchecked")
    public static java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.minecraft.server.level.ServerPlayer> getC2SHandler(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        return (java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.minecraft.server.level.ServerPlayer>) (Object) HANDLERS.get(type);
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void registerC2S(
            net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type, 
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, T> codec, 
            java.util.function.BiConsumer<T, net.minecraft.server.level.ServerPlayer> handler) {
        
        CODECS.put(type, codec);
        HANDLERS.put(type, (pkt, player) -> handler.accept((T) pkt, player));
        
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            net.minecraft.server.level.ServerPlayer player = context.player();
            if (player != null) {
                com.servermanagement.ServerManagementMod.LOGGER.warn("SECURITY ALERT: Rejected unwrapped packet {} from {}", type.id(), player.getName().getString());
            }
        });
    }

    public static void registerServerPackets() {
        // Register payload types for server-bound packets
        registerC2S(ToggleFeaturePacket.TYPE, ToggleFeaturePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ToggleAutoShowPacket.TYPE, ToggleAutoShowPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RequestAutoShowPacket.TYPE, RequestAutoShowPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMTogglePortalsPacket.TYPE, WMTogglePortalsPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMSetTimerPacket.TYPE, WMSetTimerPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMSetLobbyPacket.TYPE, WMSetLobbyPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMToggleChatIsolationPacket.TYPE, WMToggleChatIsolationPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMToggleTabIsolationPacket.TYPE, WMToggleTabIsolationPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(WMTeleportToDimensionPacket.TYPE, WMTeleportToDimensionPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMSpectatePlayerPacket.TYPE, PMSpectatePlayerPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMViewInventoryPacket.TYPE, PMViewInventoryPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMKickPlayerPacket.TYPE, PMKickPlayerPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMBanPlayerPacket.TYPE, PMBanPlayerPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMUnbanPlayerPacket.TYPE, PMUnbanPlayerPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMWhitelistPacket.TYPE, PMWhitelistPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMWhitelistTogglePacket.TYPE, PMWhitelistTogglePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PMRequestPlayerListsPacket.TYPE, PMRequestPlayerListsPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RequestWorldListPacket.TYPE, RequestWorldListPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ConsoleCommandPacket.TYPE, ConsoleCommandPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ConsoleSubscribePacket.TYPE, ConsoleSubscribePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(SaveTemplatePacket.TYPE, SaveTemplatePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(DeleteTemplatePacket.TYPE, DeleteTemplatePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ToggleTemplatePacket.TYPE, ToggleTemplatePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(SaveFreeRewardSettingsPacket.TYPE, SaveFreeRewardSettingsPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ClaimBankItemPacket.TYPE, ClaimBankItemPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ClaimDailyTaskPacket.TYPE, ClaimDailyTaskPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ClaimFreeRewardPacket.TYPE, ClaimFreeRewardPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RequestEconomyStatsPacket.TYPE, RequestEconomyStatsPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(SendMoneyRequestPacket.TYPE, SendMoneyRequestPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RespondMoneyRequestPacket.TYPE, RespondMoneyRequestPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PlaceGamblingBetPacket.TYPE, PlaceGamblingBetPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PlaceGamblingBetWithItemPacket.TYPE, PlaceGamblingBetWithItemPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(ModFileRequestPacket.TYPE, ModFileRequestPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(UpdatePerformanceSettingPacket.TYPE, UpdatePerformanceSettingPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(SaveMotdPacket.TYPE, SaveMotdPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(SyncBettingSlotStatePacket.TYPE, SyncBettingSlotStatePacket.STREAM_CODEC, (payload, player) -> payload.handle(player));

        // MineBay server-bound
        registerC2S(CreateListingPacket.TYPE, CreateListingPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(PurchaseListingPacket.TYPE, PurchaseListingPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(CancelListingPacket.TYPE, CancelListingPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(DeleteListingPacket.TYPE, DeleteListingPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(CreateOfferPacket.TYPE, CreateOfferPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(AcceptOfferPacket.TYPE, AcceptOfferPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RejectOfferPacket.TYPE, RejectOfferPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(RequestListingOffersPacket.TYPE, RequestListingOffersPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));
        registerC2S(HoldItemPacket.TYPE, HoldItemPacket.STREAM_CODEC, (payload, player) -> payload.handle(player));

        // Register Secure Packet Wrapper
        PayloadTypeRegistry.playC2S().register(com.servermanagement.network.packet.SecurePacketWrapper.TYPE, com.servermanagement.network.packet.SecurePacketWrapper.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.servermanagement.network.packet.SecurePacketWrapper.TYPE, (payload, context) -> {
            net.minecraft.server.level.ServerPlayer player = context.player();
            context.server().execute(() -> payload.handle(player));
        });

        // Register payload types for client-bound packets
        PayloadTypeRegistry.playS2C().register(SyncAutoShowPacket.TYPE, SyncAutoShowPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFeatureStatesPacket.TYPE, SyncFeatureStatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PMSyncPlayerListsPacket.TYPE, PMSyncPlayerListsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncWorldListPacket.TYPE, SyncWorldListPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncWorldDetailPacket.TYPE, SyncWorldDetailPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConsoleResponsePacket.TYPE, ConsoleResponsePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGlobalSettingsPacket.TYPE, SyncGlobalSettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEconomyTemplatesPacket.TYPE, SyncEconomyTemplatesPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBankAccountPacket.TYPE, SyncBankAccountPacket.STREAM_CODEC);
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
        PayloadTypeRegistry.playS2C().register(SyncSessionTokenPacket.TYPE, SyncSessionTokenPacket.STREAM_CODEC);
    }

    public static void registerClientPackets() {
        // Client-side handlers are registered in the client entrypoint
        ClientPlayNetworking.registerGlobalReceiver(SyncAutoShowPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncFeatureStatesPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(PMSyncPlayerListsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldListPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldDetailPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(ConsoleResponsePacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncGlobalSettingsPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomyTemplatesPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
        ClientPlayNetworking.registerGlobalReceiver(SyncBankAccountPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
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
        ClientPlayNetworking.registerGlobalReceiver(SyncSessionTokenPacket.TYPE, (payload, context) -> context.client().execute(() -> payload.handle(null)));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        if (payload instanceof com.servermanagement.network.packet.SecurePacketWrapper || payload.type().id().getPath().contains("session_token")) {
            ClientPlayNetworking.send(payload);
        } else {
            String token = com.servermanagement.client.ClientSessionRegistry.getSessionToken();
            ClientPlayNetworking.send(new com.servermanagement.network.packet.SecurePacketWrapper(payload, token));
        }
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
