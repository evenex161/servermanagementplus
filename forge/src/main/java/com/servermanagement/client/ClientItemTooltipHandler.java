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
                event.getToolTip().add(Component.empty());
                event.getToolTip().add(Component.literal("\u00A7c\u26A0 Item is on the Trading Blacklist"));
                return;
            }
        }

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
