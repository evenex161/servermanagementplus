package com.servermanagement.mixin;

import com.servermanagement.features.worldmanager.PortalEventHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric player portal travel can route through ServerPlayer#changeDimension.
 * Hook here so portal restrictions are enforced on the concrete player path.
 *
 * <p>1.20.1 signature: {@code changeDimension(ServerLevel)} returning {@code Entity}.</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChangeDimensionMixin {

    @Inject(
            method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void servermanagement$blockPortalTravelOnPlayerPath(ServerLevel destination,
                                                                CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        try {
            if (!PortalEventHandler.onEntityTravelToDimension(player, destination.dimension())) {
                cir.setReturnValue(player);
            }
        } catch (Throwable t) {
            com.servermanagement.ServerManagementModFabric.LOGGER.error(
                    "PortalEventHandler player-path check failed", t);
        }
    }
}
