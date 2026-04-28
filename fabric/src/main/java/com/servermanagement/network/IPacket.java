package com.servermanagement.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Common interface implemented by every networked packet record.
 * Lets {@link ModNetworking} send any packet without per-type overloads.
 */
public interface IPacket {
    /** Channel ID this packet is registered under. */
    ResourceLocation id();

    /** Serialize the packet into {@code buf}. */
    void encode(FriendlyByteBuf buf);
}
