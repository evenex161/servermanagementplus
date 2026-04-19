package com.servermanagement.network.packet;

import com.servermanagement.features.motd.MotdManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Client → Server: Saves the edited MOTD text.
 * Requires admin permissions (OP level 2).
 */
public record SaveMotdPacket(String motdText) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<SaveMotdPacket> TYPE = 
        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("servermanagement", "save_motd_packet"));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, SaveMotdPacket> STREAM_CODEC = 
        net.minecraft.network.codec.StreamCodec.of((buf, pkt) -> pkt.encode(buf), SaveMotdPacket::new);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MOTD_LENGTH = 512;
    public SaveMotdPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(MAX_MOTD_LENGTH));
    }

        public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

        public void handle(net.minecraft.server.level.ServerPlayer player) {
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

}
}
