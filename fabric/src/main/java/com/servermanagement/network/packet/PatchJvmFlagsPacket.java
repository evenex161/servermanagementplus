package com.servermanagement.network.packet;

import com.servermanagement.features.serverperformance.JvmFlagPatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Paths;
import java.util.List;

/**
 * Server-bound packet sent when an admin confirms patching their run scripts with ZGC flags.
 */
public record PatchJvmFlagsPacket(boolean confirmed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PatchJvmFlagsPacket> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "patchjvmflags_packet"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, PatchJvmFlagsPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), PatchJvmFlagsPacket::new);
    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public PatchJvmFlagsPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(confirmed);
    }

    public void handle(ServerPlayer player) {
        if (player != null && player.hasPermissions(2) && confirmed) {
            java.nio.file.Path serverRoot = Paths.get("").toAbsolutePath();
            var result = JvmFlagPatcher.patchRunScripts(serverRoot);

            if (result.success()) {
                String backupMsg = result.smartStartDetected()
                    ? "\u00a7a[SM+] SmartStart detected -- your original .bak backup is preserved."
                    : "\u00a7e[SM+] .bak backups created.";
                player.sendSystemMessage(Component.literal(
                    "\u00a7a[SM+] Successfully patched: " + String.join(", ", result.patchedScripts()) + "\n" +
                    backupMsg + "\n" +
                    "\u00a7e[SM+] Restart server for ZGC to activate."
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
                    "\u00a7c[SM+] No run scripts found to patch. Create a run.bat or run.sh first."
                ));
            }
        }
    }
}
