package com.servermanagement.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.worldmanager.WorldManager;
import com.servermanagement.features.playermanager.PlayerManagerSingleton;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
public class ModCommands {

    public static void onRegisterCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        ServerManagementMod.LOGGER.debug("Registering mod commands");
        
        // Main Dashboard GUI commands
        dispatcher.register(Commands.literal("servermanagement")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    // Open main dashboard
                    player.openMenu(new com.servermanagement.gui.provider.DashboardMenuProvider());
                }
                return 1;
            })
            .then(Commands.literal("dashboard")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        // Open main dashboard
                        player.openMenu(new com.servermanagement.gui.provider.DashboardMenuProvider());
                    }
                    return 1;
                })
            )
            .then(Commands.literal("resetdailies")
                .executes(context -> {
                    // Force reset all player dailies
                    com.servermanagement.features.economy.EconomyManager economyManager = 
                        com.servermanagement.features.economy.EconomyManager.getInstance(context.getSource().getServer());
                    
                    if (economyManager != null) {
                        int count = economyManager.getDailyTasksManager().forceResetAllDailies();
                        economyManager.save();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("Reset daily tasks for " + count + " players"), true);
                    } else {
                        context.getSource().sendFailure(Component.literal("Economy system not initialized"));
                    }
                    return 1;
                })
            )
        );
        
        dispatcher.register(Commands.literal("sm")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    // Open main dashboard
                    player.openMenu(new com.servermanagement.gui.provider.DashboardMenuProvider());
                }
                return 1;
            })
        );
        
        // ServerManagement Settings command
        dispatcher.register(Commands.literal("smconfig")
            .requires(source -> source.hasPermission(2))
            // Default: open GUI
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    // Sync feature states before opening
                    syncFeatureStates(player);
                    // Open config GUI (full 6-toggle ConfigScreen)
                    player.openMenu(new com.servermanagement.gui.ConfigMenuProvider());
                }
                return 1;
            })
            // Toggle features from command line
            .then(Commands.literal("toggle")
                .then(Commands.argument("feature", StringArgumentType.string())
                    .executes(context -> {
                        String featureId = StringArgumentType.getString(context, "feature");
                        boolean currentState = com.servermanagement.features.FeatureManager.isFeatureEnabled(featureId);
                        com.servermanagement.features.FeatureManager.toggleFeature(featureId, !currentState);
                        context.getSource().sendSuccess(() -> Component.literal("§aToggled " + featureId + " to: " + (!currentState ? "ON" : "OFF")), true);
                        return 1;
                    })
                )
            )
            // Config info
            .then(Commands.literal("info")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    int currentVersion = com.servermanagement.config.ModConfig.CONFIG_VERSION.get();
                    int targetVersion = com.servermanagement.config.ModConfig.CURRENT_CONFIG_VERSION;
                    
                    context.getSource().sendSuccess(() -> Component.literal("§e=== Config Information ==="), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Current Version: §f" + currentVersion), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Expected Version: §f" + targetVersion), false);
                    
                    if (currentVersion == targetVersion) {
                        context.getSource().sendSuccess(() -> Component.literal("§aConfig is up to date!"), false);
                    } else if (currentVersion < targetVersion) {
                        context.getSource().sendSuccess(() -> Component.literal("§eMigration needed: v" + currentVersion + " -> v" + targetVersion), false);
                        context.getSource().sendSuccess(() -> Component.literal("§7Run '/smconfig migrate' to update"), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal("§cConfig is from a newer mod version!"), false);
                    }
                    
                    return 1;
                })
            )
            // Config migration
            .then(Commands.literal("migrate")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    if (!com.servermanagement.config.ConfigMigration.needsMigration()) {
                        context.getSource().sendSuccess(() -> Component.literal("§aNo migration needed - config is up to date!"), false);
                        return 1;
                    }
                    
                    context.getSource().sendSuccess(() -> Component.literal("§eStarting config migration..."), false);
                    boolean success = com.servermanagement.config.ConfigMigration.checkAndMigrate();
                    
                    if (success) {
                        context.getSource().sendSuccess(() -> Component.literal("§aMigration completed successfully!"), true);
                        context.getSource().sendSuccess(() -> Component.literal("§eA backup of your old config was created."), false);
                    } else {
                        context.getSource().sendFailure(Component.literal("§cMigration failed! Check server logs for details."));
                    }
                    
                    return success ? 1 : 0;
                })
            )
            // Config validation
            .then(Commands.literal("validate")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§eValidating configuration..."), false);
                    boolean valid = com.servermanagement.config.ConfigValidator.validateAndRepair();
                    if (valid) {
                        context.getSource().sendSuccess(() -> Component.literal("§aConfiguration is valid!"), true);
                    } else {
                        context.getSource().sendFailure(Component.literal("§cConfiguration validation failed! Check server logs for details."));
                    }
                    return valid ? 1 : 0;
                })
            )
            // Config reset
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§c§lWARNING: This will reset ALL configuration to defaults!"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§eRun '/smconfig reset confirm' to proceed."), false);
                    return 1;
                })
                .then(Commands.literal("confirm")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§eResetting configuration to defaults..."), false);
                        boolean success = com.servermanagement.config.ConfigValidator.forceReset();
                        if (success) {
                            context.getSource().sendSuccess(() -> Component.literal("§aConfiguration reset successfully! A backup was created."), true);
                            context.getSource().sendSuccess(() -> Component.literal("§eRestart the server for changes to take full effect."), false);
                        } else {
                            context.getSource().sendFailure(Component.literal("§cFailed to reset configuration! Check server logs."));
                        }
                        return success ? 1 : 0;
                    })
                )
            )
            // Backup cleanup
            .then(Commands.literal("backup")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§eCleaning up old config backups..."), false);
                    com.servermanagement.config.ConfigValidator.cleanupOldBackups();
                    context.getSource().sendSuccess(() -> Component.literal("§aBackup cleanup complete!"), true);
                    return 1;
                })
            )
        );
        
        // WorldManager command (/worldmanager or /wm)
        dispatcher.register(Commands.literal("worldmanager")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    player.openMenu(new com.servermanagement.gui.WorldListMenuProvider());
                }
                return 1;
            })
        );
        
        dispatcher.register(Commands.literal("wm")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    player.openMenu(new com.servermanagement.gui.WorldListMenuProvider());
                }
                return 1;
            })
        );
        
        // PlayerManager command (/playermanager or /pm)
        dispatcher.register(Commands.literal("playermanager")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    player.openMenu(new com.servermanagement.gui.PlayerManagerMenuProvider());
                }
                return 1;
            })
        );
        
        dispatcher.register(Commands.literal("pm")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    player.openMenu(new com.servermanagement.gui.PlayerManagerMenuProvider());
                }
                return 1;
            })
        );
        
        // Spectate command — remove vanilla's /spectate first to prevent collision
        // Vanilla's /spectate requires the player to already be in spectator mode,
        // which conflicts with our mod's spectate that switches mode automatically
        var rootNode = dispatcher.getRoot();
        if (rootNode.getChild("spectate") != null) {
            try {
                // Brigadier doesn't expose a removeChild method, so we access the internal maps
                var childrenField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("children");
                childrenField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var children = (java.util.Map<String, ?>) childrenField.get(rootNode);
                children.remove("spectate");
                
                var literalsField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("literals");
                literalsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var literals = (java.util.Map<String, ?>) literalsField.get(rootNode);
                literals.remove("spectate");
            } catch (Exception e) {
                ServerManagementMod.LOGGER.warn("Failed to remove vanilla /spectate command: {}", e.getMessage());
            }
        }
        dispatcher.register(Commands.literal("spectate")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", StringArgumentType.string())
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        String targetName = StringArgumentType.getString(context, "player");
                        // PlayerManagerSingleton.spectatePlayer sends its own success/failure
                        // chat message; sending one here would duplicate the success or
                        // contradict an error (self-spectate / player-not-found).
                        PlayerManagerSingleton.spectatePlayer(player, targetName);
                    }
                    return 1;
                })
            )
        );
        
        // Stop spectate command
        dispatcher.register(Commands.literal("stopspectate")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    if (PlayerManagerSingleton.isSpectating(player)) {
                        PlayerManagerSingleton.stopSpectate(player);
                        context.getSource().sendSuccess(() -> Component.literal("Stopped spectating"), false);
                    } else {
                        context.getSource().sendFailure(Component.literal("You are not spectating anyone"));
                    }
                }
                return 1;
            })
        );
        
        // View inventory command
        dispatcher.register(Commands.literal("viewinv")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", StringArgumentType.string())
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        String targetName = StringArgumentType.getString(context, "player");
                        PlayerManagerSingleton.viewInventory(player, targetName);
                        context.getSource().sendSuccess(() -> Component.literal("Viewing inventory of " + targetName), false);
                    }
                    return 1;
                })
            )
        );
        
        // Nether portals command
        dispatcher.register(Commands.literal("netherportals")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> {
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    // Get the dimension the command sender is in
                    String dimensionId = "minecraft:overworld";
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        dimensionId = player.level().dimension().location().toString();
                    }
                    // Check timer lock
                    if (WorldManager.getInstance().getData().hasActiveTimer(dimensionId)) {
                        context.getSource().sendFailure(Component.literal("Cannot change portal state while a timer is active for this dimension!"));
                        return 0;
                    }
                    WorldManager.getInstance().toggleNetherPortals(dimensionId, enabled);
                    // Broadcast to all players
                    if (context.getSource().getServer() != null) {
                        WorldManager.broadcastPortalChange(context.getSource().getServer(), dimensionId, "nether", enabled);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Nether portals " + (enabled ? "enabled" : "disabled")), true);
                    return 1;
                })
            )
        );
        
        // End portals command
        dispatcher.register(Commands.literal("endportals")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> {
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    // Get the dimension the command sender is in
                    String dimensionId = "minecraft:overworld";
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        dimensionId = player.level().dimension().location().toString();
                    }
                    // Check timer lock
                    if (WorldManager.getInstance().getData().hasActiveTimer(dimensionId)) {
                        context.getSource().sendFailure(Component.literal("Cannot change portal state while a timer is active for this dimension!"));
                        return 0;
                    }
                    WorldManager.getInstance().toggleEndPortals(dimensionId, enabled);
                    // Broadcast to all players
                    if (context.getSource().getServer() != null) {
                        WorldManager.broadcastPortalChange(context.getSource().getServer(), dimensionId, "end", enabled);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("End portals " + (enabled ? "enabled" : "disabled")), true);
                    return 1;
                })
            )
        );
        
        // Set lobby command
        dispatcher.register(Commands.literal("setlobby")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    WorldManager.setLobbySpawn(player.getX(), player.getY(), player.getZ(),
                        player.level().dimension().location().toString(), player.getYRot(), player.getXRot());
                    context.getSource().sendSuccess(() -> Component.literal("Lobby spawn set to current location"), true);
                }
                return 1;
            })
        );
        
        // Clear lobby command
        dispatcher.register(Commands.literal("clearlobby")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                WorldManager.getInstance().getData().clearLobbySpawn();
                WorldManager.getInstance().save();
                context.getSource().sendSuccess(() -> Component.literal("Lobby spawn cleared"), true);
                return 1;
            })
        );
        
        // Teleport to lobby command
        dispatcher.register(Commands.literal("teleportlobby")
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    var lobby = WorldManager.getInstance().getData().getLobbySpawn();
                    if (lobby != null) {
                        // Teleport to the lobby's stored coordinates, not the dimension's world spawn
                        ResourceLocation dimLoc = ResourceLocation.tryParse(lobby.dimension);
                        if (dimLoc != null) {
                            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
                            ServerLevel targetLevel = player.getServer().getLevel(dimKey);
                            if (targetLevel != null) {
                                player.teleportTo(targetLevel, lobby.x, lobby.y, lobby.z, java.util.Set.of(), lobby.yaw, lobby.pitch, true);
                                context.getSource().sendSuccess(() -> Component.literal("Teleported to lobby"), false);
                            } else {
                                context.getSource().sendFailure(Component.literal("Lobby dimension not found"));
                            }
                        } else {
                            context.getSource().sendFailure(Component.literal("Invalid lobby dimension"));
                        }
                    } else {
                        context.getSource().sendFailure(Component.literal("No lobby spawn set"));
                    }
                }
                return 1;
            })
        );
        
        // SlimeHead command
        dispatcher.register(Commands.literal("slimehead")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    com.servermanagement.features.slimehead.SlimeHeadManager.giveSlimeHead(player);
                }
                return 1;
            })
            .then(Commands.argument("player", StringArgumentType.string())
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, "player");
                    ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                    if (targetPlayer != null) {
                        com.servermanagement.features.slimehead.SlimeHeadManager.giveSlimeHead(targetPlayer);
                        context.getSource().sendSuccess(() -> Component.literal("Gave slime head to " + targetName), true);
                    } else {
                        context.getSource().sendFailure(Component.literal("Player not found: " + targetName));
                    }
                    return 1;
                })
            )
        );
        
        // Bank command for Economy feature
        dispatcher.register(Commands.literal("bank")
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    // Check if Economy feature is enabled
                    if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                        player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                        return 0;
                    }
                    
                    // Sync bank account data before opening
                    var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
                    var account = economyManager.getOrCreateAccount(player.getUUID());
                    
                    com.servermanagement.network.ModNetworking.sendToPlayer(
                        new com.servermanagement.network.packet.SyncBankAccountPacket(
                            account.getBalance(),
                            account.getTransactions()
                        ),
                        player
                    );
                    
                    // Open bank GUI
                    player.openMenu(new com.servermanagement.gui.economy.BankMenuProvider());
                }
                return 1;
            })
            .then(Commands.literal("balance")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                            player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                            return 0;
                        }
                        
                        // Sync bank account data before opening
                        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
                        var account = economyManager.getOrCreateAccount(player.getUUID());
                        
                        com.servermanagement.network.ModNetworking.sendToPlayer(
                            new com.servermanagement.network.packet.SyncBankAccountPacket(
                                account.getBalance(),
                                account.getTransactions()
                            ),
                            player
                        );
                        
                        // Open Bank GUI to show balance
                        player.openMenu(new com.servermanagement.gui.economy.BankMenuProvider());
                    }
                    return 1;
                })
            )
            .then(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer sender) {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    sender.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                    return 0;
                                }
                                
                                String targetName = StringArgumentType.getString(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                
                                ServerPlayer target = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayerByName(targetName);
                                
                                if (target == null) {
                                    sender.sendSystemMessage(Component.literal("§cPlayer not found"));
                                    return 0;
                                }
                                
                                if (target.getUUID().equals(sender.getUUID())) {
                                    sender.sendSystemMessage(Component.literal("§cYou cannot pay yourself"));
                                    return 0;
                                }
                                
                                boolean success = com.servermanagement.features.economy.EconomyManager
                                    .getInstance()
                                    .transfer(sender.getUUID(), target.getUUID(), amount);
                                
                                if (success) {
                                    // Send payment notifications
                                    com.servermanagement.features.economy.notifications.NotificationManager
                                        .sendPaymentNotification(sender, target.getName().getString(), amount);
                                    com.servermanagement.features.economy.notifications.NotificationManager
                                        .sendReceivedPaymentNotification(target, sender.getName().getString(), amount);
                                } else {
                                    sender.sendSystemMessage(Component.literal("§cInsufficient funds"));
                                }
                            }
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("admin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    context.getSource().sendFailure(Component.literal("Economy feature is disabled"));
                                    return 0;
                                }
                                
                                String targetName = StringArgumentType.getString(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                
                                ServerPlayer target = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayerByName(targetName);
                                
                                if (target == null) {
                                    context.getSource().sendFailure(Component.literal("Player not found"));
                                    return 0;
                                }
                                
                                com.servermanagement.features.economy.EconomyManager
                                    .getInstance()
                                    .setBalance(target.getUUID(), amount);
                                
                                context.getSource().sendSuccess(
                                    () -> Component.literal("Set " + targetName + "'s balance to $" + amount),
                                    true
                                );
                                
                                target.sendSystemMessage(Component.literal(
                                    "§eYour balance has been set to §a$" + amount
                                ));
                                
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("give")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    context.getSource().sendFailure(Component.literal("Economy feature is disabled"));
                                    return 0;
                                }
                                
                                String targetName = StringArgumentType.getString(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                
                                ServerPlayer target = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayerByName(targetName);
                                
                                if (target == null) {
                                    context.getSource().sendFailure(Component.literal("Player not found"));
                                    return 0;
                                }
                                
                                com.servermanagement.features.economy.EconomyManager
                                    .getInstance()
                                    .deposit(
                                        target.getUUID(),
                                        amount,
                                        com.servermanagement.features.economy.TransactionType.ADMIN_GIVE,
                                        "Admin gift"
                                    );
                                
                                context.getSource().sendSuccess(
                                    () -> Component.literal("Gave $" + amount + " to " + targetName),
                                    true
                                );
                                
                                target.sendSystemMessage(Component.literal(
                                    "§aYou received §e$" + amount + "§a from an admin"
                                ));
                                
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("take")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    context.getSource().sendFailure(Component.literal("Economy feature is disabled"));
                                    return 0;
                                }
                                
                                String targetName = StringArgumentType.getString(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                
                                ServerPlayer target = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayerByName(targetName);
                                
                                if (target == null) {
                                    context.getSource().sendFailure(Component.literal("Player not found"));
                                    return 0;
                                }
                                
                                boolean success = com.servermanagement.features.economy.EconomyManager
                                    .getInstance()
                                    .withdraw(
                                        target.getUUID(),
                                        amount,
                                        com.servermanagement.features.economy.TransactionType.ADMIN_TAKE,
                                        "Admin deduction"
                                    );
                                
                                if (success) {
                                    context.getSource().sendSuccess(
                                        () -> Component.literal("Took $" + amount + " from " + targetName),
                                        true
                                    );
                                    
                                    target.sendSystemMessage(Component.literal(
                                        "§c$" + amount + " was deducted from your account"
                                    ));
                                } else {
                                    context.getSource().sendFailure(Component.literal("Player has insufficient funds"));
                                }
                                
                                return 1;
                            })
                        )
                    )
                )
            )
            .then(Commands.literal("stats")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                            player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                            return 0;
                        }
                        
                        var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                        double balance = manager.getBalance(player.getUUID());
                        int achievementCount = manager.getAchievementTracker()
                            .getRewardedCount(player.getUUID());
                        
                        player.sendSystemMessage(Component.literal("§6=== Bank Statistics ==="));
                        player.sendSystemMessage(Component.literal(
                            "§aBalance: §e$" + String.format("%.2f", balance)
                        ));
                        player.sendSystemMessage(Component.literal(
                            "§aRewarded Achievements: §e" + achievementCount
                        ));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("dailies")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                            player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                            return 0;
                        }
                        
                        // Sync daily tasks data before opening
                        var economyManager = com.servermanagement.features.economy.EconomyManager.getInstance();
                        var dailyTasksManager = economyManager.getDailyTasksManager();
                        var playerTasks = dailyTasksManager.getOrCreatePlayerTasks(player.getUUID());
                        var templateManager = economyManager.getTemplateManager();
                        
                        // Use admin-configurable free reward amount from template manager
                        int freeRewardAmount = templateManager != null
                            ? (int) templateManager.getFreeRewardAmount()
                            : playerTasks.getFreeRewardAmount();
                        
                        // Calculate reset time
                        long resetTime = System.currentTimeMillis() + playerTasks.getTimeUntilTaskRefresh();
                        
                        com.servermanagement.network.ModNetworking.sendToPlayer(
                            new com.servermanagement.network.packet.SyncDailyTasksPacket(
                                playerTasks.getTasks(),
                                resetTime,
                                playerTasks.isFreeRewardAvailable(),
                                freeRewardAmount,
                                playerTasks.getTimeUntilFreeReward()
                            ),
                            player
                        );
                        
                        // Open Daily Tasks GUI
                        player.openMenu(new com.servermanagement.gui.economy.DailyTasksMenuProvider());
                    }
                    return 1;
                })
                .then(Commands.literal("claim")
                    .then(Commands.argument("taskNumber", IntegerArgumentType.integer(1, 3))
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                    return 0;
                                }
                                
                                int taskNum = IntegerArgumentType.getInteger(context, "taskNumber");
                                int taskIndex = taskNum - 1;
                                
                                var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                                int reward = manager.getDailyTasksManager()
                                    .claimTaskReward(player.getUUID(), taskIndex);
                                
                                if (reward > 0) {
                                    manager.deposit(
                                        player.getUUID(),
                                        reward,
                                        com.servermanagement.features.economy.TransactionType.ADMIN_GIVE,
                                        "Daily Task Reward"
                                    );
                                    
                                    // Send reward notification
                                    com.servermanagement.features.economy.notifications.NotificationManager
                                        .sendRewardClaimedNotification(player, reward);
                                } else {
                                    player.sendSystemMessage(Component.literal(
                                        "§cTask not completed or already claimed"
                                    ));
                                }
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("free")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                return 0;
                            }
                            
                            var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                            int reward = manager.getDailyTasksManager()
                                .claimFreeReward(player.getUUID());
                            
                            if (reward > 0) {
                                manager.deposit(
                                    player.getUUID(),
                                    reward,
                                    com.servermanagement.features.economy.TransactionType.ADMIN_GIVE,
                                    "Free Daily Reward"
                                );
                                
                                // Send reward notification
                                com.servermanagement.features.economy.notifications.NotificationManager
                                    .sendRewardClaimedNotification(player, reward);
                            } else {
                                var playerTasks = manager.getDailyTasksManager()
                                    .getOrCreatePlayerTasks(player.getUUID());
                                long timeRemaining = playerTasks.getTimeUntilFreeReward();
                                player.sendSystemMessage(Component.literal(
                                    "§cFree reward not available. Next reward in: " +
                                    com.servermanagement.features.economy.PlayerDailyTasks.formatTimeRemaining(timeRemaining)
                                ));
                            }
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("request")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer requester) {
                                if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                    requester.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                    return 0;
                                }
                                
                                String targetName = StringArgumentType.getString(context, "player");
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                
                                ServerPlayer target = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayerByName(targetName);
                                
                                if (target == null) {
                                    requester.sendSystemMessage(Component.literal("§cPlayer not found or not online"));
                                    return 0;
                                }
                                
                                if (target.getUUID().equals(requester.getUUID())) {
                                    requester.sendSystemMessage(Component.literal("§cYou cannot request money from yourself"));
                                    return 0;
                                }
                                
                                var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                                var request = manager.getRequestManager()
                                    .createRequest(requester.getUUID(), target.getUUID(), amount, "Money request");
                                
                                if (request != null) {
                                    manager.save();
                                    
                                    requester.sendSystemMessage(Component.literal(
                                        "§aRequest sent to §e" + targetName + "§a for §e$" + amount
                                    ));
                                    
                                    target.sendSystemMessage(Component.literal(
                                        "§e" + requester.getName().getString() + "§a is requesting §e$" + amount
                                    ));
                                    target.sendSystemMessage(Component.literal(
                                        "§7Use §e/bank requests§7 to view and respond"
                                    ));
                                } else {
                                    requester.sendSystemMessage(Component.literal(
                                        "§cYou have too many pending requests. Cancel some first."
                                    ));
                                }
                            }
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("requests")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                            player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                            return 0;
                        }
                        
                        var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                        var incoming = manager.getRequestManager()
                            .getPendingIncomingRequests(player.getUUID());
                        var outgoing = manager.getRequestManager()
                            .getPendingRequestsByRequester(player.getUUID());
                        
                        player.sendSystemMessage(Component.literal("§6=== Money Requests ==="));
                        
                        if (!incoming.isEmpty()) {
                            player.sendSystemMessage(Component.literal("§eIncoming Requests:"));
                            for (var req : incoming) {
                                ServerPlayer requesterPlayer = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayer(req.getRequesterUUID());
                                String requesterName = requesterPlayer != null ? 
                                    requesterPlayer.getName().getString() : "Unknown";
                                
                                player.sendSystemMessage(Component.literal(
                                    "§a• §f" + requesterName + " §7requests §e$" + String.format("%.0f", req.getAmount()) +
                                    " §7(" + req.getFormattedAge() + ")"
                                ));
                                player.sendSystemMessage(Component.literal(
                                    "  §7ID: §e" + req.getRequestId().toString().substring(0, 8) + 
                                    " §7- Use §e/bank accept <id>§7 or §e/bank deny <id>"
                                ));
                            }
                        }
                        
                        if (!outgoing.isEmpty()) {
                            player.sendSystemMessage(Component.literal("§eOutgoing Requests:"));
                            for (var req : outgoing) {
                                ServerPlayer targetPlayer = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayer(req.getTargetUUID());
                                String targetName = targetPlayer != null ? 
                                    targetPlayer.getName().getString() : "Unknown";
                                
                                player.sendSystemMessage(Component.literal(
                                    "§a• §7To §f" + targetName + "§7: §e$" + String.format("%.0f", req.getAmount()) +
                                    " §7(" + req.getFormattedAge() + ")"
                                ));
                            }
                        }
                        
                        if (incoming.isEmpty() && outgoing.isEmpty()) {
                            player.sendSystemMessage(Component.literal("§7No pending requests"));
                        }
                    }
                    return 1;
                })
            )
            .then(Commands.literal("accept")
                .then(Commands.argument("requestId", StringArgumentType.word())
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                return 0;
                            }
                            
                            String idStr = StringArgumentType.getString(context, "requestId");
                            var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                            
                            // Find request by partial ID
                            var request = manager.getRequestManager()
                                .getPendingIncomingRequests(player.getUUID())
                                .stream()
                                .filter(r -> r.getRequestId().toString().startsWith(idStr))
                                .findFirst()
                                .orElse(null);
                            
                            if (request == null) {
                                player.sendSystemMessage(Component.literal("§cRequest not found"));
                                return 0;
                            }
                            
                            // Transfer the money
                            boolean success = manager.transfer(
                                player.getUUID(),
                                request.getRequesterUUID(),
                                request.getAmount()
                            );
                            
                            if (success) {
                                manager.getRequestManager().acceptRequest(request.getRequestId(), player.getUUID());
                                manager.save();
                                
                                player.sendSystemMessage(Component.literal(
                                    "§aAccepted request and sent §e$" + String.format("%.0f", request.getAmount())
                                ));
                                
                                // Notify requester if online
                                ServerPlayer requester = context.getSource().getServer()
                                    .getPlayerList()
                                    .getPlayer(request.getRequesterUUID());
                                if (requester != null) {
                                    requester.sendSystemMessage(Component.literal(
                                        "§a" + player.getName().getString() + " accepted your request and sent §e$" + 
                                        String.format("%.0f", request.getAmount())
                                    ));
                                }
                            } else {
                                player.sendSystemMessage(Component.literal("§cInsufficient funds"));
                            }
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("deny")
                .then(Commands.argument("requestId", StringArgumentType.word())
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) {
                                player.sendSystemMessage(Component.literal("§cEconomy feature is disabled"));
                                return 0;
                            }
                            
                            String idStr = StringArgumentType.getString(context, "requestId");
                            var manager = com.servermanagement.features.economy.EconomyManager.getInstance();
                            
                            // Find request by partial ID
                            var request = manager.getRequestManager()
                                .getPendingIncomingRequests(player.getUUID())
                                .stream()
                                .filter(r -> r.getRequestId().toString().startsWith(idStr))
                                .findFirst()
                                .orElse(null);
                            
                            if (request == null) {
                                player.sendSystemMessage(Component.literal("§cRequest not found"));
                                return 0;
                            }
                            
                            manager.getRequestManager().denyRequest(request.getRequestId(), player.getUUID());
                            manager.save();
                            
                            player.sendSystemMessage(Component.literal("§cDenied money request"));
                            
                            // Notify requester if online
                            ServerPlayer requester = context.getSource().getServer()
                                .getPlayerList()
                                .getPlayer(request.getRequesterUUID());
                            if (requester != null) {
                                requester.sendSystemMessage(Component.literal(
                                    "§c" + player.getName().getString() + " denied your request for §e$" + 
                                    String.format("%.0f", request.getAmount())
                                ));
                            }
                        }
                        return 1;
                    })
                )
            )
        );
        
        // Server Performance command (/smperformance or /smperf)
        registerPerformanceCommand(dispatcher, "smperformance");
        registerPerformanceCommand(dispatcher, "smperf");
        
        // Performance metrics command
        dispatcher.register(Commands.literal("smmetrics")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                String report = com.servermanagement.util.PerformanceMetrics.getInstance().getReport();
                context.getSource().sendSuccess(() -> Component.literal(report), false);
                return 1;
            })
            .then(Commands.literal("reset")
                .executes(context -> {
                    com.servermanagement.util.PerformanceMetrics.getInstance().reset();
                    context.getSource().sendSuccess(() -> Component.literal("§aPerformance metrics reset"), false);
                    return 1;
                })
            )
        );
        
        // Register /help integration with SM command descriptions
        HelpCommandIntegration.register(dispatcher);
        
        ServerManagementMod.LOGGER.debug("Registered all mod commands");
    }
    
    private static int executePerformanceStatus(net.minecraft.commands.CommandSourceStack source) {
        var manager = com.servermanagement.features.serverperformance.ServerPerformanceManager.getInstance();
        boolean enabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("server_performance");
        
        source.sendSuccess(() -> Component.literal("§6=== Server Performance ==="), false);
        source.sendSuccess(() -> Component.literal("§7Feature: " + (enabled ? "§aEnabled" : "§cDisabled")), false);
        
        if (!enabled) {
            source.sendSuccess(() -> Component.literal("§7Enable via /smconfig toggle server_performance"), false);
            return 1;
        }
        
        var status = manager.getTpsStatus();
        String tpsColor = status.getColorCode();
        source.sendSuccess(() -> Component.literal(
            tpsColor + "TPS: " + String.format("%.1f", manager.getCurrentTps()) +
            " §7| §fMSPT: " + String.format("%.1f", manager.getAverageMspt()) + "ms"
        ), false);
        
        boolean autoOpt = ModConfig.TPS_AUTO_OPTIMIZE.get();
        source.sendSuccess(() -> Component.literal(
            "§7Auto-Optimize: " + (autoOpt ? (manager.isAutoOptimizeActive() ? "§eACTIVE" : "§aStandby") : "§cOff")
        ), false);
        
        source.sendSuccess(() -> Component.literal("§7--- Subsystems ---"), false);
        source.sendSuccess(() -> Component.literal(
            "§7Item Merging: " + (ModConfig.ITEM_MERGING_ENABLED.get() ? "§aON" : "§cOFF") +
            " §7| Mob Spawn Limiter: " + (ModConfig.MOB_SPAWN_LIMITER_ENABLED.get() ? "§aON" : "§cOFF")
        ), false);
        source.sendSuccess(() -> Component.literal(
            "§7Entity Range: " + (ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.get() ? "§aON" : "§cOFF") +
            " §7| Villager Throttle: " + (ModConfig.VILLAGER_THROTTLE_ENABLED.get() ? "§aON" : "§cOFF")
        ), false);
        source.sendSuccess(() -> Component.literal(
            "§7Redstone Throttle: " + (ModConfig.REDSTONE_THROTTLE_ENABLED.get() ? "§aON" : "§cOFF") +
            " §7| TPS Monitor: " + (ModConfig.TPS_MONITOR_ENABLED.get() ? "§aON" : "§cOFF")
        ), false);
        
        return 1;
    }
    
    private static int executePerformanceStats(net.minecraft.commands.CommandSourceStack source) {
        var manager = com.servermanagement.features.serverperformance.ServerPerformanceManager.getInstance();
        boolean enabled = com.servermanagement.features.FeatureManager.isFeatureEnabled("server_performance");
        
        if (!enabled) {
            source.sendSuccess(() -> Component.literal("§cServer Performance feature is disabled"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Performance Stats ==="), false);
        source.sendSuccess(() -> Component.literal("§7Items Merged: §e" + manager.getTotalItemsMerged()), false);
        source.sendSuccess(() -> Component.literal("§7Spawns Cancelled: §e" + manager.getTotalSpawnsCancelled()), false);
        source.sendSuccess(() -> Component.literal("§7Entities Throttled: §e" + manager.getTotalEntitiesThrottled()), false);
        source.sendSuccess(() -> Component.literal("§7Redstone Updates Throttled: §e" + manager.getTotalRedstoneThrottled()), false);
        
        return 1;
    }
    
    /**
     * Registers a full performance command tree under the given root literal.
     */
    private static void registerPerformanceCommand(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> executePerformanceStatus(ctx.getSource()))
            .then(Commands.literal("status")
                .executes(ctx -> executePerformanceStatus(ctx.getSource())))
            .then(Commands.literal("stats")
                .executes(ctx -> executePerformanceStats(ctx.getSource())))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    com.servermanagement.features.serverperformance.ServerPerformanceManager.getInstance().resetStats();
                    ctx.getSource().sendSuccess(() -> Component.literal("§aPerformance stats reset"), true);
                    return 1;
                }))
            .then(Commands.literal("toggle")
                .then(Commands.argument("subsystem", StringArgumentType.string())
                    .suggests((ctx, builder) -> {
                        for (String s : new String[]{"feature", "item_merging", "mob_spawn_limiter",
                                "entity_activation_range", "villager_throttle", "redstone_throttle",
                                "tps_monitor", "auto_optimize"}) {
                            builder.suggest(s);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String subsystem = StringArgumentType.getString(ctx, "subsystem");
                        return executePerformanceToggle(ctx.getSource(), subsystem);
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            String subsystem = StringArgumentType.getString(ctx, "subsystem");
                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                            return executePerformanceSet(ctx.getSource(), subsystem, enabled);
                        })
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("setting", StringArgumentType.string())
                    .suggests((ctx, builder) -> {
                        for (String s : new String[]{"item_merge_radius", "item_merge_interval",
                                "mob_cap_multiplier", "monster_activation_range", "animal_activation_range",
                                "misc_activation_range", "villager_tick_interval", "redstone_updates_per_tick",
                                "tps_warning_threshold", "tps_critical_threshold"}) {
                            builder.suggest(s);
                        }
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes(ctx -> {
                            String setting = StringArgumentType.getString(ctx, "setting");
                            String value = StringArgumentType.getString(ctx, "value");
                            return executePerformanceSetValue(ctx.getSource(), setting, value);
                        })
                    )
                )
            )
            .then(Commands.literal("gui")
                .executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                        com.servermanagement.network.packet.SyncPerformanceSettingsPacket.syncToPlayer(player);
                        player.openMenu(new com.servermanagement.gui.provider.PerformanceSettingsMenuProvider());
                    }
                    return 1;
                })
            )
        );
    }
    
    private static int executePerformanceToggle(net.minecraft.commands.CommandSourceStack source, String subsystem) {
        boolean current = getPerformanceToggle(subsystem);
        return executePerformanceSet(source, subsystem, !current);
    }
    
    private static boolean getPerformanceToggle(String subsystem) {
        return switch (subsystem) {
            case "feature" -> ModConfig.SERVER_PERFORMANCE_ENABLED.get();
            case "item_merging" -> ModConfig.ITEM_MERGING_ENABLED.get();
            case "mob_spawn_limiter" -> ModConfig.MOB_SPAWN_LIMITER_ENABLED.get();
            case "entity_activation_range" -> ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.get();
            case "villager_throttle" -> ModConfig.VILLAGER_THROTTLE_ENABLED.get();
            case "redstone_throttle" -> ModConfig.REDSTONE_THROTTLE_ENABLED.get();
            case "tps_monitor" -> ModConfig.TPS_MONITOR_ENABLED.get();
            case "auto_optimize" -> ModConfig.TPS_AUTO_OPTIMIZE.get();
            default -> false;
        };
    }
    
    private static int executePerformanceSet(net.minecraft.commands.CommandSourceStack source, String subsystem, boolean enabled) {
        switch (subsystem) {
            case "feature" -> ModConfig.SERVER_PERFORMANCE_ENABLED.set(enabled);
            case "item_merging" -> ModConfig.ITEM_MERGING_ENABLED.set(enabled);
            case "mob_spawn_limiter" -> ModConfig.MOB_SPAWN_LIMITER_ENABLED.set(enabled);
            case "entity_activation_range" -> ModConfig.ENTITY_ACTIVATION_RANGE_ENABLED.set(enabled);
            case "villager_throttle" -> ModConfig.VILLAGER_THROTTLE_ENABLED.set(enabled);
            case "redstone_throttle" -> ModConfig.REDSTONE_THROTTLE_ENABLED.set(enabled);
            case "tps_monitor" -> ModConfig.TPS_MONITOR_ENABLED.set(enabled);
            case "auto_optimize" -> ModConfig.TPS_AUTO_OPTIMIZE.set(enabled);
            default -> {
                source.sendFailure(Component.literal("§cUnknown subsystem: " + subsystem));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal("§aSet " + subsystem + " to " + (enabled ? "§aON" : "§cOFF")), true);
        return 1;
    }
    
    private static int executePerformanceSetValue(net.minecraft.commands.CommandSourceStack source, String setting, String value) {
        try {
            switch (setting) {
                case "item_merge_radius" -> ModConfig.ITEM_MERGE_RADIUS.set(Double.parseDouble(value));
                case "item_merge_interval" -> ModConfig.ITEM_MERGE_INTERVAL.set(Integer.parseInt(value));
                case "mob_cap_multiplier" -> ModConfig.MOB_CAP_MULTIPLIER.set(Integer.parseInt(value));
                case "monster_activation_range" -> ModConfig.MONSTER_ACTIVATION_RANGE.set(Integer.parseInt(value));
                case "animal_activation_range" -> ModConfig.ANIMAL_ACTIVATION_RANGE.set(Integer.parseInt(value));
                case "misc_activation_range" -> ModConfig.MISC_ACTIVATION_RANGE.set(Integer.parseInt(value));
                case "villager_tick_interval" -> ModConfig.VILLAGER_TICK_INTERVAL.set(Integer.parseInt(value));
                case "redstone_updates_per_tick" -> ModConfig.REDSTONE_UPDATES_PER_TICK.set(Integer.parseInt(value));
                case "tps_warning_threshold" -> ModConfig.TPS_WARNING_THRESHOLD.set(Double.parseDouble(value));
                case "tps_critical_threshold" -> ModConfig.TPS_CRITICAL_THRESHOLD.set(Double.parseDouble(value));
                default -> {
                    source.sendFailure(Component.literal("§cUnknown setting: " + setting));
                    return 0;
                }
            }
            source.sendSuccess(() -> Component.literal("§aSet " + setting + " to §e" + value), true);
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§cInvalid value: " + value));
            return 0;
        }
    }

    /**
     * Syncs feature states from server to client before opening GUI.
     */
    private static void syncFeatureStates(ServerPlayer player) {
        var featureStates = com.servermanagement.features.FeatureManager.getFeatureStates();
        com.servermanagement.network.ModNetworking.sendToPlayer(
            new com.servermanagement.network.packet.SyncFeatureStatesPacket(featureStates),
            player
        );
    }
}
