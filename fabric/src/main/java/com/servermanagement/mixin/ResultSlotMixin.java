package com.servermanagement.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge previously fired `net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent`
 * whenever a player removes an item from a crafting result slot. Fabric API has
 * no equivalent event, so {@code DailyTaskProgressListener.onItemCrafted} would
 * never be invoked and CRAFT_ITEMS daily tasks could not progress.
 *
 * <p>{@link ResultSlot#onTake(Player, ItemStack)} is the vanilla hook that runs
 * exactly once per craft (after the player has actually taken the items, so the
 * stack count is final). Inject at HEAD with the original stack still intact.
 */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {
    private static boolean servermanagement$firstCraftLogged = false;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void servermanagement$trackCraftedItems(Player player, ItemStack stack, CallbackInfo ci) {
        try {
            if (!servermanagement$firstCraftLogged) {
                servermanagement$firstCraftLogged = true;
                org.slf4j.LoggerFactory.getLogger("servermanagement").info(
                    "[DailyTaskDiag] ResultSlot.onTake first fire (player={}, item={}, count={})",
                    player.getName().getString(), stack.getItem(), stack.getCount());
            }
            com.servermanagement.features.economy.DailyTaskProgressListener
                .onItemCrafted(player, stack);
        } catch (Throwable ignored) {
            // Never let task tracking break vanilla crafting.
        }
    }
}
