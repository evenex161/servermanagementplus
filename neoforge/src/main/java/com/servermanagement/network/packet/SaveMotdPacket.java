package com.servermanagement.network.packet;

import com.servermanagement.features.motd.MotdManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Client → Server: Saves the edited MOTD text.
 * Requires admin permissions (OP level 2).
 */
public class SaveMotdPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveMotdPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("servermanagement", "save_motd"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveMotdPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveMotdPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MOTD_LENGTH = 512;

    private final String motdText;

    public SaveMotdPacket(String motdText) {
        this.motdText = motdText;
    }

    public SaveMotdPacket(FriendlyByteBuf buf) {
        this.motdText = buf.readUtf(MAX_MOTD_LENGTH);
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

        public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = ((context.player() instanceof net.minecraft.server.level.ServerPlayer) ? (net.minecraft.server.level.ServerPlayer) context.player() : null);
            if (player == null) {
                return;
            }
            if (!player.hasPermissions(2)) {
                LOGGER.warn("Player {} attempted to change MOTD without permission", player.getName().getString());
                return;
            }

            // Validate length
            String sanitized = motdText;
            if (sanitized.length() > MAX_MOTD_LENGTH) {
                sanitized = sanitized.substring(0, MAX_MOTD_LENGTH);
            }

            MotdManager.getInstance().setMotdText(sanitized);
            LOGGER.info("Player {} updated server MOTD", player.getName().getString());
            player.sendSystemMessage(Component.literal("\u00A7aServer MOTD updated successfully!"));
        });
        // packet handled
    }
}
