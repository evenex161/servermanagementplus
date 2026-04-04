package com.servermanagement.network;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.servermanagement.network.packet.ConsoleResponsePacket;

/**
 * Captures command output and relays it to the player's console screen
 */
public class ConsoleCommandListener implements CommandSource {
    private final ServerPlayer player;

    public ConsoleCommandListener(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendSystemMessage(Component component) {
        String text = component.getString();
        ModNetworking.sendToPlayer(new ConsoleResponsePacket("[INFO] " + text), player);
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
