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

        // Economy feature master toggle check
        if (!com.servermanagement.features.FeatureManager.isFeatureEnabled("economy")) return;

        // Admin setting check
        if (!ClientPacketHandler.showMarketValueTooltips()) return;

        // Blacklist Notice Check
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String blacklist = ClientPacketHandler.getTradeBlacklist();
        if (blacklist != null && !blacklist.isEmpty()) {
            boolean isBlacklisted = false;
            for (String b : blacklist.split(",")) {
                if (b.trim().equals(itemId)) {
                    isBlacklisted = true;
                    break;
                }
            }
            if (isBlacklisted) {
                lines.add(Component.empty());
                lines.add(Component.literal("\u00A7c\u26A0 Item is on the Trading Blacklist"));
                return;
            }
        }

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
