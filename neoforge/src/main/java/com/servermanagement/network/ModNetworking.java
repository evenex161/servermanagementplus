package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.packet.*;
import com.servermanagement.network.packet.minebay.*;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    private static final java.util.Map<net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?>, net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload>> CODECS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?>, java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.neoforged.neoforge.network.handling.IPayloadContext>> HANDLERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, ? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> getC2SCodec(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        return CODECS.get(type);
    }

    @SuppressWarnings("unchecked")
    public static java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.neoforged.neoforge.network.handling.IPayloadContext> getC2SHandler(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        return (java.util.function.BiConsumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload, net.neoforged.neoforge.network.handling.IPayloadContext>) (Object) HANDLERS.get(type);
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void registerC2S(PayloadRegistrar registrar, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type, net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, T> codec, java.util.function.BiConsumer<T, net.neoforged.neoforge.network.handling.IPayloadContext> handler) {
        CODECS.put(type, codec);
        HANDLERS.put(type, (pkt, ctx) -> handler.accept((T) pkt, ctx));
        
        registrar.playToServer(type, codec, (pkt, ctx) -> {
            net.minecraft.server.level.ServerPlayer player = (ctx.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) ctx.player() : null;
            if (player != null) {
                ServerManagementMod.LOGGER.warn("SECURITY ALERT: Rejected unwrapped packet {} from {}", type.id(), player.getName().getString());
            }
        });
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ServerManagementMod.MOD_ID).versioned("1");

        // Server-bound packets (client → server)
        registerC2S(registrar, ToggleFeaturePacket.TYPE, ToggleFeaturePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ToggleAutoShowPacket.TYPE, ToggleAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RequestAutoShowPacket.TYPE, RequestAutoShowPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMTogglePortalsPacket.TYPE, WMTogglePortalsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMSetTimerPacket.TYPE, WMSetTimerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMSetLobbyPacket.TYPE, WMSetLobbyPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMToggleChatIsolationPacket.TYPE, WMToggleChatIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMToggleTabIsolationPacket.TYPE, WMToggleTabIsolationPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, WMTeleportToDimensionPacket.TYPE, WMTeleportToDimensionPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMSpectatePlayerPacket.TYPE, PMSpectatePlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMViewInventoryPacket.TYPE, PMViewInventoryPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMKickPlayerPacket.TYPE, PMKickPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMBanPlayerPacket.TYPE, PMBanPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMUnbanPlayerPacket.TYPE, PMUnbanPlayerPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMWhitelistPacket.TYPE, PMWhitelistPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMRequestPlayerListsPacket.TYPE, PMRequestPlayerListsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PMWhitelistTogglePacket.TYPE, PMWhitelistTogglePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RequestWorldListPacket.TYPE, RequestWorldListPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ConsoleCommandPacket.TYPE, ConsoleCommandPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ConsoleSubscribePacket.TYPE, ConsoleSubscribePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, SaveTemplatePacket.TYPE, SaveTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, DeleteTemplatePacket.TYPE, DeleteTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ToggleTemplatePacket.TYPE, ToggleTemplatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, SaveFreeRewardSettingsPacket.TYPE, SaveFreeRewardSettingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ClaimDailyTaskPacket.TYPE, ClaimDailyTaskPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ClaimFreeRewardPacket.TYPE, ClaimFreeRewardPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ClaimBankItemPacket.TYPE, ClaimBankItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, ModFileRequestPacket.TYPE, ModFileRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PlaceGamblingBetPacket.TYPE, PlaceGamblingBetPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PlaceGamblingBetWithItemPacket.TYPE, PlaceGamblingBetWithItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, SyncBettingSlotStatePacket.TYPE, SyncBettingSlotStatePacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, SendMoneyRequestPacket.TYPE, SendMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RespondMoneyRequestPacket.TYPE, RespondMoneyRequestPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, UpdatePerformanceSettingPacket.TYPE, UpdatePerformanceSettingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, SaveMotdPacket.TYPE, SaveMotdPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RequestEconomyStatsPacket.TYPE, RequestEconomyStatsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // MineBay server-bound packets
        registerC2S(registrar, HoldItemPacket.TYPE, HoldItemPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, CreateListingPacket.TYPE, CreateListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, CancelListingPacket.TYPE, CancelListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, PurchaseListingPacket.TYPE, PurchaseListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, CreateOfferPacket.TYPE, CreateOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, AcceptOfferPacket.TYPE, AcceptOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RejectOfferPacket.TYPE, RejectOfferPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, RequestListingOffersPacket.TYPE, RequestListingOffersPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registerC2S(registrar, DeleteListingPacket.TYPE, DeleteListingPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // Register Secure Packet Wrapper
        registrar.playToServer(com.servermanagement.network.packet.SecurePacketWrapper.TYPE, com.servermanagement.network.packet.SecurePacketWrapper.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

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
        registrar.playToClient(SyncSessionTokenPacket.TYPE, SyncSessionTokenPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // MineBay client-bound packets
        registrar.playToClient(SyncMineBayListingsPacket.TYPE, SyncMineBayListingsPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(SyncListingOffersPacket.TYPE, SyncListingOffersPacket.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        ServerManagementMod.LOGGER.info("Registered 74 network packets");
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.debug("Client-side packet handlers ready");
    }

    public static void sendToServer(CustomPacketPayload packet) {
        if (packet instanceof com.servermanagement.network.packet.SecurePacketWrapper || packet.type().id().getPath().contains("session_token")) {
            ClientPacketDistributor.sendToServer(packet);
        } else {
            String token = com.servermanagement.client.ClientSessionRegistry.getSessionToken();
            ClientPacketDistributor.sendToServer(new com.servermanagement.network.packet.SecurePacketWrapper(packet, token));
        }
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAllPlayers(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}
