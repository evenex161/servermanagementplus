package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class MineStacksCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("minestacks")
                .executes(MineStacksCommand::openMineStacksGUI)
        );
        
        // Register "casino" as an alias
        dispatcher.register(
            Commands.literal("casino")
                .executes(MineStacksCommand::openMineStacksGUI)
        );
    }
    
    private static int openMineStacksGUI(CommandContext<CommandSourceStack> context) {
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("MineStacks is currently disabled because Economy is disabled!"));
            return 0;
        }
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Open GUI using the existing provider
            com.servermanagement.gui.gambling.MineStacksMenuProvider.open(player);
            return 1;
        }
        return 0;
    }
}
