package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
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
import com.servermanagement.network.packet.IPacket;
import com.servermanagement.network.packet.ModFileChunkPacket;
import com.servermanagement.network.packet.ModFileCompletePacket;
import com.servermanagement.network.packet.ModFileRequestPacket;
import com.servermanagement.network.packet.OpenGuiPacket;
import com.servermanagement.network.packet.PMSpectatePlayerPacket;
import com.servermanagement.network.packet.PMViewInventoryPacket;
import com.servermanagement.network.packet.PMKickPlayerPacket;
import com.servermanagement.network.packet.PMBanPlayerPacket;
import com.servermanagement.network.packet.PMUnbanPlayerPacket;
import com.servermanagement.network.packet.PMWhitelistPacket;
import com.servermanagement.network.packet.PMWhitelistTogglePacket;
import com.servermanagement.network.packet.PMRequestPlayerListsPacket;
import com.servermanagement.network.packet.PMSyncPlayerListsPacket;
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
import com.servermanagement.network.packet.SyncMoneyRequestsPacket;
import com.servermanagement.network.packet.SyncMotdPacket;
import com.servermanagement.network.packet.SyncPerformanceSettingsPacket;
import com.servermanagement.network.packet.SyncWorldDetailPacket;
import com.servermanagement.network.packet.SyncWorldListPacket;
import com.servermanagement.network.packet.ToggleAutoShowPacket;
import com.servermanagement.network.packet.ToggleFeaturePacket;
import com.servermanagement.network.packet.ToggleTemplatePacket;
import com.servermanagement.network.packet.UpdatePerformanceSettingPacket;
import com.servermanagement.network.packet.VersionCheckPacket;
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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static WrappedSimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = new WrappedSimpleChannel(NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            s -> true,
            s -> true
        ));

        ServerManagementMod.LOGGER.info("Registering network packets");
        
        // Config packets (bidirectional)
        INSTANCE.registerMessage(id(), ToggleFeaturePacket.class, ToggleFeaturePacket::encode, ToggleFeaturePacket::new, ToggleFeaturePacket::handle);
            
        INSTANCE.registerMessage(id(), ToggleAutoShowPacket.class, ToggleAutoShowPacket::encode, ToggleAutoShowPacket::new, ToggleAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), RequestAutoShowPacket.class, RequestAutoShowPacket::encode, RequestAutoShowPacket::new, RequestAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncAutoShowPacket.class, SyncAutoShowPacket::encode, SyncAutoShowPacket::new, SyncAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncFeatureStatesPacket.class, SyncFeatureStatesPacket::encode, SyncFeatureStatesPacket::new, SyncFeatureStatesPacket::handle);
        
        // WorldManager packets (server-bound)
        INSTANCE.registerMessage(id(), WMTogglePortalsPacket.class, WMTogglePortalsPacket::encode, WMTogglePortalsPacket::new, WMTogglePortalsPacket::handle);
            
        INSTANCE.registerMessage(id(), WMSetTimerPacket.class, WMSetTimerPacket::encode, WMSetTimerPacket::new, WMSetTimerPacket::handle);
            
        INSTANCE.registerMessage(id(), WMSetLobbyPacket.class, WMSetLobbyPacket::encode, WMSetLobbyPacket::new, WMSetLobbyPacket::handle);
            
        INSTANCE.registerMessage(id(), WMToggleChatIsolationPacket.class, WMToggleChatIsolationPacket::encode, WMToggleChatIsolationPacket::new, WMToggleChatIsolationPacket::handle);
            
        INSTANCE.registerMessage(id(), WMToggleTabIsolationPacket.class, WMToggleTabIsolationPacket::encode, WMToggleTabIsolationPacket::new, WMToggleTabIsolationPacket::handle);
            
        INSTANCE.registerMessage(id(), WMTeleportToDimensionPacket.class, WMTeleportToDimensionPacket::encode, WMTeleportToDimensionPacket::new, WMTeleportToDimensionPacket::handle);
        
        // PlayerManager packets (server-bound)
        INSTANCE.registerMessage(id(), PMSpectatePlayerPacket.class, PMSpectatePlayerPacket::encode, PMSpectatePlayerPacket::new, PMSpectatePlayerPacket::handle);
            
        INSTANCE.registerMessage(id(), PMViewInventoryPacket.class, PMViewInventoryPacket::encode, PMViewInventoryPacket::new, PMViewInventoryPacket::handle);
            
        INSTANCE.registerMessage(id(), PMKickPlayerPacket.class, PMKickPlayerPacket::encode, PMKickPlayerPacket::new, PMKickPlayerPacket::handle);
            
        INSTANCE.registerMessage(id(), PMBanPlayerPacket.class, PMBanPlayerPacket::encode, PMBanPlayerPacket::new, PMBanPlayerPacket::handle);
            
        INSTANCE.registerMessage(id(), PMUnbanPlayerPacket.class, PMUnbanPlayerPacket::encode, PMUnbanPlayerPacket::new, PMUnbanPlayerPacket::handle);
            
        INSTANCE.registerMessage(id(), PMWhitelistPacket.class, PMWhitelistPacket::encode, PMWhitelistPacket::new, PMWhitelistPacket::handle);
            
        INSTANCE.registerMessage(id(), PMRequestPlayerListsPacket.class, PMRequestPlayerListsPacket::encode, PMRequestPlayerListsPacket::new, PMRequestPlayerListsPacket::handle);
            
        INSTANCE.registerMessage(id(), PMSyncPlayerListsPacket.class, PMSyncPlayerListsPacket::encode, PMSyncPlayerListsPacket::new, PMSyncPlayerListsPacket::handle);
            
        INSTANCE.registerMessage(id(), PMWhitelistTogglePacket.class, PMWhitelistTogglePacket::encode, PMWhitelistTogglePacket::new, PMWhitelistTogglePacket::handle);
            
        // Add more packets for GUI data sync
        INSTANCE.registerMessage(id(), SyncWorldListPacket.class, SyncWorldListPacket::encode, SyncWorldListPacket::new, SyncWorldListPacket::handle);
            
        INSTANCE.registerMessage(id(), RequestWorldListPacket.class, RequestWorldListPacket::encode, RequestWorldListPacket::new, RequestWorldListPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncWorldDetailPacket.class, SyncWorldDetailPacket::encode, SyncWorldDetailPacket::new, SyncWorldDetailPacket::handle);
            
        INSTANCE.registerMessage(id(), OpenGuiPacket.class, OpenGuiPacket::encode, OpenGuiPacket::new, OpenGuiPacket::handle);
            
        INSTANCE.registerMessage(id(), ConsoleCommandPacket.class, ConsoleCommandPacket::encode, ConsoleCommandPacket::new, ConsoleCommandPacket::handle);
        
        INSTANCE.registerMessage(id(), ConsoleResponsePacket.class, ConsoleResponsePacket::encode, ConsoleResponsePacket::new, ConsoleResponsePacket::handle);
        
        INSTANCE.registerMessage(id(), ConsoleSubscribePacket.class, ConsoleSubscribePacket::encode, ConsoleSubscribePacket::new, ConsoleSubscribePacket::handle);
        
        INSTANCE.registerMessage(id(), SyncGlobalSettingsPacket.class, SyncGlobalSettingsPacket::encode, SyncGlobalSettingsPacket::new, SyncGlobalSettingsPacket::handle);
        
        // Economy Management admin packets
        INSTANCE.registerMessage(id(), SaveTemplatePacket.class, SaveTemplatePacket::encode, SaveTemplatePacket::new, SaveTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), DeleteTemplatePacket.class, DeleteTemplatePacket::encode, DeleteTemplatePacket::new, DeleteTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), ToggleTemplatePacket.class, ToggleTemplatePacket::encode, ToggleTemplatePacket::new, ToggleTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), SaveFreeRewardSettingsPacket.class, SaveFreeRewardSettingsPacket::encode, SaveFreeRewardSettingsPacket::new, SaveFreeRewardSettingsPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncEconomyTemplatesPacket.class, SyncEconomyTemplatesPacket::encode, SyncEconomyTemplatesPacket::new, SyncEconomyTemplatesPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncBankAccountPacket.class, SyncBankAccountPacket::encode, SyncBankAccountPacket::new, SyncBankAccountPacket::handle);
        
        INSTANCE.registerMessage(id(), BankTransferPacket.class, BankTransferPacket::encode, BankTransferPacket::new, BankTransferPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncDailyTasksPacket.class, SyncDailyTasksPacket::encode, SyncDailyTasksPacket::new, SyncDailyTasksPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncAchievementsPacket.class, SyncAchievementsPacket::encode, SyncAchievementsPacket::new, SyncAchievementsPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimDailyTaskPacket.class, ClaimDailyTaskPacket::encode, ClaimDailyTaskPacket::new, ClaimDailyTaskPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimFreeRewardPacket.class, ClaimFreeRewardPacket::encode, ClaimFreeRewardPacket::new, ClaimFreeRewardPacket::handle);
        
        // MineBay packets
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.HoldItemPacket.class, com.servermanagement.network.packet.minebay.HoldItemPacket::encode, com.servermanagement.network.packet.minebay.HoldItemPacket::new, com.servermanagement.network.packet.minebay.HoldItemPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CreateListingPacket.class, com.servermanagement.network.packet.minebay.CreateListingPacket::encode, com.servermanagement.network.packet.minebay.CreateListingPacket::new, com.servermanagement.network.packet.minebay.CreateListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CancelListingPacket.class, com.servermanagement.network.packet.minebay.CancelListingPacket::encode, com.servermanagement.network.packet.minebay.CancelListingPacket::new, com.servermanagement.network.packet.minebay.CancelListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket.class, com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::encode, com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::new, com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.PurchaseListingPacket.class, com.servermanagement.network.packet.minebay.PurchaseListingPacket::encode, com.servermanagement.network.packet.minebay.PurchaseListingPacket::new, com.servermanagement.network.packet.minebay.PurchaseListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CreateOfferPacket.class, com.servermanagement.network.packet.minebay.CreateOfferPacket::encode, com.servermanagement.network.packet.minebay.CreateOfferPacket::new, com.servermanagement.network.packet.minebay.CreateOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.AcceptOfferPacket.class, com.servermanagement.network.packet.minebay.AcceptOfferPacket::encode, com.servermanagement.network.packet.minebay.AcceptOfferPacket::new, com.servermanagement.network.packet.minebay.AcceptOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.RejectOfferPacket.class, com.servermanagement.network.packet.minebay.RejectOfferPacket::encode, com.servermanagement.network.packet.minebay.RejectOfferPacket::new, com.servermanagement.network.packet.minebay.RejectOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.RequestListingOffersPacket.class, com.servermanagement.network.packet.minebay.RequestListingOffersPacket::encode, com.servermanagement.network.packet.minebay.RequestListingOffersPacket::new, com.servermanagement.network.packet.minebay.RequestListingOffersPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.SyncListingOffersPacket.class, com.servermanagement.network.packet.minebay.SyncListingOffersPacket::encode, com.servermanagement.network.packet.minebay.SyncListingOffersPacket::new, com.servermanagement.network.packet.minebay.SyncListingOffersPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.DeleteListingPacket.class, com.servermanagement.network.packet.minebay.DeleteListingPacket::encode, com.servermanagement.network.packet.minebay.DeleteListingPacket::new, com.servermanagement.network.packet.minebay.DeleteListingPacket::handle);
        
        // Bank inventory packets
        INSTANCE.registerMessage(id(), SyncBankInventoryPacket.class, SyncBankInventoryPacket::encode, SyncBankInventoryPacket::new, SyncBankInventoryPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimBankItemPacket.class, ClaimBankItemPacket::encode, ClaimBankItemPacket::new, ClaimBankItemPacket::handle);
        
        // OTA Update packets
        INSTANCE.registerMessage(id(), VersionCheckPacket.class, VersionCheckPacket::encode, VersionCheckPacket::new, VersionCheckPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileRequestPacket.class, ModFileRequestPacket::encode, ModFileRequestPacket::new, ModFileRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileChunkPacket.class, ModFileChunkPacket::encode, ModFileChunkPacket::new, ModFileChunkPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileCompletePacket.class, ModFileCompletePacket::encode, ModFileCompletePacket::new, ModFileCompletePacket::handle);
        
        INSTANCE.registerMessage(id(), CheckForUpdatesPacket.class, CheckForUpdatesPacket::encode, CheckForUpdatesPacket::new, CheckForUpdatesPacket::handle);
        
        INSTANCE.registerMessage(id(), StartServerUpdatePacket.class, StartServerUpdatePacket::encode, StartServerUpdatePacket::new, StartServerUpdatePacket::handle);
        
        INSTANCE.registerMessage(id(), SyncUpdateInfoPacket.class, SyncUpdateInfoPacket::encode, SyncUpdateInfoPacket::new, SyncUpdateInfoPacket::handle);

        // Session validation packets
        INSTANCE.registerMessage(id(), SyncSessionTokenPacket.class, SyncSessionTokenPacket::encode, SyncSessionTokenPacket::new, SyncSessionTokenPacket::handle);
        
        INSTANCE.registerMessage(id(), AuthenticateSessionPacket.class, AuthenticateSessionPacket::encode, AuthenticateSessionPacket::new, AuthenticateSessionPacket::handle);
        
        // Gambling packets
        INSTANCE.registerMessage(id(), PlaceGamblingBetPacket.class, PlaceGamblingBetPacket::encode, PlaceGamblingBetPacket::new, PlaceGamblingBetPacket::handle);
        
        INSTANCE.registerMessage(id(), PlaceGamblingBetWithItemPacket.class, PlaceGamblingBetWithItemPacket::encode, PlaceGamblingBetWithItemPacket::new, PlaceGamblingBetWithItemPacket::handle);
        
        INSTANCE.registerMessage(id(), GamblingResultPacket.class, GamblingResultPacket::encode, GamblingResultPacket::new, GamblingResultPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncBettingSlotStatePacket.class, SyncBettingSlotStatePacket::encode, SyncBettingSlotStatePacket::new, SyncBettingSlotStatePacket::handle);
        
        INSTANCE.registerMessage(id(), SyncGamblingStatsPacket.class, SyncGamblingStatsPacket::encode, SyncGamblingStatsPacket::new, SyncGamblingStatsPacket::handle);
        
        INSTANCE.registerMessage(id(), GamblingTensionPacket.class, GamblingTensionPacket::encode, GamblingTensionPacket::new, GamblingTensionPacket::handle);
        
        // Money Request packets
        INSTANCE.registerMessage(id(), SendMoneyRequestPacket.class, SendMoneyRequestPacket::encode, SendMoneyRequestPacket::new, SendMoneyRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), RespondMoneyRequestPacket.class, RespondMoneyRequestPacket::encode, RespondMoneyRequestPacket::new, RespondMoneyRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncMoneyRequestsPacket.class, SyncMoneyRequestsPacket::encode, SyncMoneyRequestsPacket::new, SyncMoneyRequestsPacket::handle);
        
        // Performance Settings packets
        INSTANCE.registerMessage(id(), SyncPerformanceSettingsPacket.class, SyncPerformanceSettingsPacket::encode, SyncPerformanceSettingsPacket::new, SyncPerformanceSettingsPacket::handle);
        
        INSTANCE.registerMessage(id(), UpdatePerformanceSettingPacket.class, UpdatePerformanceSettingPacket::encode, UpdatePerformanceSettingPacket::new, UpdatePerformanceSettingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.SyncMarketPricesPacket.class, com.servermanagement.network.packet.SyncMarketPricesPacket::encode, com.servermanagement.network.packet.SyncMarketPricesPacket::new, com.servermanagement.network.packet.SyncMarketPricesPacket::handle);

        // MOTD Editor packets
        INSTANCE.registerMessage(id(), SyncMotdPacket.class, SyncMotdPacket::encode, SyncMotdPacket::new, SyncMotdPacket::handle);

        INSTANCE.registerMessage(id(), SaveMotdPacket.class, SaveMotdPacket::encode, SaveMotdPacket::new, SaveMotdPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncEconomyStatsPacket.class, SyncEconomyStatsPacket::encode, SyncEconomyStatsPacket::new, SyncEconomyStatsPacket::handle);
        
        INSTANCE.registerMessage(id(), RequestEconomyStatsPacket.class, RequestEconomyStatsPacket::encode, RequestEconomyStatsPacket::new, RequestEconomyStatsPacket::handle);
        ServerManagementMod.LOGGER.info("Registered {} network packets", packetId);
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.debug("Client-side packet handlers ready");
    }
    
    public static void sendToServer(IPacket packet) {
        com.servermanagement.gui.debug.DebugLogger.logPacketSent(packet);
        INSTANCE.sendToServer(packet);
    }
    
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {
        com.servermanagement.gui.debug.DebugLogger.logPacketSent(packet);
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToAllPlayers(IPacket packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    public static SimpleChannel getChannel() {
        return INSTANCE.delegate;
    }

    private static boolean isAdminOnlyPacket(Object pkt) {
        if (pkt instanceof OpenGuiPacket ogp) {
            return ogp.guiType().isAdminOnly();
        }
        return pkt instanceof ToggleFeaturePacket ||
               pkt instanceof WMTogglePortalsPacket ||
               pkt instanceof WMSetTimerPacket ||
               pkt instanceof WMSetLobbyPacket ||
               pkt instanceof WMToggleChatIsolationPacket ||
               pkt instanceof WMToggleTabIsolationPacket ||
               pkt instanceof WMTeleportToDimensionPacket ||
               pkt instanceof PMSpectatePlayerPacket ||
               pkt instanceof PMViewInventoryPacket ||
               pkt instanceof PMKickPlayerPacket ||
               pkt instanceof PMBanPlayerPacket ||
               pkt instanceof PMUnbanPlayerPacket ||
               pkt instanceof PMWhitelistPacket ||
               pkt instanceof PMWhitelistTogglePacket ||
               pkt instanceof PMRequestPlayerListsPacket ||
               pkt instanceof ConsoleCommandPacket ||
               pkt instanceof ConsoleSubscribePacket ||
               pkt instanceof SaveTemplatePacket ||
               pkt instanceof DeleteTemplatePacket ||
               pkt instanceof ToggleTemplatePacket ||
               pkt instanceof SaveFreeRewardSettingsPacket ||
               pkt instanceof UpdatePerformanceSettingPacket ||
               pkt instanceof SaveMotdPacket ||
               pkt instanceof CheckForUpdatesPacket ||
               pkt instanceof StartServerUpdatePacket ||
               pkt instanceof RequestEconomyStatsPacket;
    }

    public static class WrappedSimpleChannel {
        private final SimpleChannel delegate;
        
        public WrappedSimpleChannel(SimpleChannel delegate) {
            this.delegate = delegate;
        }
        
        public <MSG> void registerMessage(int index, Class<MSG> messageType, 
                java.util.function.BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder, 
                java.util.function.Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder, 
                java.util.function.BiConsumer<MSG, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> messageConsumer) {
            delegate.registerMessage(index, messageType, encoder, decoder, (pkt, ctxSupplier) -> {
                net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
                ServerPlayer player = ctx.getSender();
                if (player != null && isAdminOnlyPacket(pkt)) {
                    if (!player.hasPermissions(2) || !com.servermanagement.security.SessionManager.getInstance().isSessionAuthenticated(player.getUUID())) {
                        com.mojang.logging.LogUtils.getLogger().warn("Player {} failed session token validation for packet {}", 
                            player.getName().getString(), pkt.getClass().getSimpleName());
                        ctx.setPacketHandled(true);
                        return;
                    }
                }
                messageConsumer.accept(pkt, ctxSupplier);
            });
        }
        
        public void sendToServer(Object message) {
            delegate.sendToServer(message);
        }
        
        public <MSG> void send(net.minecraftforge.network.PacketDistributor.PacketTarget target, MSG message) {
            delegate.send(target, message);
        }
    }
}
