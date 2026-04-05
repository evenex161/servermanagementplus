package com.servermanagement.network.packet;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.client.ClientMoneyRequestData;
import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import com.servermanagement.features.economy.MoneyRequest;
import com.servermanagement.features.economy.MoneyRequestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client → Server: Create a new money request
 */
public class SendMoneyRequestPacket implements IPacket {
    public static final CustomPacketPayload.Type<SendMoneyRequestPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "send_money_request_packet"));
    
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SendMoneyRequestPacket> STREAM_CODEC = 
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SendMoneyRequestPacket::new);
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    private final String targetPlayerName;
    private final double amount;
    private final String message;

    public SendMoneyRequestPacket(String targetPlayerName, double amount, String message) {
        this.targetPlayerName = targetPlayerName;
        this.amount = amount;
        this.message = message;
    }

    public SendMoneyRequestPacket(FriendlyByteBuf buf) {
        this.targetPlayerName = buf.readUtf(16);
        this.amount = buf.readDouble();
        this.message = buf.readUtf(256);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.targetPlayerName, 16);
        buf.writeDouble(this.amount);
        buf.writeUtf(this.message, 256);
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = (ServerPlayer) context.player();
            if (sender == null) return;

            // Validate player name
            if (this.targetPlayerName == null || this.targetPlayerName.trim().isEmpty()
                    || this.targetPlayerName.length() > 16
                    || !this.targetPlayerName.matches("[a-zA-Z0-9_]+")) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid player name"));
                return;
            }

            // Validate amount
            if (Double.isNaN(this.amount) || Double.isInfinite(this.amount)
                    || this.amount < 0.01 || this.amount > 1000000.0) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cAmount must be between $0.01 and $1,000,000"));
                return;
            }

            // Sanitize message
            String safeMessage = this.message == null ? "" : this.message.trim();
            if (safeMessage.length() > 100) {
                safeMessage = safeMessage.substring(0, 100);
            }

            // Find target player (must be online)
            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(targetPlayerName);
            if (target == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cPlayer not found: " + targetPlayerName));
                return;
            }

            if (target.getUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cYou cannot request money from yourself"));
                return;
            }

            // Create the request
            EconomyManager econ = EconomyManager.getInstance(sender.server);
            MoneyRequestManager reqManager = econ.getRequestManager();
            MoneyRequest request = reqManager.createRequest(
                sender.getUUID(), target.getUUID(), this.amount, safeMessage);

            if (request == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cYou have too many pending requests (max 10)"));
                return;
            }

            reqManager.save(sender.server);

            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                String.format("§aRequest sent to %s for $%.2f", target.getName().getString(), this.amount)));

            // Notify target player
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                String.format("§e%s is requesting $%.2f from you. Open your Bank to respond.",
                    sender.getName().getString(), this.amount)));

            // Sync updated request lists to both players
            syncRequestsToPlayer(sender, econ);
            syncRequestsToPlayer(target, econ);
        });
        
    }

    static void syncRequestsToPlayer(ServerPlayer player, EconomyManager econ) {
        MoneyRequestManager reqManager = econ.getRequestManager();
        java.util.UUID uuid = player.getUUID();

        List<MoneyRequest> pendingIncoming = reqManager.getPendingIncomingRequests(uuid);
        List<MoneyRequest> outgoing = reqManager.getPendingRequestsByRequester(uuid);

        List<ClientMoneyRequestData.RequestEntry> inEntries = new ArrayList<>();
        for (MoneyRequest req : pendingIncoming) {
            String name = getPlayerName(player, req.getRequesterUUID());
            inEntries.add(new ClientMoneyRequestData.RequestEntry(
                req.getRequestId(), name, req.getAmount(),
                req.getMessage(), req.getFormattedAge(), req.getStatus().name()));
        }

        List<ClientMoneyRequestData.RequestEntry> outEntries = new ArrayList<>();
        for (MoneyRequest req : outgoing) {
            String name = getPlayerName(player, req.getTargetUUID());
            outEntries.add(new ClientMoneyRequestData.RequestEntry(
                req.getRequestId(), name, req.getAmount(),
                req.getMessage(), req.getFormattedAge(), req.getStatus().name()));
        }

        com.servermanagement.network.ModNetworking.sendToPlayer(
            new SyncMoneyRequestsPacket(inEntries, outEntries), player);
    }

    private static String getPlayerName(ServerPlayer context, java.util.UUID uuid) {
        ServerPlayer p = context.server.getPlayerList().getPlayer(uuid);
        if (p != null) return p.getName().getString();
        // Fallback: try usercache
        var profile = context.server.getProfileCache().get(uuid);
        return profile.map(com.mojang.authlib.GameProfile::getName).orElse("Unknown");
    }
}
