package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Base interface for all mod network packets.
 * Extends CustomPacketPayload for NeoForge payload-based networking.
 */
public interface IPacket extends CustomPacketPayload {
    void encode(FriendlyByteBuf buf);
    
    void handle(IPayloadContext ctx);
}
