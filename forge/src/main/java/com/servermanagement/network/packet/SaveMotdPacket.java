package com.servermanagement.network.packet;

import com.servermanagement.features.motd.MotdManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Client → Server: Saves the edited MOTD text.
 * Requires admin permissions (OP level 2).
 */
public record SaveMotdPacket(String motdText) implements IPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MOTD_LENGTH = 512;

    public SaveMotdPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(MAX_MOTD_LENGTH));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

    @Override
    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
