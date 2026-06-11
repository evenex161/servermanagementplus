package com.servermanagement.mixin;

import com.servermanagement.features.worldmanager.PortalEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric player portal travel can route through ServerPlayer#changeDimension.
 * Hook here so portal restrictions are enforced on the concrete player path.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChangeDimensionMixin {

    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void servermanagement$blockPortalTravelOnPlayerPath(TeleportTransition transition,
                                                                CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        try {
            if (!PortalEventHandler.onEntityTravelToDimension(player, transition.newLevel().dimension())) {
                cir.setReturnValue(player);
            }
        } catch (Throwable t) {
            com.servermanagement.ServerManagementModFabric.LOGGER.error(
                    "PortalEventHandler player-path check failed", t);
        }
    }
}
