package com.servermanagement.network.packet;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.network.ModNetworking;
import com.servermanagement.security.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record SecurePacketWrapper(CustomPacketPayload payload, String token) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SecurePacketWrapper> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("servermanagement", "secure_wrapper_packet"));
    
    public static final StreamCodec<FriendlyByteBuf, SecurePacketWrapper> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SecurePacketWrapper decode(FriendlyByteBuf buf) {
            String token = buf.readUtf(256);
            String typeStr = buf.readUtf(256);
            Identifier typeId = Identifier.tryParse(typeStr);
            if (typeId == null) {
                throw new IllegalArgumentException("Invalid wrapper inner packet identifier: " + typeStr);
            }
            CustomPacketPayload.Type<?> type = new CustomPacketPayload.Type<>(typeId);
            
            StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload> codec = ModNetworking.getC2SCodec(type);
            if (codec == null) {
                throw new IllegalArgumentException("Unknown wrapper inner packet type: " + typeId);
            }
            CustomPacketPayload payload = codec.decode(buf);
            return new SecurePacketWrapper(payload, token);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void encode(FriendlyByteBuf buf, SecurePacketWrapper pkt) {
            buf.writeUtf(pkt.token(), 256);
            buf.writeUtf(pkt.payload().type().id().toString(), 256);
            
            StreamCodec<FriendlyByteBuf, CustomPacketPayload> codec = (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) ModNetworking.getC2SCodec(pkt.payload().type());
            if (codec == null) {
                throw new IllegalArgumentException("Unknown wrapper inner packet type: " + pkt.payload().type().id());
            }
            codec.encode(buf, pkt.payload());
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(ServerPlayer player) {
        if (player == null) return;

        // 1. Rate Limiting Check
        if (!com.servermanagement.network.PacketTimestampTracker.checkRateLimit(player)) {
            return;
        }

        // 2. Session Token Validation
        if (!SessionManager.getInstance().validateSession(player.getUUID(), token)) {
            ServerManagementMod.LOGGER.warn("SECURITY ALERT: Invalid session token for player {} attempting to send {}", 
                player.getName().getString(), payload.type().id());
            return;
        }

        // 3. Delegate to the actual handler stored in ModNetworking
        try {
            var handler = ModNetworking.getC2SHandler(payload.type());
            if (handler != null) {
                handler.accept(payload, player);
            } else {
                ServerManagementMod.LOGGER.error("No handler registered for inner packet: {}", payload.type().id());
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Error handling packet {}: ", payload.type().id(), e);
        }
    }
}
