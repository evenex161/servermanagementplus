package com.servermanagement.network.packet;

import com.servermanagement.client.ClientPacketHandler;
import com.servermanagement.ServerManagementMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client: Syncs the current MOTD text before opening the editor.
 */
public class SyncMotdPacket implements IPacket {
    public static final CustomPacketPayload.Type<SyncMotdPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ServerManagementMod.MOD_ID, "sync_motd_packet"));

    public static final StreamCodec<FriendlyByteBuf, SyncMotdPacket> STREAM_CODEC =
        StreamCodec.of((buf, pkt) -> pkt.encode(buf), SyncMotdPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    private final String motdText;

    public SyncMotdPacket(String motdText) {
        this.motdText = motdText;
    }

    public SyncMotdPacket(FriendlyByteBuf buf) {
        this.motdText = buf.readUtf(32767);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.motdText);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleMotdSync(motdText);
        });
    }

    public String getMotdText() {
        return motdText;
    }
}
