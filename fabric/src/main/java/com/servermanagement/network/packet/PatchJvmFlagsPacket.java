package com.servermanagement.network.packet;

import com.servermanagement.features.serverperformance.JvmFlagPatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.network.IPacket;

import java.nio.file.Paths;
import com.servermanagement.Constants;

/**
 * Server-bound packet sent when an admin confirms patching their run scripts with ZGC flags.
 */
public record PatchJvmFlagsPacket(boolean confirmed) implements IPacket {
    public static final ResourceLocation ID = new ResourceLocation("servermanagement", "patchjvmflags_packet");

    @Override
    public ResourceLocation id() { return ID; }

    public PatchJvmFlagsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(confirmed);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2) && confirmed) {
            java.nio.file.Path serverRoot = Paths.get("").toAbsolutePath();
            var result = JvmFlagPatcher.patchRunScripts(serverRoot);

            if (result.success()) {
                String backupMsg = result.smartStartDetected()
                    ? "\u00a7a" + Constants.CHAT_PREFIX + " SmartStart detected -- your original .bak backup is preserved."
                    : "\u00a7e" + Constants.CHAT_PREFIX + " .bak backups created.";
                player.sendSystemMessage(Component.literal(
                    "\u00a7a" + Constants.CHAT_PREFIX + " Successfully patched: " + String.join(", ", result.patchedScripts()) + "\n" +
                    backupMsg + "\n" +
                    "\u00a7e" + Constants.CHAT_PREFIX + " Restart server for ZGC to activate."
                ));
                if (result.smartStartDetected()) {
                    player.sendSystemMessage(
                        Component.literal(" [Restart Now]").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD)
                            .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/sm restart"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Write restart flag and gracefully stop the server. SmartStart will auto-restart it."))))
                    );
                }
            } else {
                player.sendSystemMessage(Component.literal(
                    "\u00a7c" + Constants.CHAT_PREFIX + " No run scripts found to patch. Create a run.bat or run.sh first."
                ));
            }
        }
    }
}
