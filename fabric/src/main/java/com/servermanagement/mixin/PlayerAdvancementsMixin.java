package com.servermanagement.mixin;

import com.servermanagement.features.economy.AchievementRewardListener;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bug 3: Fabric has no public advancement-grant event, so achievement rewards
 * (the equivalent of Forge's {@code AdvancementEvent.AdvancementEarnEvent})
 * never fired. Inject after {@code PlayerAdvancements#award} returns true
 * (a criterion was granted) and forward to the existing reward listener
 * only when the advancement has just transitioned to fully done.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Unique
    private boolean servermanagement$wasDoneBeforeAward;

    @Shadow
    private ServerPlayer player;

    @Shadow
    public abstract AdvancementProgress getOrStartProgress(AdvancementHolder advancement);

    @Inject(
            method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
            at = @At("HEAD")
    )
    private void servermanagement$capturePreAwardDoneState(AdvancementHolder advancement, String criterion,
                                                           CallbackInfoReturnable<Boolean> cir) {
        AdvancementProgress progress = this.getOrStartProgress(advancement);
        this.servermanagement$wasDoneBeforeAward = progress != null && progress.isDone();
    }

    @Inject(
            method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
            at = @At("RETURN")
    )
    private void servermanagement$onAward(AdvancementHolder advancement, String criterion,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || this.player == null) {
            return;
        }
        try {
            final boolean wasDoneBeforeAward = this.servermanagement$wasDoneBeforeAward;

            // Re-check on the server queue to avoid ordering edge cases where
            // progress state is not finalized at this exact return point.
            this.player.level().getServer().execute(() -> {
                AdvancementProgress progress = this.getOrStartProgress(advancement);
                if (!wasDoneBeforeAward && progress != null && progress.isDone()) {
                    AchievementRewardListener.onAdvancementEarned(this.player, advancement);
                }
            });
        } catch (Throwable t) {
            com.servermanagement.ServerManagementModFabric.LOGGER.error(
                    "AchievementRewardListener failed for {}", advancement.id(), t);
        }
    }
}
