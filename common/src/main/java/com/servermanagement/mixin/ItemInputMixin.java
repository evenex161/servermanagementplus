package com.servermanagement.mixin;

import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInput.class)
public class ItemInputMixin {
    @Inject(method = "createItemStack", at = @At("RETURN"))
    private void onCreateItemStack(int count, boolean allowOversizedStacks, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack != null && !stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean("servermanagement_creative", true));
        }
    }
}
