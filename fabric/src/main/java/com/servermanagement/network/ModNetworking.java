package com.servermanagement.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.servermanagement.ServerManagementModFabric;

import com.servermanagement.network.packet.BankTransferPacket;
import com.servermanagement.network.packet.ClaimBankItemPacket;
import com.servermanagement.network.packet.ClaimDailyTaskPacket;
import com.servermanagement.network.packet.ClaimFreeRewardPacket;
import com.servermanagement.network.packet.ConsoleCommandPacket;
import com.servermanagement.network.packet.ConsoleResponsePacket;
import com.servermanagement.network.packet.ConsoleSubscribePacket;
import com.servermanagement.network.packet.DeleteTemplatePacket;
import com.servermanagement.network.packet.GamblingResultPacket;
import com.servermanagement.network.packet.GamblingTensionPacket;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.PMBanPlayerPacket;
import com.servermanagement.network.packet.PMKickPlayerPacket;
import com.servermanagement.network.packet.PMRequestPlayerListsPacket;
import com.servermanagement.network.packet.PMSpectatePlayerPacket;
import com.servermanagement.network.packet.PMSyncPlayerListsPacket;
import com.servermanagement.network.packet.PMUnbanPlayerPacket;
import com.servermanagement.network.packet.PMViewInventoryPacket;
import com.servermanagement.network.packet.PMWhitelistPacket;
import com.servermanagement.network.packet.PMWhitelistTogglePacket;
import com.servermanagement.network.packet.PlaceGamblingBetPacket;
import com.servermanagement.network.packet.PlaceGamblingBetWithItemPacket;
import com.servermanagement.network.packet.RequestAutoShowPacket;
import com.servermanagement.network.packet.RequestEconomyStatsPacket;
import com.servermanagement.network.packet.RequestWorldListPacket;
import com.servermanagement.network.packet.RespondMoneyRequestPacket;
import com.servermanagement.network.packet.SaveFreeRewardSettingsPacket;
import com.servermanagement.network.packet.SaveMotdPacket;
import com.servermanagement.network.packet.SaveTemplatePacket;
import com.servermanagement.network.packet.SendMoneyRequestPacket;
import com.servermanagement.network.packet.SyncAchievementsPacket;
import com.servermanagement.network.packet.SyncAutoShowPacket;
import com.servermanagement.network.packet.SyncBankAccountPacket;
import com.servermanagement.network.packet.SyncBankInventoryPacket;
import com.servermanagement.network.packet.SyncBettingSlotStatePacket;
import com.servermanagement.network.packet.SyncDailyTasksPacket;
import com.servermanagement.network.packet.SyncEconomyStatsPacket;
import com.servermanagement.network.packet.SyncEconomyTemplatesPacket;
import com.servermanagement.network.packet.SyncFeatureStatesPacket;
import com.servermanagement.network.packet.SyncGamblingStatsPacket;
import com.servermanagement.network.packet.SyncGlobalSettingsPacket;
import com.servermanagement.network.packet.SyncMarketPricesPacket;
import com.servermanagement.network.packet.SyncMoneyRequestsPacket;
import com.servermanagement.network.packet.SyncMotdPacket;
import com.servermanagement.network.packet.SyncPerformanceSettingsPacket;
import com.servermanagement.network.packet.SyncWorldDetailPacket;
import com.servermanagement.network.packet.SyncWorldListPacket;
import com.servermanagement.network.packet.ToggleAutoShowPacket;
import com.servermanagement.network.packet.ToggleFeaturePacket;
import com.servermanagement.network.packet.ToggleTemplatePacket;
import com.servermanagement.network.packet.UpdatePerformanceSettingPacket;
import com.servermanagement.network.packet.CheckForUpdatesPacket;
import com.servermanagement.network.packet.StartServerUpdatePacket;
import com.servermanagement.network.packet.SyncUpdateInfoPacket;
import com.servermanagement.network.packet.WMSetLobbyPacket;
import com.servermanagement.network.packet.SyncSessionTokenPacket;
import com.servermanagement.network.packet.AuthenticateSessionPacket;
import com.servermanagement.network.packet.WMSetTimerPacket;
import com.servermanagement.network.packet.WMTeleportToDimensionPacket;
import com.servermanagement.network.packet.WMToggleChatIsolationPacket;
import com.servermanagement.network.packet.WMTogglePortalsPacket;
import com.servermanagement.network.packet.WMToggleTabIsolationPacket;
import com.servermanagement.network.packet.minebay.AcceptOfferPacket;
import com.servermanagement.network.packet.minebay.CancelListingPacket;
import com.servermanagement.network.packet.minebay.CreateListingPacket;
import com.servermanagement.network.packet.minebay.CreateOfferPacket;
import com.servermanagement.network.packet.minebay.DeleteListingPacket;
import com.servermanagement.network.packet.minebay.HoldItemPacket;
import com.servermanagement.network.packet.minebay.PurchaseListingPacket;
import com.servermanagement.network.packet.minebay.RejectOfferPacket;
import com.servermanagement.network.packet.minebay.RequestListingOffersPacket;
import com.servermanagement.network.packet.minebay.SyncListingOffersPacket;
import com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket;

/**
 * 1.20.1 fabric networking facade.
 *
 * <p>Each {@link IPacket} is registered under a {@link net.minecraft.resources.ResourceLocation}
 * channel using the legacy {@code ResourceLocation + FriendlyByteBuf} fabric API
 * (1.21+ {@code CustomPacketPayload} / {@code PayloadTypeRegistry} are not available
 * on Fabric API 0.92.x for Minecraft 1.20.1).</p>
 */
public final class ModNetworking {

    private ModNetworking() { }

    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ToggleFeaturePacket.ID, (server, player, handler, buf, sender) -> { ToggleFeaturePacket pkt = new ToggleFeaturePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ToggleAutoShowPacket.ID, (server, player, handler, buf, sender) -> { ToggleAutoShowPacket pkt = new ToggleAutoShowPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestAutoShowPacket.ID, (server, player, handler, buf, sender) -> { RequestAutoShowPacket pkt = new RequestAutoShowPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMTogglePortalsPacket.ID, (server, player, handler, buf, sender) -> { WMTogglePortalsPacket pkt = new WMTogglePortalsPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMSetTimerPacket.ID, (server, player, handler, buf, sender) -> { WMSetTimerPacket pkt = new WMSetTimerPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMSetLobbyPacket.ID, (server, player, handler, buf, sender) -> { WMSetLobbyPacket pkt = new WMSetLobbyPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMToggleChatIsolationPacket.ID, (server, player, handler, buf, sender) -> { WMToggleChatIsolationPacket pkt = new WMToggleChatIsolationPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMToggleTabIsolationPacket.ID, (server, player, handler, buf, sender) -> { WMToggleTabIsolationPacket pkt = new WMToggleTabIsolationPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(WMTeleportToDimensionPacket.ID, (server, player, handler, buf, sender) -> { WMTeleportToDimensionPacket pkt = new WMTeleportToDimensionPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMSpectatePlayerPacket.ID, (server, player, handler, buf, sender) -> { PMSpectatePlayerPacket pkt = new PMSpectatePlayerPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMViewInventoryPacket.ID, (server, player, handler, buf, sender) -> { PMViewInventoryPacket pkt = new PMViewInventoryPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMKickPlayerPacket.ID, (server, player, handler, buf, sender) -> { PMKickPlayerPacket pkt = new PMKickPlayerPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMBanPlayerPacket.ID, (server, player, handler, buf, sender) -> { PMBanPlayerPacket pkt = new PMBanPlayerPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMUnbanPlayerPacket.ID, (server, player, handler, buf, sender) -> { PMUnbanPlayerPacket pkt = new PMUnbanPlayerPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMWhitelistPacket.ID, (server, player, handler, buf, sender) -> { PMWhitelistPacket pkt = new PMWhitelistPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMWhitelistTogglePacket.ID, (server, player, handler, buf, sender) -> { PMWhitelistTogglePacket pkt = new PMWhitelistTogglePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PMRequestPlayerListsPacket.ID, (server, player, handler, buf, sender) -> { PMRequestPlayerListsPacket pkt = new PMRequestPlayerListsPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestWorldListPacket.ID, (server, player, handler, buf, sender) -> { RequestWorldListPacket pkt = new RequestWorldListPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(OpenGuiPacket.ID, (server, player, handler, buf, sender) -> { OpenGuiPacket pkt = new OpenGuiPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ConsoleCommandPacket.ID, (server, player, handler, buf, sender) -> { ConsoleCommandPacket pkt = new ConsoleCommandPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ConsoleSubscribePacket.ID, (server, player, handler, buf, sender) -> { ConsoleSubscribePacket pkt = new ConsoleSubscribePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveTemplatePacket.ID, (server, player, handler, buf, sender) -> { SaveTemplatePacket pkt = new SaveTemplatePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(DeleteTemplatePacket.ID, (server, player, handler, buf, sender) -> { DeleteTemplatePacket pkt = new DeleteTemplatePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ToggleTemplatePacket.ID, (server, player, handler, buf, sender) -> { ToggleTemplatePacket pkt = new ToggleTemplatePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveFreeRewardSettingsPacket.ID, (server, player, handler, buf, sender) -> { SaveFreeRewardSettingsPacket pkt = new SaveFreeRewardSettingsPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(BankTransferPacket.ID, (server, player, handler, buf, sender) -> { BankTransferPacket pkt = new BankTransferPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimBankItemPacket.ID, (server, player, handler, buf, sender) -> { ClaimBankItemPacket pkt = new ClaimBankItemPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimDailyTaskPacket.ID, (server, player, handler, buf, sender) -> { ClaimDailyTaskPacket pkt = new ClaimDailyTaskPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(ClaimFreeRewardPacket.ID, (server, player, handler, buf, sender) -> { ClaimFreeRewardPacket pkt = new ClaimFreeRewardPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestEconomyStatsPacket.ID, (server, player, handler, buf, sender) -> { RequestEconomyStatsPacket pkt = new RequestEconomyStatsPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(SendMoneyRequestPacket.ID, (server, player, handler, buf, sender) -> { SendMoneyRequestPacket pkt = new SendMoneyRequestPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RespondMoneyRequestPacket.ID, (server, player, handler, buf, sender) -> { RespondMoneyRequestPacket pkt = new RespondMoneyRequestPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PlaceGamblingBetPacket.ID, (server, player, handler, buf, sender) -> { PlaceGamblingBetPacket pkt = new PlaceGamblingBetPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PlaceGamblingBetWithItemPacket.ID, (server, player, handler, buf, sender) -> { PlaceGamblingBetWithItemPacket pkt = new PlaceGamblingBetWithItemPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(UpdatePerformanceSettingPacket.ID, (server, player, handler, buf, sender) -> { UpdatePerformanceSettingPacket pkt = new UpdatePerformanceSettingPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(SaveMotdPacket.ID, (server, player, handler, buf, sender) -> { SaveMotdPacket pkt = new SaveMotdPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(SyncBettingSlotStatePacket.ID, (server, player, handler, buf, sender) -> { SyncBettingSlotStatePacket pkt = new SyncBettingSlotStatePacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(CreateListingPacket.ID, (server, player, handler, buf, sender) -> { CreateListingPacket pkt = new CreateListingPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(PurchaseListingPacket.ID, (server, player, handler, buf, sender) -> { PurchaseListingPacket pkt = new PurchaseListingPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(CancelListingPacket.ID, (server, player, handler, buf, sender) -> { CancelListingPacket pkt = new CancelListingPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(DeleteListingPacket.ID, (server, player, handler, buf, sender) -> { DeleteListingPacket pkt = new DeleteListingPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(CreateOfferPacket.ID, (server, player, handler, buf, sender) -> { CreateOfferPacket pkt = new CreateOfferPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(AcceptOfferPacket.ID, (server, player, handler, buf, sender) -> { AcceptOfferPacket pkt = new AcceptOfferPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RejectOfferPacket.ID, (server, player, handler, buf, sender) -> { RejectOfferPacket pkt = new RejectOfferPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(RequestListingOffersPacket.ID, (server, player, handler, buf, sender) -> { RequestListingOffersPacket pkt = new RequestListingOffersPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(HoldItemPacket.ID, (server, player, handler, buf, sender) -> { HoldItemPacket pkt = new HoldItemPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(AuthenticateSessionPacket.ID, (server, player, handler, buf, sender) -> { AuthenticateSessionPacket pkt = new AuthenticateSessionPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(CheckForUpdatesPacket.ID, (server, player, handler, buf, sender) -> { CheckForUpdatesPacket pkt = new CheckForUpdatesPacket(buf); server.execute(() -> pkt.handle(player)); });
        ServerPlayNetworking.registerGlobalReceiver(StartServerUpdatePacket.ID, (server, player, handler, buf, sender) -> { StartServerUpdatePacket pkt = new StartServerUpdatePacket(buf); server.execute(() -> pkt.handle(player)); });
    }

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SyncAutoShowPacket.ID, (client, handler, buf, sender) -> { SyncAutoShowPacket pkt = new SyncAutoShowPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncFeatureStatesPacket.ID, (client, handler, buf, sender) -> { SyncFeatureStatesPacket pkt = new SyncFeatureStatesPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(PMSyncPlayerListsPacket.ID, (client, handler, buf, sender) -> { PMSyncPlayerListsPacket pkt = new PMSyncPlayerListsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldListPacket.ID, (client, handler, buf, sender) -> { SyncWorldListPacket pkt = new SyncWorldListPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncWorldDetailPacket.ID, (client, handler, buf, sender) -> { SyncWorldDetailPacket pkt = new SyncWorldDetailPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(ConsoleResponsePacket.ID, (client, handler, buf, sender) -> { ConsoleResponsePacket pkt = new ConsoleResponsePacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncGlobalSettingsPacket.ID, (client, handler, buf, sender) -> { SyncGlobalSettingsPacket pkt = new SyncGlobalSettingsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomyTemplatesPacket.ID, (client, handler, buf, sender) -> { SyncEconomyTemplatesPacket pkt = new SyncEconomyTemplatesPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncBankAccountPacket.ID, (client, handler, buf, sender) -> { SyncBankAccountPacket pkt = new SyncBankAccountPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncBankInventoryPacket.ID, (client, handler, buf, sender) -> { SyncBankInventoryPacket pkt = new SyncBankInventoryPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncDailyTasksPacket.ID, (client, handler, buf, sender) -> { SyncDailyTasksPacket pkt = new SyncDailyTasksPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncAchievementsPacket.ID, (client, handler, buf, sender) -> { SyncAchievementsPacket pkt = new SyncAchievementsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncEconomyStatsPacket.ID, (client, handler, buf, sender) -> { SyncEconomyStatsPacket pkt = new SyncEconomyStatsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncMarketPricesPacket.ID, (client, handler, buf, sender) -> { SyncMarketPricesPacket pkt = new SyncMarketPricesPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncMoneyRequestsPacket.ID, (client, handler, buf, sender) -> { SyncMoneyRequestsPacket pkt = new SyncMoneyRequestsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(GamblingResultPacket.ID, (client, handler, buf, sender) -> { GamblingResultPacket pkt = new GamblingResultPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(GamblingTensionPacket.ID, (client, handler, buf, sender) -> { GamblingTensionPacket pkt = new GamblingTensionPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncGamblingStatsPacket.ID, (client, handler, buf, sender) -> { SyncGamblingStatsPacket pkt = new SyncGamblingStatsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncPerformanceSettingsPacket.ID, (client, handler, buf, sender) -> { SyncPerformanceSettingsPacket pkt = new SyncPerformanceSettingsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncMotdPacket.ID, (client, handler, buf, sender) -> { SyncMotdPacket pkt = new SyncMotdPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncMineBayListingsPacket.ID, (client, handler, buf, sender) -> { SyncMineBayListingsPacket pkt = new SyncMineBayListingsPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncListingOffersPacket.ID, (client, handler, buf, sender) -> { SyncListingOffersPacket pkt = new SyncListingOffersPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncSessionTokenPacket.ID, (client, handler, buf, sender) -> { SyncSessionTokenPacket pkt = new SyncSessionTokenPacket(buf); client.execute(() -> pkt.handle(null)); });
        ClientPlayNetworking.registerGlobalReceiver(SyncUpdateInfoPacket.ID, (client, handler, buf, sender) -> { SyncUpdateInfoPacket pkt = new SyncUpdateInfoPacket(buf); client.execute(() -> com.servermanagement.client.ClientUpdateManager.receiveUpdateInfo(pkt)); });
    }

    // ---------------- Send helpers -----------------

    public static void sendToServer(IPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ClientPlayNetworking.send(packet.id(), buf);
    }

    public static void sendToPlayer(ServerPlayer player, IPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPlayNetworking.send(player, packet.id(), buf);
    }

    /** Forge-style argument order convenience overload. */
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {
        sendToPlayer(player, packet);
    }

    public static void sendToAllPlayers(IPacket packet) {
        MinecraftServer server = ServerManagementModFabric.getServer();
        if (server == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, packet.id(), new FriendlyByteBuf(buf.copy()));
        }
    }

    private static boolean isAdminChannelId(net.minecraft.resources.ResourceLocation id) {
        return id.equals(ToggleFeaturePacket.ID) ||
               id.equals(WMTogglePortalsPacket.ID) ||
               id.equals(WMSetTimerPacket.ID) ||
               id.equals(WMSetLobbyPacket.ID) ||
               id.equals(WMToggleChatIsolationPacket.ID) ||
               id.equals(WMToggleTabIsolationPacket.ID) ||
               id.equals(WMTeleportToDimensionPacket.ID) ||
               id.equals(PMSpectatePlayerPacket.ID) ||
               id.equals(PMViewInventoryPacket.ID) ||
               id.equals(PMKickPlayerPacket.ID) ||
               id.equals(PMBanPlayerPacket.ID) ||
               id.equals(PMUnbanPlayerPacket.ID) ||
               id.equals(PMWhitelistPacket.ID) ||
               id.equals(PMWhitelistTogglePacket.ID) ||
               id.equals(PMRequestPlayerListsPacket.ID) ||
               id.equals(ConsoleCommandPacket.ID) ||
               id.equals(ConsoleSubscribePacket.ID) ||
               id.equals(SaveTemplatePacket.ID) ||
               id.equals(DeleteTemplatePacket.ID) ||
               id.equals(ToggleTemplatePacket.ID) ||
               id.equals(SaveFreeRewardSettingsPacket.ID) ||
               id.equals(UpdatePerformanceSettingPacket.ID) ||
               id.equals(SaveMotdPacket.ID) ||
               id.equals(CheckForUpdatesPacket.ID) ||
               id.equals(StartServerUpdatePacket.ID) ||
               id.equals(RequestEconomyStatsPacket.ID);
    }

    private static class ServerPlayNetworking {
        public static void registerGlobalReceiver(net.minecraft.resources.ResourceLocation id,
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.PlayChannelHandler handler) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler1, buf, sender) -> {
                if (isAdminChannelId(id)) {
                    if (!player.hasPermissions(2) || !com.servermanagement.security.SessionManager.getInstance().isSessionAuthenticated(player.getUUID())) {
                        com.mojang.logging.LogUtils.getLogger().warn("Player {} failed session token validation for channel {}", 
                            player.getName().getString(), id);
                        return;
                    }
                }
                if (id.equals(OpenGuiPacket.ID)) {
                    FriendlyByteBuf dup = new FriendlyByteBuf(buf.duplicate());
                    OpenGuiPacket pkt = new OpenGuiPacket(dup);
                    if (pkt.guiType().isAdminOnly()) {
                        if (!player.hasPermissions(2) || !com.servermanagement.security.SessionManager.getInstance().isSessionAuthenticated(player.getUUID())) {
                            com.mojang.logging.LogUtils.getLogger().warn("Player {} failed session token validation for admin GUI {}", 
                                player.getName().getString(), pkt.guiType());
                            return;
                        }
                    }
                }
                handler.receive(server, player, handler1, buf, sender);
            });
        }

        public static void send(ServerPlayer player, net.minecraft.resources.ResourceLocation id, FriendlyByteBuf buf) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, id, buf);
        }
    }
}
