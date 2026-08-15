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

        // GC Advisory notification for admins
        if (player.hasPermissions(2)) {
            var urgency = com.servermanagement.features.serverperformance.GCAdvisor.getUrgency();
            boolean needsPatching = urgency != com.servermanagement.features.serverperformance.GCAdvisor.UrgencyLevel.OK
                && !com.servermanagement.features.serverperformance.GCAdvisor.isScriptPatched()
                && !com.servermanagement.features.serverperformance.GCAdvisor.isDismissed()
                && !com.servermanagement.features.serverperformance.JvmFlagPatcher.isAlreadyOptimal(
                    java.nio.file.Paths.get("").toAbsolutePath());

            if (needsPatching) {
                var gcType = com.servermanagement.features.serverperformance.GCAdvisor.getDetectedGC();
                boolean isDH = urgency == com.servermanagement.features.serverperformance.GCAdvisor.UrgencyLevel.CRITICAL;
                String desc = isDH
                    ? gcType.getDisplayName() + " + Distant Horizons detected"
                    : gcType.getDisplayName() + " detected -- ZGC recommended";

                ChatFormatting bannerColor = isDH ? ChatFormatting.RED : ChatFormatting.GOLD;
                MutableComponent gcMsg = Component.literal("\n")
                    .append(Component.literal(" [SM+] GC Advisor: " + desc + "\n").withStyle(bannerColor, ChatFormatting.BOLD))
                    .append(Component.literal(" Recommended: -XX:+UseZGC -XX:+ZGenerational\n\n").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" [Patch Run Scripts]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sm gc patch"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Patch your run.bat/run.sh with ZGC flags (.bak backup created)")))))
                    .append(Component.literal("   "))
                    .append(Component.literal("[Dismiss]").withStyle(ChatFormatting.GRAY)
                        .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sm gc dismiss"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Hide this notification until server restart")))))
                    .append(Component.literal("\n"));

                player.sendSystemMessage(gcMsg);
            }
        }
    }
}
