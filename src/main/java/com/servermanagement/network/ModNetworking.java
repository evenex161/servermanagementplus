package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

        ServerManagementMod.LOGGER.info("Registering network packets");
        
        // Config packets (bidirectional)
        INSTANCE.messageBuilder(ToggleFeaturePacket.class, id())
            .encoder(ToggleFeaturePacket::encode)
            .decoder(ToggleFeaturePacket::new)
            .consumer(ToggleFeaturePacket::handle)
            .add();
            
        INSTANCE.messageBuilder(ToggleAutoShowPacket.class, id())
            .encoder(ToggleAutoShowPacket::encode)
            .decoder(ToggleAutoShowPacket::new)
            .consumer(ToggleAutoShowPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(RequestAutoShowPacket.class, id())
            .encoder(RequestAutoShowPacket::encode)
            .decoder(RequestAutoShowPacket::new)
            .consumer(RequestAutoShowPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncAutoShowPacket.class, id())
            .encoder(SyncAutoShowPacket::encode)
            .decoder(SyncAutoShowPacket::new)
            .consumer(SyncAutoShowPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncFeatureStatesPacket.class, id())
            .encoder(SyncFeatureStatesPacket::encode)
            .decoder(SyncFeatureStatesPacket::new)
            .consumer(SyncFeatureStatesPacket::handle)
            .add();
        
        // WorldManager packets (server-bound)
        INSTANCE.messageBuilder(WMTogglePortalsPacket.class, id())
            .encoder(WMTogglePortalsPacket::encode)
            .decoder(WMTogglePortalsPacket::new)
            .consumer(WMTogglePortalsPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(WMSetTimerPacket.class, id())
            .encoder(WMSetTimerPacket::encode)
            .decoder(WMSetTimerPacket::new)
            .consumer(WMSetTimerPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(WMSetLobbyPacket.class, id())
            .encoder(WMSetLobbyPacket::encode)
            .decoder(WMSetLobbyPacket::new)
            .consumer(WMSetLobbyPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(WMToggleChatIsolationPacket.class, id())
            .encoder(WMToggleChatIsolationPacket::encode)
            .decoder(WMToggleChatIsolationPacket::new)
            .consumer(WMToggleChatIsolationPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(WMToggleTabIsolationPacket.class, id())
            .encoder(WMToggleTabIsolationPacket::encode)
            .decoder(WMToggleTabIsolationPacket::new)
            .consumer(WMToggleTabIsolationPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(WMTeleportToDimensionPacket.class, id())
            .encoder(WMTeleportToDimensionPacket::encode)
            .decoder(WMTeleportToDimensionPacket::new)
            .consumer(WMTeleportToDimensionPacket::handle)
            .add();
        
        // PlayerManager packets (server-bound)
        INSTANCE.messageBuilder(PMSpectatePlayerPacket.class, id())
            .encoder(PMSpectatePlayerPacket::encode)
            .decoder(PMSpectatePlayerPacket::new)
            .consumer(PMSpectatePlayerPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(PMViewInventoryPacket.class, id())
            .encoder(PMViewInventoryPacket::encode)
            .decoder(PMViewInventoryPacket::new)
            .consumer(PMViewInventoryPacket::handle)
            .add();
            
        // Add more packets for GUI data sync
        INSTANCE.messageBuilder(SyncWorldListPacket.class, id())
            .encoder(SyncWorldListPacket::encode)
            .decoder(SyncWorldListPacket::new)
            .consumer(SyncWorldListPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(RequestWorldListPacket.class, id())
            .encoder(RequestWorldListPacket::encode)
            .decoder(RequestWorldListPacket::new)
            .consumer(RequestWorldListPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(SyncWorldDetailPacket.class, id())
            .encoder(SyncWorldDetailPacket::encode)
            .decoder(SyncWorldDetailPacket::new)
            .consumer(SyncWorldDetailPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(OpenGuiPacket.class, id())
            .encoder(OpenGuiPacket::encode)
            .decoder(OpenGuiPacket::new)
            .consumer(OpenGuiPacket::handle)
            .add();
            
        INSTANCE.messageBuilder(ConsoleCommandPacket.class, id())
            .encoder(ConsoleCommandPacket::encode)
            .decoder(ConsoleCommandPacket::new)
            .consumer(ConsoleCommandPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ConsoleResponsePacket.class, id())
            .encoder(ConsoleResponsePacket::encode)
            .decoder(ConsoleResponsePacket::new)
            .consumer(ConsoleResponsePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ConsoleSubscribePacket.class, id())
            .encoder(ConsoleSubscribePacket::encode)
            .decoder(ConsoleSubscribePacket::new)
            .consumer(ConsoleSubscribePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncGlobalSettingsPacket.class, id())
            .encoder(SyncGlobalSettingsPacket::encode)
            .decoder(SyncGlobalSettingsPacket::new)
            .consumer(SyncGlobalSettingsPacket::handle)
            .add();
        
        // Economy Management admin packets
        INSTANCE.messageBuilder(SaveTemplatePacket.class, id())
            .encoder(SaveTemplatePacket::encode)
            .decoder(SaveTemplatePacket::new)
            .consumer(SaveTemplatePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(DeleteTemplatePacket.class, id())
            .encoder(DeleteTemplatePacket::encode)
            .decoder(DeleteTemplatePacket::new)
            .consumer(DeleteTemplatePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ToggleTemplatePacket.class, id())
            .encoder(ToggleTemplatePacket::encode)
            .decoder(ToggleTemplatePacket::new)
            .consumer(ToggleTemplatePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SaveFreeRewardSettingsPacket.class, id())
            .encoder(SaveFreeRewardSettingsPacket::encode)
            .decoder(SaveFreeRewardSettingsPacket::new)
            .consumer(SaveFreeRewardSettingsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncEconomyTemplatesPacket.class, id())
            .encoder(SyncEconomyTemplatesPacket::encode)
            .decoder(SyncEconomyTemplatesPacket::new)
            .consumer(SyncEconomyTemplatesPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncBankAccountPacket.class, id())
            .encoder(SyncBankAccountPacket::encode)
            .decoder(SyncBankAccountPacket::new)
            .consumer(SyncBankAccountPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(BankTransferPacket.class, id())
            .encoder(BankTransferPacket::encode)
            .decoder(BankTransferPacket::new)
            .consumer(BankTransferPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncDailyTasksPacket.class, id())
            .encoder(SyncDailyTasksPacket::encode)
            .decoder(SyncDailyTasksPacket::new)
            .consumer(SyncDailyTasksPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncAchievementsPacket.class, id())
            .encoder(SyncAchievementsPacket::encode)
            .decoder(SyncAchievementsPacket::new)
            .consumer(SyncAchievementsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ClaimDailyTaskPacket.class, id())
            .encoder(ClaimDailyTaskPacket::encode)
            .decoder(ClaimDailyTaskPacket::new)
            .consumer(ClaimDailyTaskPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ClaimFreeRewardPacket.class, id())
            .encoder(ClaimFreeRewardPacket::encode)
            .decoder(ClaimFreeRewardPacket::new)
            .consumer(ClaimFreeRewardPacket::handle)
            .add();
        
        // MineBay packets
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.HoldItemPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.HoldItemPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.HoldItemPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.HoldItemPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.CreateListingPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.CreateListingPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.CreateListingPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.CreateListingPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.CancelListingPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.CancelListingPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.CancelListingPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.CancelListingPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.PurchaseListingPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.PurchaseListingPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.PurchaseListingPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.PurchaseListingPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.CreateOfferPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.CreateOfferPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.CreateOfferPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.CreateOfferPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.AcceptOfferPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.AcceptOfferPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.AcceptOfferPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.AcceptOfferPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.RejectOfferPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.RejectOfferPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.RejectOfferPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.RejectOfferPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.RequestListingOffersPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.RequestListingOffersPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.RequestListingOffersPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.RequestListingOffersPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.SyncListingOffersPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.SyncListingOffersPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.SyncListingOffersPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.SyncListingOffersPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.minebay.DeleteListingPacket.class, id())
            .encoder(com.servermanagement.network.packet.minebay.DeleteListingPacket::encode)
            .decoder(com.servermanagement.network.packet.minebay.DeleteListingPacket::new)
            .consumer(com.servermanagement.network.packet.minebay.DeleteListingPacket::handle)
            .add();
        
        // Bank inventory packets
        INSTANCE.messageBuilder(SyncBankInventoryPacket.class, id())
            .encoder(SyncBankInventoryPacket::encode)
            .decoder(SyncBankInventoryPacket::new)
            .consumer(SyncBankInventoryPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ClaimBankItemPacket.class, id())
            .encoder(ClaimBankItemPacket::encode)
            .decoder(ClaimBankItemPacket::new)
            .consumer(ClaimBankItemPacket::handle)
            .add();
        
        // OTA Update packets
        INSTANCE.messageBuilder(VersionCheckPacket.class, id())
            .encoder(VersionCheckPacket::encode)
            .decoder(VersionCheckPacket::new)
            .consumer(VersionCheckPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ModFileRequestPacket.class, id())
            .encoder(ModFileRequestPacket::encode)
            .decoder(ModFileRequestPacket::new)
            .consumer(ModFileRequestPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ModFileChunkPacket.class, id())
            .encoder(ModFileChunkPacket::encode)
            .decoder(ModFileChunkPacket::new)
            .consumer(ModFileChunkPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ModFileCompletePacket.class, id())
            .encoder(ModFileCompletePacket::encode)
            .decoder(ModFileCompletePacket::new)
            .consumer(ModFileCompletePacket::handle)
            .add();
        
        // Gambling packets
        INSTANCE.messageBuilder(PlaceGamblingBetPacket.class, id())
            .encoder(PlaceGamblingBetPacket::encode)
            .decoder(PlaceGamblingBetPacket::new)
            .consumer(PlaceGamblingBetPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(PlaceGamblingBetWithItemPacket.class, id())
            .encoder(PlaceGamblingBetWithItemPacket::encode)
            .decoder(PlaceGamblingBetWithItemPacket::new)
            .consumer(PlaceGamblingBetWithItemPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(GamblingResultPacket.class, id())
            .encoder(GamblingResultPacket::encode)
            .decoder(GamblingResultPacket::new)
            .consumer(GamblingResultPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncBettingSlotStatePacket.class, id())
            .encoder(SyncBettingSlotStatePacket::encode)
            .decoder(SyncBettingSlotStatePacket::new)
            .consumer(SyncBettingSlotStatePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncGamblingStatsPacket.class, id())
            .encoder(SyncGamblingStatsPacket::encode)
            .decoder(SyncGamblingStatsPacket::new)
            .consumer(SyncGamblingStatsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(GamblingTensionPacket.class, id())
            .encoder(GamblingTensionPacket::encode)
            .decoder(GamblingTensionPacket::new)
            .consumer(GamblingTensionPacket::handle)
            .add();
        
        // Money Request packets
        INSTANCE.messageBuilder(SendMoneyRequestPacket.class, id())
            .encoder(SendMoneyRequestPacket::encode)
            .decoder(SendMoneyRequestPacket::new)
            .consumer(SendMoneyRequestPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(RespondMoneyRequestPacket.class, id())
            .encoder(RespondMoneyRequestPacket::encode)
            .decoder(RespondMoneyRequestPacket::new)
            .consumer(RespondMoneyRequestPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncMoneyRequestsPacket.class, id())
            .encoder(SyncMoneyRequestsPacket::encode)
            .decoder(SyncMoneyRequestsPacket::new)
            .consumer(SyncMoneyRequestsPacket::handle)
            .add();
        
        // Performance Settings packets
        INSTANCE.messageBuilder(SyncPerformanceSettingsPacket.class, id())
            .encoder(SyncPerformanceSettingsPacket::encode)
            .decoder(SyncPerformanceSettingsPacket::new)
            .consumer(SyncPerformanceSettingsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(UpdatePerformanceSettingPacket.class, id())
            .encoder(UpdatePerformanceSettingPacket::encode)
            .decoder(UpdatePerformanceSettingPacket::new)
            .consumer(UpdatePerformanceSettingPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(com.servermanagement.network.packet.SyncMarketPricesPacket.class, id())
            .encoder(com.servermanagement.network.packet.SyncMarketPricesPacket::encode)
            .decoder(com.servermanagement.network.packet.SyncMarketPricesPacket::new)
            .consumer(com.servermanagement.network.packet.SyncMarketPricesPacket::handle)
            .add();

        // MOTD Editor packets
        INSTANCE.messageBuilder(SyncMotdPacket.class, id())
            .encoder(SyncMotdPacket::encode)
            .decoder(SyncMotdPacket::new)
            .consumer(SyncMotdPacket::handle)
            .add();

        INSTANCE.messageBuilder(SaveMotdPacket.class, id())
            .encoder(SaveMotdPacket::encode)
            .decoder(SaveMotdPacket::new)
            .consumer(SaveMotdPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(SyncEconomyStatsPacket.class, id())
            .encoder(SyncEconomyStatsPacket::encode)
            .decoder(SyncEconomyStatsPacket::new)
            .consumer(SyncEconomyStatsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(RequestEconomyStatsPacket.class, id())
            .encoder(RequestEconomyStatsPacket::encode)
            .decoder(RequestEconomyStatsPacket::new)
            .consumer(RequestEconomyStatsPacket::handle)
            .add();
        
        INSTANCE.build();
        
        ServerManagementMod.LOGGER.info("Registered {} network packets", packetId);
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.info("Client-side packet handlers ready");
    }
    
    public static void sendToServer(IPacket packet) {
        INSTANCE.send(packet, PacketDistributor.SERVER.noArg());
    }
    
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {
        INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
    }
    
    public static void sendToAllPlayers(IPacket packet) {
        INSTANCE.send(packet, PacketDistributor.ALL.noArg());
    }
    
    public static SimpleChannel getChannel() {
        return INSTANCE;
    }
}
