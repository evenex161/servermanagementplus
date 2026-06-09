package com.servermanagement.features.economy;

import com.servermanagement.features.Feature;
import net.minecraft.server.MinecraftServer;

/**
 * Economy Feature - Provides a money system with bank accounts for all players.
 * Players can earn money through various means and use it for transactions.
 */
public class EconomyFeature implements Feature {
    
    @Override
    public String getId() {
        return "economy";
    }

    @Override
    public String getDisplayName() {
        return "Economy";
    }

    @Override
    public String getDescription() {
        return "Money system with bank accounts for all players";
    }

    @Override
    public String getDetailedDescription() {
        return "Economy Feature:\n\n" +
               "• Bank accounts for every player\n" +
               "• View balance and transaction history\n" +
               "• Transfer money between players\n" +
               "• Command: /bank to access your account\n\n" +
               "This feature provides a comprehensive money system where players can:\n" +
               "- Check their balance\n" +
               "- View transaction history (last 50 transactions)\n" +
               "- Send money to other players\n" +
               "- Receive money from various sources\n\n" +
               "Admins can manage player balances using /bank admin commands.";
    }

    @Override
    public void initialize(MinecraftServer server) {
        // Initialize the economy manager
        EconomyManager.getInstance().initialize(server);
    }

    @Override
    public void onEnable() {
        // Called when feature is enabled
    }

    @Override
    public void onDisable() {
        // Called when feature is disabled
        // Note: We don't delete data, just stop accepting new transactions
    }
}
