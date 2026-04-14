package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Adds market price information to item tooltips in all inventories.
 */
@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, value = Dist.CLIENT)
public class ClientItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Only show if market data has been synced (inflation > 0 means data received)
        if (ClientMarketData.getInflationMultiplier() <= 0) return;

        double basePrice = ClientMarketData.getBasePrice(stack);
        if (basePrice <= 0) return;

        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.literal("\u00A76\u2022 Market Price: \u00A7a$" + String.format(Locale.US, "%.2f", basePrice) + " each"));
        if (stack.getCount() > 1) {
            double stackPrice = ClientMarketData.getStackPrice(stack);
            event.getToolTip().add(Component.literal("\u00A76\u2022 Stack Value: \u00A7a$" + String.format(Locale.US, "%.2f", stackPrice) +
                " \u00A77(" + stack.getCount() + " items)"));
        }
    }
}
