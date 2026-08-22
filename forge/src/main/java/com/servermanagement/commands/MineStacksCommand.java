package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.Constants;

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
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Check master economy feature first (ConfigScreen toggle), then sub-feature flag
            if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(Constants.CHAT_PREFIX + " Economy feature is disabled.").withStyle(net.minecraft.ChatFormatting.RED));
                return 0;
            }
            if (!com.servermanagement.config.ModConfig.MINESTACKS_ENABLED.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(Constants.CHAT_PREFIX + " MineStacks is currently disabled.").withStyle(net.minecraft.ChatFormatting.RED));
                return 0;
            }
            // Open GUI using the existing provider
            com.servermanagement.gui.gambling.MineStacksMenuProvider.open(player);
            return 1;
        }
        return 0;
    }
}
