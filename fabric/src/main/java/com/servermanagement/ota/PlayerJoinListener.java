package com.servermanagement.ota;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.updater.ServerUpdateScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles player join events to trigger update notifications
 */
public class PlayerJoinListener {
    
    public static void onPlayerJoin(ServerPlayer player) {
        if (player.hasPermissions(2) && ServerUpdateScheduler.pendingUpdate != null) {
            String version = ServerUpdateScheduler.pendingUpdate.version();
            
            MutableComponent message = Component.literal("\n")
                    .append(Component.literal(" ServerManagement+ Update Available!\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" Version: " + version + "\n\n").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" [Update Now]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sm update"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to update the server")))))
                    .append(Component.literal("   "))
                    .append(Component.literal(" [Skip Version]").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)
                            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sm update skip"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to skip this version")))))
                    .append(Component.literal("\n"));
            
            player.sendSystemMessage(message);
        }
    }
}
