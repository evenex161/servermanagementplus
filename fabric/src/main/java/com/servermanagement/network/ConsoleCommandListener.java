package com.servermanagement.network;

import com.servermanagement.network.packet.ConsoleResponsePacket;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Captures command output and relays it to the player's console screen.
 * Used as the CommandSource for commands executed via the in-game console GUI.
 */
public class ConsoleCommandListener implements CommandSource {
    private final ServerPlayer player;

    public ConsoleCommandListener(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendSystemMessage(Component component) {
        String text = component.getString();
        if (text != null && !text.isEmpty()) {
            ModNetworking.sendToPlayer(player, new ConsoleResponsePacket("[CMD] " + text));
        }
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }
}
