package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.MoneyRequest;
import com.servermanagement.features.economy.MoneyRequestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: Respond to a money request (accept, deny, or cancel)
 */
public record RespondMoneyRequestPacket(UUID requestId, Action action) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RespondMoneyRequestPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("servermanagement", "respond_money_request_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, RespondMoneyRequestPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), RespondMoneyRequestPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    
    public enum Action {
        ACCEPT, DENY, CANCEL
    }
    public RespondMoneyRequestPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readEnum(Action.class));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.requestId);
        buf.writeEnum(this.action);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
            if (player == null) return;

            EconomyManager econ = EconomyManager.getInstance(player.level().getServer());
            MoneyRequestManager reqManager = econ.getRequestManager();
            MoneyRequest request = reqManager.findRequest(requestId);

            if (request == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cRequest not found or already processed"));
                return;
            }

            switch (action) {
                case ACCEPT -> {
                    // Player is the target — they pay the requester
                    if (!request.getTargetUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cYou cannot accept this request"));
                        return;
                    }

                    // Check balance
                    BankAccount payerAccount = econ.getOrCreateAccount(player.getUUID());
                    if (payerAccount.getBalance() < request.getAmount()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cInsufficient funds to fulfill this request"));
                        return;
                    }

                    // Perform transfer
                    boolean success = econ.transfer(player.getUUID(), request.getRequesterUUID(), request.getAmount());
                    if (!success) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cTransfer failed"));
                        return;
                    }

                    reqManager.acceptRequest(requestId, player.getUUID());
                    reqManager.save(player.level().getServer());

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§aPaid $%.2f to fulfill the request", request.getAmount())));

                    // Notify requester if online
                    ServerPlayer requester = player.level().getServer().getPlayerList().getPlayer(request.getRequesterUUID());
                    if (requester != null) {
                        requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§a%s accepted your money request for $%.2f!",
                                player.getName().getString(), request.getAmount())));
                        // Sync both players' bank data + requests
                        syncBankAndRequests(requester, econ);
                    }
                    syncBankAndRequests(player, econ);
                }
                case DENY -> {
                    if (!request.getTargetUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cYou cannot deny this request"));
                        return;
                    }

                    reqManager.denyRequest(requestId, player.getUUID());
                    reqManager.save(player.level().getServer());

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Request denied"));

                    // Notify requester if online
                    ServerPlayer requester = player.level().getServer().getPlayerList().getPlayer(request.getRequesterUUID());
                    if (requester != null) {
                        requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§c%s denied your request for $%.2f",
                                player.getName().getString(), request.getAmount())));
                        SendMoneyRequestPacket.syncRequestsToPlayer(requester, econ);
                    }
                    SendMoneyRequestPacket.syncRequestsToPlayer(player, econ);
                }
                case CANCEL -> {
                    if (!request.getRequesterUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cYou cannot cancel this request"));
                        return;
                    }

                    reqManager.cancelRequest(requestId, player.getUUID());
                    reqManager.save(player.level().getServer());

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Request cancelled"));

                    // Notify target if online
                    ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(request.getTargetUUID());
                    if (target != null) {
                        SendMoneyRequestPacket.syncRequestsToPlayer(target, econ);
                    }
                    SendMoneyRequestPacket.syncRequestsToPlayer(player, econ);
                }
            }
    }

    private void syncBankAndRequests(ServerPlayer player, EconomyManager econ) {
        // Sync bank balance
        BankAccount account = econ.getOrCreateAccount(player.getUUID());
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncBankAccountPacket(account.getBalance(), account.getTransactions()),
            player);
        // Sync requests
        SendMoneyRequestPacket.syncRequestsToPlayer(player, econ);
    }
}
