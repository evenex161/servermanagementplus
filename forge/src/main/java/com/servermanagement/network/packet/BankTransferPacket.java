package com.servermanagement.network.packet;

import com.servermanagement.features.economy.BankAccount;
import com.servermanagement.features.economy.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Packet for transferring money between players
 */
public record BankTransferPacket(String targetPlayerName, double amount) implements IPacket {
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    
    public BankTransferPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(32767), buf.readDouble());
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.targetPlayerName, 32767);
        buf.writeDouble(this.amount);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
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
            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(targetPlayerName);
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
            EconomyManager manager = EconomyManager.getInstance(sender.server);
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
        ctx.get().setPacketHandled(true);
    }
}
