package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Overrides the vanilla /help command to include descriptions for all
 * Server Management commands. Admin commands are automatically filtered
 * by Brigadier's requires() check and only shown to players with OP level 2+.
 */
public class HelpCommandIntegration {

    private static final Map<String, String> COMMAND_DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, List<HelpEntry>> DETAILED_HELP = new LinkedHashMap<>();
    private static final Set<String> ADMIN_COMMANDS = Set.of(
        "servermanagement", "sm", "smconfig", "worldmanager", "wm",
        "playermanager", "pm", "spectate", "stopspectate", "viewinv",
        "netherportals", "endportals", "setlobby", "clearlobby",
        "slimehead", "smmetrics"
    );

    static {
        // === Root-level command descriptions (shown in /help) ===
        // Player commands
        COMMAND_DESCRIPTIONS.put("bank", "Economy - bank, payments, and daily tasks");
        COMMAND_DESCRIPTIONS.put("minebay", "Open the MineBay player marketplace");
        COMMAND_DESCRIPTIONS.put("minestacks", "Open MineStacks Casino");
        COMMAND_DESCRIPTIONS.put("casino", "Open MineStacks Casino");
        COMMAND_DESCRIPTIONS.put("teleportlobby", "Teleport to the server lobby");
        // Admin commands
        COMMAND_DESCRIPTIONS.put("servermanagement", "Open the admin Dashboard");
        COMMAND_DESCRIPTIONS.put("sm", "Open the admin Dashboard");
        COMMAND_DESCRIPTIONS.put("smconfig", "Manage mod settings");
        COMMAND_DESCRIPTIONS.put("worldmanager", "Open World Manager");
        COMMAND_DESCRIPTIONS.put("wm", "Open World Manager");
        COMMAND_DESCRIPTIONS.put("playermanager", "Open Player Manager");
        COMMAND_DESCRIPTIONS.put("pm", "Open Player Manager");
        COMMAND_DESCRIPTIONS.put("spectate", "Spectate a player in real-time");
        COMMAND_DESCRIPTIONS.put("stopspectate", "Stop spectating a player");
        COMMAND_DESCRIPTIONS.put("viewinv", "View a player's inventory");
        COMMAND_DESCRIPTIONS.put("netherportals", "Toggle Nether portals on/off");
        COMMAND_DESCRIPTIONS.put("endportals", "Toggle End portals on/off");
        COMMAND_DESCRIPTIONS.put("setlobby", "Set the lobby spawn point");
        COMMAND_DESCRIPTIONS.put("clearlobby", "Remove the lobby spawn point");
        COMMAND_DESCRIPTIONS.put("slimehead", "Give a Slime Head item");
        COMMAND_DESCRIPTIONS.put("smmetrics", "View server performance metrics");

        // === Detailed help (shown when /help <command> is used) ===

        // /bank subcommands
        List<HelpEntry> bankHelp = new ArrayList<>();
        bankHelp.add(new HelpEntry("/bank", "Open the Bank GUI", false));
        bankHelp.add(new HelpEntry("/bank balance", "View your bank balance", false));
        bankHelp.add(new HelpEntry("/bank pay <player> <amount>", "Send money to a player", false));
        bankHelp.add(new HelpEntry("/bank request <player> <amount>", "Request money from a player", false));
        bankHelp.add(new HelpEntry("/bank requests", "View pending money requests", false));
        bankHelp.add(new HelpEntry("/bank accept <id>", "Accept a money request", false));
        bankHelp.add(new HelpEntry("/bank deny <id>", "Deny a money request", false));
        bankHelp.add(new HelpEntry("/bank stats", "View your bank statistics", false));
        bankHelp.add(new HelpEntry("/bank dailies", "Open Daily Tasks GUI", false));
        bankHelp.add(new HelpEntry("/bank dailies claim <1-3>", "Claim a completed daily task", false));
        bankHelp.add(new HelpEntry("/bank dailies free", "Claim free daily reward", false));
        bankHelp.add(new HelpEntry("/bank admin set <player> <amount>", "Set a player's balance", true));
        bankHelp.add(new HelpEntry("/bank admin give <player> <amount>", "Give money to a player", true));
        bankHelp.add(new HelpEntry("/bank admin take <player> <amount>", "Take money from a player", true));
        DETAILED_HELP.put("bank", bankHelp);

        // /servermanagement subcommands
        List<HelpEntry> smHelp = new ArrayList<>();
        smHelp.add(new HelpEntry("/servermanagement", "Open the admin Dashboard", true));
        smHelp.add(new HelpEntry("/servermanagement dashboard", "Open the admin Dashboard", true));
        smHelp.add(new HelpEntry("/servermanagement resetdailies", "Force-reset all daily tasks", true));
        DETAILED_HELP.put("servermanagement", smHelp);
        DETAILED_HELP.put("sm", smHelp);

        // /smconfig subcommands
        List<HelpEntry> smconfigHelp = new ArrayList<>();
        smconfigHelp.add(new HelpEntry("/smconfig", "Open mod settings GUI", true));
        smconfigHelp.add(new HelpEntry("/smconfig toggle <feature>", "Toggle a feature on/off", true));
        smconfigHelp.add(new HelpEntry("/smconfig info", "Show config version info", true));
        smconfigHelp.add(new HelpEntry("/smconfig validate", "Validate config integrity", true));
        smconfigHelp.add(new HelpEntry("/smconfig reset confirm", "Reset config to defaults", true));
        smconfigHelp.add(new HelpEntry("/smconfig migrate", "Perform config migration", true));
        smconfigHelp.add(new HelpEntry("/smconfig backup", "Clean up old config backups", true));
        DETAILED_HELP.put("smconfig", smconfigHelp);

        // /smmetrics subcommands
        List<HelpEntry> metricsHelp = new ArrayList<>();
        metricsHelp.add(new HelpEntry("/smmetrics", "View performance metrics", true));
        metricsHelp.add(new HelpEntry("/smmetrics reset", "Reset performance metrics", true));
        DETAILED_HELP.put("smmetrics", metricsHelp);

        // /slimehead subcommands
        DETAILED_HELP.put("slimehead", List.of(
            new HelpEntry("/slimehead", "Give yourself a Slime Head", true),
            new HelpEntry("/slimehead <player>", "Give a Slime Head to a player", true)
        ));

        // Simple commands (single-entry detailed help)
        DETAILED_HELP.put("minebay", List.of(new HelpEntry("/minebay", "Open the MineBay player marketplace", false)));
        DETAILED_HELP.put("minestacks", List.of(new HelpEntry("/minestacks", "Open MineStacks Casino", false)));
        DETAILED_HELP.put("casino", List.of(new HelpEntry("/casino", "Open MineStacks Casino", false)));
        DETAILED_HELP.put("teleportlobby", List.of(new HelpEntry("/teleportlobby", "Teleport to the server lobby", false)));
        DETAILED_HELP.put("worldmanager", List.of(new HelpEntry("/worldmanager", "Open World Manager", true)));
        DETAILED_HELP.put("wm", List.of(new HelpEntry("/wm", "Open World Manager", true)));
        DETAILED_HELP.put("playermanager", List.of(new HelpEntry("/playermanager", "Open Player Manager", true)));
        DETAILED_HELP.put("pm", List.of(new HelpEntry("/pm", "Open Player Manager", true)));
        DETAILED_HELP.put("spectate", List.of(new HelpEntry("/spectate <player>", "Spectate a player in real-time", true)));
        DETAILED_HELP.put("stopspectate", List.of(new HelpEntry("/stopspectate", "Stop spectating a player", true)));
        DETAILED_HELP.put("viewinv", List.of(new HelpEntry("/viewinv <player>", "View a player's inventory", true)));
        DETAILED_HELP.put("netherportals", List.of(new HelpEntry("/netherportals <true|false>", "Toggle Nether portals on/off", true)));
        DETAILED_HELP.put("endportals", List.of(new HelpEntry("/endportals <true|false>", "Toggle End portals on/off", true)));
        DETAILED_HELP.put("setlobby", List.of(new HelpEntry("/setlobby", "Set the lobby spawn point", true)));
        DETAILED_HELP.put("clearlobby", List.of(new HelpEntry("/clearlobby", "Remove the lobby spawn point", true)));
    }

    /**
     * Register the /help override. Must be called during RegisterCommandsEvent
     * AFTER all other commands have been registered. Brigadier will merge the
     * new executes() handler onto the existing /help node.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("help")
            .executes(ctx -> showAllCommands(ctx, dispatcher))
            .then(Commands.argument("command", StringArgumentType.greedyString())
                .executes(ctx -> showCommandHelp(ctx, dispatcher))
            )
        );
    }

    /**
     * /help — List all commands the player has access to, sorted alphabetically.
     * SM commands show descriptions; admin commands are tagged [Admin].
     * Vanilla/other mod commands appear without description.
     */
    private static int showAllCommands(CommandContext<CommandSourceStack> ctx, CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandSourceStack source = ctx.getSource();

        List<CommandNode<CommandSourceStack>> nodes = new ArrayList<>(dispatcher.getRoot().getChildren());
        nodes.sort(Comparator.comparing(CommandNode::getName));

        int count = 0;
        for (CommandNode<CommandSourceStack> node : nodes) {
            if (!node.canUse(source)) continue;

            String name = node.getName();
            String desc = COMMAND_DESCRIPTIONS.get(name);
            boolean isAdmin = ADMIN_COMMANDS.contains(name);

            final String cmdName = name;
            if (desc != null) {
                final String cmdDesc = desc;
                if (isAdmin) {
                    source.sendSuccess(() -> Component.literal(
                        "\u00A7e/" + cmdName + " \u00A77- " + cmdDesc + " \u00A7c[Admin]"), false);
                } else {
                    source.sendSuccess(() -> Component.literal(
                        "\u00A7e/" + cmdName + " \u00A77- " + cmdDesc), false);
                }
            } else {
                source.sendSuccess(() -> Component.literal("/" + cmdName), false);
            }
            count++;
        }

        source.sendSuccess(() -> Component.literal(
            "\u00A77Use \u00A7e/help <command>\u00A77 for more info on a command"), false);
        return count;
    }

    /**
     * /help <command> — Show detailed subcommand help for SM commands,
     * or fall back to vanilla Brigadier usage for other commands.
     * Admin subcommands are only shown when the player has OP level 2+.
     */
    private static int showCommandHelp(CommandContext<CommandSourceStack> ctx, CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandName = StringArgumentType.getString(ctx, "command").trim();
        CommandSourceStack source = ctx.getSource();

        // Exact match first
        List<HelpEntry> entries = DETAILED_HELP.get(commandName);
        if (entries != null) {
            return showDetailedHelp(source, commandName, entries);
        }

        // Try root command match (e.g. "bank admin" -> "bank")
        String rootCmd = commandName.split(" ")[0];
        entries = DETAILED_HELP.get(rootCmd);
        if (entries != null) {
            return showDetailedHelp(source, rootCmd, entries);
        }

        // Not an SM command — fall back to vanilla Brigadier usage
        return showVanillaHelp(source, commandName, dispatcher);
    }

    /**
     * Show our custom detailed help for a specific SM command.
     * Admin-only entries are hidden from non-admins. If the command is
     * entirely admin-only and the player has no permission, show an error.
     */
    private static int showDetailedHelp(CommandSourceStack source, String commandName, List<HelpEntry> entries) {
        boolean isAdmin = source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);

        source.sendSuccess(() -> Component.literal(
            "\u00A76=== /" + commandName + " Help ==="), false);

        int count = 0;
        boolean shownAdminHeader = false;

        for (HelpEntry entry : entries) {
            if (entry.adminOnly && !isAdmin) continue;

            if (entry.adminOnly && !shownAdminHeader) {
                source.sendSuccess(() -> Component.literal(
                    "\u00A7c--- Admin Commands ---"), false);
                shownAdminHeader = true;
            }

            final String usage = entry.usage;
            final String desc = entry.description;
            source.sendSuccess(() -> Component.literal(
                "\u00A7e" + usage + " \u00A77- " + desc), false);
            count++;
        }

        if (count == 0) {
            source.sendFailure(Component.literal("You don't have permission to view this command's help"));
            return 0;
        }

        return count;
    }

    /**
     * Fallback: replicate vanilla /help <command> behavior using
     * Brigadier's getSmartUsage for non-SM commands.
     */
    private static int showVanillaHelp(CommandSourceStack source, String commandName, CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            ParseResults<CommandSourceStack> parseResults = dispatcher.parse(commandName, source);
            List<ParsedCommandNode<CommandSourceStack>> nodes = parseResults.getContext().getNodes();

            if (nodes.isEmpty()) {
                source.sendFailure(Component.literal("Unknown command: " + commandName));
                return 0;
            }

            CommandNode<CommandSourceStack> lastNode = nodes.get(nodes.size() - 1).getNode();
            Map<CommandNode<CommandSourceStack>, String> usageMap = dispatcher.getSmartUsage(lastNode, source);

            if (usageMap.isEmpty()) {
                source.sendSuccess(() -> Component.literal("/" + commandName), false);
                return 1;
            }

            for (String usage : usageMap.values()) {
                source.sendSuccess(() -> Component.literal("/" + commandName + " " + usage), false);
            }

            return usageMap.size();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Unknown command: " + commandName));
            return 0;
        }
    }

    /**
     * Represents a single help entry for a command or subcommand.
     */
    private static final class HelpEntry {
        final String usage;
        final String description;
        final boolean adminOnly;

        HelpEntry(String usage, String description, boolean adminOnly) {
            this.usage = usage;
            this.description = description;
            this.adminOnly = adminOnly;
        }
    }
}
