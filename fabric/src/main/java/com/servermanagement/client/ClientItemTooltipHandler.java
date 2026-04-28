package com.servermanagement.client;

import com.servermanagement.ServerManagementMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/**
 * Adds market price information to item tooltips in all inventories.
 */
public class ClientItemTooltipHandler {

    public static void onItemTooltip(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.TooltipFlag context, java.util.List<net.minecraft.network.chat.Component> lines) {
        if (stack.isEmpty()) return;

        // Only show if market data has been synced (inflation > 0 means data received)
        if (ClientMarketData.getInflationMultiplier() <= 0) return;

        double basePrice = ClientMarketData.getBasePrice(stack);
        if (basePrice <= 0) return;

        lines.add(Component.empty());
        lines.add(Component.literal("\u00A76\u2022 Market Price: \u00A7a$" + String.format(Locale.US, "%.2f", basePrice) + " each"));
        if (stack.getCount() > 1) {
            double stackPrice = ClientMarketData.getStackPrice(stack);
            lines.add(Component.literal("\u00A76\u2022 Stack Value: \u00A7a$" + String.format(Locale.US, "%.2f", stackPrice) +
                " \u00A77(" + stack.getCount() + " items)"));
        }
    }
}
