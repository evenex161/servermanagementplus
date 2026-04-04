package com.servermanagement.network;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            // Accept any client version (enables OTA updates)
            clientVersion -> true,
            // Accept any server version (enables OTA updates)
            serverVersion -> true
        );

        ServerManagementMod.LOGGER.info("Registering network packets");
        
        // Config packets (bidirectional)
        INSTANCE.registerMessage(id(), ToggleFeaturePacket.class,
            ToggleFeaturePacket::encode,
            ToggleFeaturePacket::new,
            ToggleFeaturePacket::handle);
            
        INSTANCE.registerMessage(id(), ToggleAutoShowPacket.class,
            ToggleAutoShowPacket::encode,
            ToggleAutoShowPacket::new,
            ToggleAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), RequestAutoShowPacket.class,
            RequestAutoShowPacket::encode,
            RequestAutoShowPacket::new,
            RequestAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncAutoShowPacket.class,
            SyncAutoShowPacket::encode,
            SyncAutoShowPacket::new,
            SyncAutoShowPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncFeatureStatesPacket.class,
            SyncFeatureStatesPacket::encode,
            SyncFeatureStatesPacket::new,
            SyncFeatureStatesPacket::handle);
        
        // WorldManager packets (server-bound)
        INSTANCE.registerMessage(id(), WMTogglePortalsPacket.class,
            WMTogglePortalsPacket::encode,
            WMTogglePortalsPacket::new,
            WMTogglePortalsPacket::handle);
            
        INSTANCE.registerMessage(id(), WMSetTimerPacket.class,
            WMSetTimerPacket::encode,
            WMSetTimerPacket::new,
            WMSetTimerPacket::handle);
            
        INSTANCE.registerMessage(id(), WMSetLobbyPacket.class,
            WMSetLobbyPacket::encode,
            WMSetLobbyPacket::new,
            WMSetLobbyPacket::handle);
            
        INSTANCE.registerMessage(id(), WMToggleChatIsolationPacket.class,
            WMToggleChatIsolationPacket::encode,
            WMToggleChatIsolationPacket::new,
            WMToggleChatIsolationPacket::handle);
            
        INSTANCE.registerMessage(id(), WMToggleTabIsolationPacket.class,
            WMToggleTabIsolationPacket::encode,
            WMToggleTabIsolationPacket::new,
            WMToggleTabIsolationPacket::handle);
            
        INSTANCE.registerMessage(id(), WMTeleportToDimensionPacket.class,
            WMTeleportToDimensionPacket::encode,
            WMTeleportToDimensionPacket::new,
            WMTeleportToDimensionPacket::handle);
        
        // PlayerManager packets (server-bound)
        INSTANCE.registerMessage(id(), PMSpectatePlayerPacket.class,
            PMSpectatePlayerPacket::encode,
            PMSpectatePlayerPacket::new,
            PMSpectatePlayerPacket::handle);
            
        INSTANCE.registerMessage(id(), PMViewInventoryPacket.class,
            PMViewInventoryPacket::encode,
            PMViewInventoryPacket::new,
            PMViewInventoryPacket::handle);
            
        // Add more packets for GUI data sync
        INSTANCE.registerMessage(id(), SyncWorldListPacket.class,
            SyncWorldListPacket::encode,
            SyncWorldListPacket::new,
            SyncWorldListPacket::handle);
            
        INSTANCE.registerMessage(id(), RequestWorldListPacket.class,
            RequestWorldListPacket::encode,
            RequestWorldListPacket::new,
            RequestWorldListPacket::handle);
            
        INSTANCE.registerMessage(id(), SyncWorldDetailPacket.class,
            SyncWorldDetailPacket::encode,
            SyncWorldDetailPacket::new,
            SyncWorldDetailPacket::handle);
            
        INSTANCE.registerMessage(id(), OpenGuiPacket.class,
            OpenGuiPacket::encode,
            OpenGuiPacket::new,
            OpenGuiPacket::handle);
            
        INSTANCE.registerMessage(id(), ConsoleCommandPacket.class,
            ConsoleCommandPacket::encode,
            ConsoleCommandPacket::new,
            ConsoleCommandPacket::handle);
        
        INSTANCE.registerMessage(id(), ConsoleResponsePacket.class,
            ConsoleResponsePacket::encode,
            ConsoleResponsePacket::new,
            ConsoleResponsePacket::handle);
        
        INSTANCE.registerMessage(id(), SyncGlobalSettingsPacket.class,
            SyncGlobalSettingsPacket::encode,
            SyncGlobalSettingsPacket::new,
            SyncGlobalSettingsPacket::handle);
        
        // Economy Management admin packets
        INSTANCE.registerMessage(id(), SaveTemplatePacket.class,
            SaveTemplatePacket::encode,
            SaveTemplatePacket::new,
            SaveTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), DeleteTemplatePacket.class,
            DeleteTemplatePacket::encode,
            DeleteTemplatePacket::new,
            DeleteTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), ToggleTemplatePacket.class,
            ToggleTemplatePacket::encode,
            ToggleTemplatePacket::new,
            ToggleTemplatePacket::handle);
        
        INSTANCE.registerMessage(id(), SaveFreeRewardSettingsPacket.class,
            SaveFreeRewardSettingsPacket::encode,
            SaveFreeRewardSettingsPacket::new,
            SaveFreeRewardSettingsPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncEconomyTemplatesPacket.class,
            SyncEconomyTemplatesPacket::encode,
            SyncEconomyTemplatesPacket::new,
            SyncEconomyTemplatesPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncBankAccountPacket.class,
            SyncBankAccountPacket::encode,
            SyncBankAccountPacket::new,
            SyncBankAccountPacket::handle);
        
        INSTANCE.registerMessage(id(), BankTransferPacket.class,
            BankTransferPacket::encode,
            BankTransferPacket::new,
            BankTransferPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncDailyTasksPacket.class,
            SyncDailyTasksPacket::encode,
            SyncDailyTasksPacket::new,
            SyncDailyTasksPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncAchievementsPacket.class,
            SyncAchievementsPacket::encode,
            SyncAchievementsPacket::new,
            SyncAchievementsPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimDailyTaskPacket.class,
            ClaimDailyTaskPacket::encode,
            ClaimDailyTaskPacket::new,
            ClaimDailyTaskPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimFreeRewardPacket.class,
            ClaimFreeRewardPacket::encode,
            ClaimFreeRewardPacket::new,
            ClaimFreeRewardPacket::handle);
        
        // MineBay packets
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.HoldItemPacket.class,
            com.servermanagement.network.packet.minebay.HoldItemPacket::encode,
            com.servermanagement.network.packet.minebay.HoldItemPacket::new,
            com.servermanagement.network.packet.minebay.HoldItemPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CreateListingPacket.class,
            com.servermanagement.network.packet.minebay.CreateListingPacket::encode,
            com.servermanagement.network.packet.minebay.CreateListingPacket::new,
            com.servermanagement.network.packet.minebay.CreateListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CancelListingPacket.class,
            com.servermanagement.network.packet.minebay.CancelListingPacket::encode,
            com.servermanagement.network.packet.minebay.CancelListingPacket::new,
            com.servermanagement.network.packet.minebay.CancelListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket.class,
            com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::encode,
            com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::new,
            com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.PurchaseListingPacket.class,
            com.servermanagement.network.packet.minebay.PurchaseListingPacket::encode,
            com.servermanagement.network.packet.minebay.PurchaseListingPacket::new,
            com.servermanagement.network.packet.minebay.PurchaseListingPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.CreateOfferPacket.class,
            com.servermanagement.network.packet.minebay.CreateOfferPacket::encode,
            com.servermanagement.network.packet.minebay.CreateOfferPacket::new,
            com.servermanagement.network.packet.minebay.CreateOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.AcceptOfferPacket.class,
            com.servermanagement.network.packet.minebay.AcceptOfferPacket::encode,
            com.servermanagement.network.packet.minebay.AcceptOfferPacket::new,
            com.servermanagement.network.packet.minebay.AcceptOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.RejectOfferPacket.class,
            com.servermanagement.network.packet.minebay.RejectOfferPacket::encode,
            com.servermanagement.network.packet.minebay.RejectOfferPacket::new,
            com.servermanagement.network.packet.minebay.RejectOfferPacket::handle);
        
        INSTANCE.registerMessage(id(), com.servermanagement.network.packet.minebay.DeleteListingPacket.class,
            com.servermanagement.network.packet.minebay.DeleteListingPacket::encode,
            com.servermanagement.network.packet.minebay.DeleteListingPacket::new,
            com.servermanagement.network.packet.minebay.DeleteListingPacket::handle);
        
        // Bank inventory packets
        INSTANCE.registerMessage(id(), SyncBankInventoryPacket.class,
            SyncBankInventoryPacket::encode,
            SyncBankInventoryPacket::new,
            SyncBankInventoryPacket::handle);
        
        INSTANCE.registerMessage(id(), ClaimBankItemPacket.class,
            ClaimBankItemPacket::encode,
            ClaimBankItemPacket::new,
            ClaimBankItemPacket::handle);
        
        // OTA Update packets
        INSTANCE.registerMessage(id(), VersionCheckPacket.class,
            VersionCheckPacket::encode,
            VersionCheckPacket::new,
            VersionCheckPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileRequestPacket.class,
            ModFileRequestPacket::encode,
            ModFileRequestPacket::new,
            ModFileRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileChunkPacket.class,
            ModFileChunkPacket::encode,
            ModFileChunkPacket::new,
            ModFileChunkPacket::handle);
        
        INSTANCE.registerMessage(id(), ModFileCompletePacket.class,
            ModFileCompletePacket::encode,
            ModFileCompletePacket::new,
            ModFileCompletePacket::handle);
        
        // Gambling packets
        INSTANCE.registerMessage(id(), PlaceGamblingBetPacket.class,
            PlaceGamblingBetPacket::encode,
            PlaceGamblingBetPacket::new,
            PlaceGamblingBetPacket::handle);
        
        INSTANCE.registerMessage(id(), PlaceGamblingBetWithItemPacket.class,
            PlaceGamblingBetWithItemPacket::encode,
            PlaceGamblingBetWithItemPacket::new,
            PlaceGamblingBetWithItemPacket::handle);
        
        INSTANCE.registerMessage(id(), GamblingResultPacket.class,
            GamblingResultPacket::encode,
            GamblingResultPacket::new,
            GamblingResultPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncBettingSlotStatePacket.class,
            SyncBettingSlotStatePacket::encode,
            SyncBettingSlotStatePacket::new,
            SyncBettingSlotStatePacket::handle);
        
        INSTANCE.registerMessage(id(), SyncGamblingStatsPacket.class,
            SyncGamblingStatsPacket::encode,
            SyncGamblingStatsPacket::new,
            SyncGamblingStatsPacket::handle);
        
        INSTANCE.registerMessage(id(), GamblingTensionPacket.class,
            GamblingTensionPacket::encode,
            GamblingTensionPacket::new,
            GamblingTensionPacket::handle);
        
        // Money Request packets
        INSTANCE.registerMessage(id(), SendMoneyRequestPacket.class,
            SendMoneyRequestPacket::encode,
            SendMoneyRequestPacket::new,
            SendMoneyRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), RespondMoneyRequestPacket.class,
            RespondMoneyRequestPacket::encode,
            RespondMoneyRequestPacket::new,
            RespondMoneyRequestPacket::handle);
        
        INSTANCE.registerMessage(id(), SyncMoneyRequestsPacket.class,
            SyncMoneyRequestsPacket::encode,
            SyncMoneyRequestsPacket::new,
            SyncMoneyRequestsPacket::handle);
        
        ServerManagementMod.LOGGER.info("Registered {} network packets", packetId);
    }

    public static void registerClientPackets() {
        ServerManagementMod.LOGGER.info("Client-side packet handlers ready");
    }
    
    public static void sendToServer(IPacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToAllPlayers(IPacket packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    public static SimpleChannel getChannel() {
        return INSTANCE;
    }
}
