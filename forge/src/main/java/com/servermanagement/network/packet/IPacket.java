package com.servermanagement.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public interface IPacket {
    void encode(FriendlyByteBuf buf);
    
    void handle(CustomPayloadEvent.Context ctx);
}
