package com.servermanagement.network.packet;

import com.servermanagement.features.serverperformance.JvmFlagPatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((ctx.player() instanceof ServerPlayer) ? (ServerPlayer) ctx.player() : null);
            if (player != null && player.hasPermissions(2) && confirmed) {
                java.nio.file.Path serverRoot = Paths.get("").toAbsolutePath();
                var result = JvmFlagPatcher.patchRunScripts(serverRoot);

                if (result.success()) {
                    player.sendSystemMessage(Component.literal(
                        "\u00a7a[SM+] Successfully patched: " + String.join(", ", result.patchedScripts()) +
                        "\n\u00a7e[SM+] .bak backups created. Restart server for ZGC to activate."
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "\u00a7c[SM+] No run scripts found to patch. Create a run.bat or run.sh first."
                    ));
                }
            }
        });
    }
}
