package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.servermanagement.features.economy.OverflowInventoryManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class OverflowCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("overflow")
                .executes(OverflowCommand::listOverflowItems)
                .then(Commands.literal("claim")
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(OverflowCommand::claimOverflowItem))
                    .executes(OverflowCommand::claimAllOverflowItems)
                )
        );
    }

    private static int listOverflowItems(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        OverflowInventoryManager overflow = OverflowInventoryManager.getInstance();
        List<ItemStack> items = overflow.getItems(player.getUUID());

        if (items.isEmpty()) {
            player.displayClientMessage(Component.literal("§7No overflow items to claim."), true);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6═══ §eOverflow Inventory §7(" + items.size() + " items) §6═══"));
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            player.sendSystemMessage(Component.literal("§7" + (i + 1) + ". §f" +
                stack.getCount() + "x " + stack.getDisplayName().getString()));
        }
        player.sendSystemMessage(Component.literal("§7Use §a/overflow claim <number> §7to claim a specific item"));
        player.sendSystemMessage(Component.literal("§7Use §a/overflow claim §7to claim all items"));
        return 1;
    }

    private static int claimOverflowItem(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        int index = IntegerArgumentType.getInteger(context, "index") - 1; // Convert 1-based to 0-based
        OverflowInventoryManager overflow = OverflowInventoryManager.getInstance();

        ItemStack claimed = overflow.claimItem(player.getUUID(), index);
        if (claimed.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cInvalid item number!"));
            return 0;
        }

        if (!com.servermanagement.features.economy.OverflowInventoryManager.safeAddToInventory(player, claimed)) {
            // Can't fit — put it back
            overflow.addItem(player.getUUID(), claimed);
            player.sendSystemMessage(Component.literal("§cYour inventory is full! Make room first."));
            return 0;
        }

        player.displayClientMessage(Component.literal("§a§l✓ §r§aClaimed: §f" + claimed.getCount() + "x " +
            claimed.getDisplayName().getString()), true);
        return 1;
    }

    private static int claimAllOverflowItems(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        OverflowInventoryManager overflow = OverflowInventoryManager.getInstance();
        int remaining = overflow.deliverItems(player);

        if (remaining > 0) {
            player.sendSystemMessage(Component.literal("§eDelivered some items. §c" + remaining +
                " item(s) §ecouldn't fit — make room and try again."));
        } else if (overflow.getItemCount(player.getUUID()) == 0) {
            player.displayClientMessage(Component.literal("§a§l✓ §r§aAll overflow items claimed!"), true);
        } else {
            player.displayClientMessage(Component.literal("§7No overflow items to claim."), true);
        }
        return 1;
    }
}
