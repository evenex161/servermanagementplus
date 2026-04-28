package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.MoneyRequest;
import com.servermanagement.features.economy.MoneyRequestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client ÔåÆ Server: Respond to a money request (accept, deny, or cancel)
 */
public record RespondMoneyRequestPacket(UUID requestId, Action action) implements IPacket {
    
    public enum Action {
        ACCEPT, DENY, CANCEL
    }

    public RespondMoneyRequestPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readEnum(Action.class));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.requestId);
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        Supplier<NetworkEvent.Context> context = contextSupplier;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            EconomyManager econ = EconomyManager.getInstance(player.server);
            MoneyRequestManager reqManager = econ.getRequestManager();
            MoneyRequest request = reqManager.findRequest(requestId);

            if (request == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "┬ºcRequest not found or already processed"));
                return;
            }

            switch (action) {
                case ACCEPT -> {
                    // Player is the target ÔÇö they pay the requester
                    if (!request.getTargetUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºcYou cannot accept this request"));
                        return;
                    }

                    // Check balance
                    BankAccount payerAccount = econ.getOrCreateAccount(player.getUUID());
                    if (payerAccount.getBalance() < request.getAmount()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºcInsufficient funds to fulfill this request"));
                        return;
                    }

                    // Perform transfer
                    boolean success = econ.transfer(player.getUUID(), request.getRequesterUUID(), request.getAmount());
                    if (!success) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºcTransfer failed"));
                        return;
                    }

                    reqManager.acceptRequest(requestId, player.getUUID());
                    reqManager.save(player.server);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        String.format("┬ºaPaid $%.2f to fulfill the request", request.getAmount())));

                    // Notify requester if online
                    ServerPlayer requester = player.server.getPlayerList().getPlayer(request.getRequesterUUID());
                    if (requester != null) {
                        requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            String.format("┬ºa%s accepted your money request for $%.2f!",
                                player.getName().getString(), request.getAmount())));
                        // Sync both players' bank data + requests
                        syncBankAndRequests(requester, econ);
                    }
                    syncBankAndRequests(player, econ);
                }
                case DENY -> {
                    if (!request.getTargetUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºcYou cannot deny this request"));
                        return;
                    }

                    reqManager.denyRequest(requestId, player.getUUID());
                    reqManager.save(player.server);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("┬º7Request denied"));

                    // Notify requester if online
                    ServerPlayer requester = player.server.getPlayerList().getPlayer(request.getRequesterUUID());
                    if (requester != null) {
                        requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            String.format("┬ºc%s denied your request for $%.2f",
                                player.getName().getString(), request.getAmount())));
                        SendMoneyRequestPacket.syncRequestsToPlayer(requester, econ);
                    }
                    SendMoneyRequestPacket.syncRequestsToPlayer(player, econ);
                }
                case CANCEL -> {
                    if (!request.getRequesterUUID().equals(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "┬ºcYou cannot cancel this request"));
                        return;
                    }

                    reqManager.cancelRequest(requestId, player.getUUID());
                    reqManager.save(player.server);

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("┬º7Request cancelled"));

                    // Notify target if online
                    ServerPlayer target = player.server.getPlayerList().getPlayer(request.getTargetUUID());
                    if (target != null) {
                        SendMoneyRequestPacket.syncRequestsToPlayer(target, econ);
                    }
                    SendMoneyRequestPacket.syncRequestsToPlayer(player, econ);
                }
            }
        });
        context.setPacketHandled(true);
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
