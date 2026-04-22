package com.servermanagement.mixin;

import com.servermanagement.features.worldmanager.PortalEventHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bug 1: On Forge {@code EntityTravelToDimensionEvent} fires before travel
 * happens and may be cancelled. Fabric only exposes
 * {@code ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD}, which is
 * post-travel and forces an ugly teleport-back. Inject at the head of
 * {@link Entity#changeDimension(DimensionTransition)} and short-circuit
 * before the player actually moves when {@link PortalEventHandler} says no.
 */
@Mixin(Entity.class)
public abstract class EntityChangeDimensionMixin {

    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void servermanagement$blockPortalTravel(DimensionTransition transition,
                                                    CallbackInfoReturnable<Entity> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer)) {
            return;
        }
        try {
            if (!PortalEventHandler.onEntityTravelToDimension(self, transition.newLevel().dimension())) {
                cir.setReturnValue(self);
            }
        } catch (Throwable t) {
            com.servermanagement.ServerManagementModFabric.LOGGER.error(
                    "PortalEventHandler check failed", t);
        }
    }
}
