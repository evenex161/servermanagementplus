package com.servermanagement.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class CreativeItemImportMixin {
    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"))
    private void onHandleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        ItemStack stack = packet.getItem();
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putBoolean("servermanagement_creative", true);
        }
    }
}
