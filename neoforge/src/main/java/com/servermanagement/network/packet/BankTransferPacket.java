package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.regex.Pattern;

/**
 * Packet for transferring money between players
 */
public record BankTransferPacket(String targetPlayerName, double amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BankTransferPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "bank_transfer"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, BankTransferPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), BankTransferPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    
    public BankTransferPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16), buf.readDouble());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.targetPlayerName, 16);
        buf.writeDouble(this.amount);
    }
    
    public void handle(IPayloadContext context) {
context.enqueueWork(() -> {
            ServerPlayer sender = (context.player() instanceof ServerPlayer ? (ServerPlayer) context.player() : null);
            if (sender == null) {
                return; // No sender - reject packet
            }
            
            // Input validation - prevent exploits
            if (this.targetPlayerName == null || this.targetPlayerName.trim().isEmpty()) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid player name"));
                return;
            }
            
            // Sanitize player name (prevent injection/exploits)
            if (this.targetPlayerName.length() > 16 || !PLAYER_NAME_PATTERN.matcher(this.targetPlayerName).matches()) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid player name format"));
                return;
            }
            
            if (Double.isNaN(this.amount) || Double.isInfinite(this.amount) || this.amount <= 0) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cInvalid transfer amount"));
                return;
            }
            
            if (this.amount < 0.01 || this.amount > 1000000.0) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cTransfer amount must be between $0.01 and $1,000,000"));
                return;
            }
            
            // Get target player
            ServerPlayer target = sender.level().getServer().getPlayerList().getPlayerByName(targetPlayerName);
            if (target == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cPlayer not found: " + targetPlayerName));
                return;
            }
            
            if (target.getUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cYou cannot transfer money to yourself"));
                return;
            }
            
            // Perform transfer (transfer method only takes 3 parameters)
            EconomyManager manager = EconomyManager.getInstance(sender.level().getServer());
            boolean success = manager.transfer(sender.getUUID(), target.getUUID(), amount);
            
            if (success) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    String.format("§aTransferred $%.2f to %s", amount, target.getName().getString())));
                target.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    String.format("§aReceived $%.2f from %s", amount, sender.getName().getString())));
                
                // Sync balances and transactions
                BankAccount senderAccount = manager.getOrCreateAccount(sender.getUUID());
                BankAccount targetAccount = manager.getOrCreateAccount(target.getUUID());
                
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncBankAccountPacket(
                        senderAccount.getBalance(), 
                        senderAccount.getTransactions()
                    ), sender);
                    
                com.servermanagement.network.ModNetworking.sendToPlayer(
                    new SyncBankAccountPacket(
                        targetAccount.getBalance(), 
                        targetAccount.getTransactions()
                    ), target);
            } else {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cTransfer failed - insufficient funds"));
            }
        });
}
}
