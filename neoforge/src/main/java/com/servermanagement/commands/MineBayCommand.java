package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MineBayCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("minebay")
                .executes(MineBayCommand::openMineBayGUI)
        );
    }
    
    private static int openMineBayGUI(CommandContext<CommandSourceStack> context) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            context.getSource().sendFailure(Component.literal("MineBay is currently disabled because Economy is disabled!"));
            return 0;
        }
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Sync balance before opening
            var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
            var account = economyManager.getOrCreateAccount(player.getUUID());
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.SyncBankAccountPacket(
                    account.getBalance(),
                    account.getTransactions()
                ),
                player
            );
            
            // Sync listings before opening
            var mineBayManager = com.servermanagement.features.minebay.MineBayManager.getInstance();
            var listings = mineBayManager.getActiveListings();
            com.servermanagement.network.ModNetworking.sendToPlayer(
                new com.servermanagement.network.packet.minebay.SyncMineBayListingsPacket(listings),
                player
            );
            
            // Open GUI directly
            player.openMenu(new com.servermanagement.gui.minebay.MineBayMenuProvider());
            return 1;
        }
        return 0;
    }
}
