package com.servermanagement.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the default note-block {@code playNote} when a Slime player-head sits
 * on top, allowing {@link com.servermanagement.features.slimehead.SlimeHeadManager}
 * to play the slime-squish sound instead.
 */
@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin {

    @Inject(
            method = "playNote(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void servermanagement$slimeHead(@Nullable Entity entity, BlockState state, Level level, BlockPos pos,
                                             CallbackInfo ci) {
        if (com.servermanagement.features.slimehead.SlimeHeadManager.tryPlaySlimeSound(level, pos)) {
            ci.cancel();
        }
    }
}
