package com.servermanagement.features.gambling;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Valuation system for items used in gambling
 */
public class ItemValuation {
    private static final Map<String, Double> ITEM_VALUES = new HashMap<>();
    
    static {
        // Precious materials
        ITEM_VALUES.put("minecraft:diamond", 100.0);
        ITEM_VALUES.put("minecraft:emerald", 50.0);
        ITEM_VALUES.put("minecraft:gold_ingot", 25.0);
        ITEM_VALUES.put("minecraft:iron_ingot", 10.0);
        ITEM_VALUES.put("minecraft:netherite_ingot", 500.0);
        ITEM_VALUES.put("minecraft:netherite_scrap", 125.0);
        
        // Blocks
        ITEM_VALUES.put("minecraft:diamond_block", 900.0);
        ITEM_VALUES.put("minecraft:emerald_block", 450.0);
        ITEM_VALUES.put("minecraft:gold_block", 225.0);
        ITEM_VALUES.put("minecraft:iron_block", 90.0);
        ITEM_VALUES.put("minecraft:netherite_block", 4500.0);
        
        // Rare items
        ITEM_VALUES.put("minecraft:nether_star", 1000.0);
        ITEM_VALUES.put("minecraft:elytra", 750.0);
        ITEM_VALUES.put("minecraft:totem_of_undying", 500.0);
        ITEM_VALUES.put("minecraft:beacon", 800.0);
        ITEM_VALUES.put("minecraft:dragon_egg", 5000.0);
        ITEM_VALUES.put("minecraft:enchanted_golden_apple", 200.0);
        
        // Enchanted books (base value)
        ITEM_VALUES.put("minecraft:enchanted_book", 100.0);
        
        // Tools and armor (diamond)
        ITEM_VALUES.put("minecraft:diamond_sword", 200.0);
        ITEM_VALUES.put("minecraft:diamond_pickaxe", 300.0);
        ITEM_VALUES.put("minecraft:diamond_axe", 300.0);
        ITEM_VALUES.put("minecraft:diamond_shovel", 100.0);
        ITEM_VALUES.put("minecraft:diamond_hoe", 200.0);
        ITEM_VALUES.put("minecraft:diamond_helmet", 500.0);
        ITEM_VALUES.put("minecraft:diamond_chestplate", 800.0);
        ITEM_VALUES.put("minecraft:diamond_leggings", 700.0);
        ITEM_VALUES.put("minecraft:diamond_boots", 400.0);
        
        // Tools and armor (netherite)
        ITEM_VALUES.put("minecraft:netherite_sword", 600.0);
        ITEM_VALUES.put("minecraft:netherite_pickaxe", 800.0);
        ITEM_VALUES.put("minecraft:netherite_axe", 800.0);
        ITEM_VALUES.put("minecraft:netherite_shovel", 500.0);
        ITEM_VALUES.put("minecraft:netherite_hoe", 600.0);
        ITEM_VALUES.put("minecraft:netherite_helmet", 1300.0);
        ITEM_VALUES.put("minecraft:netherite_chestplate", 1600.0);
        ITEM_VALUES.put("minecraft:netherite_leggings", 1500.0);
        ITEM_VALUES.put("minecraft:netherite_boots", 1200.0);
        
        // Potions and materials
        ITEM_VALUES.put("minecraft:ender_pearl", 5.0);
        ITEM_VALUES.put("minecraft:blaze_rod", 8.0);
        ITEM_VALUES.put("minecraft:ghast_tear", 15.0);
        ITEM_VALUES.put("minecraft:slime_ball", 3.0);
        
        // Common materials (low value)
        ITEM_VALUES.put("minecraft:coal", 1.0);
        ITEM_VALUES.put("minecraft:redstone", 2.0);
        ITEM_VALUES.put("minecraft:lapis_lazuli", 3.0);
        ITEM_VALUES.put("minecraft:quartz", 2.0);
    }
    
    /**
     * Get the monetary value of an item stack
     */
    public static double getItemValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getKey(stack.getItem()).toString();
        
        double baseValue = ITEM_VALUES.getOrDefault(itemId, 1.0); // Default to $1
        
        // Multiply by stack count
        double totalValue = baseValue * stack.getCount();
        
        // Bonus for enchantments
        if (stack.isEnchanted()) {
            int enchantmentCount = stack.getEnchantmentTags().size();
            totalValue *= (1.0 + (enchantmentCount * 0.2)); // 20% bonus per enchantment
        }
        
        // Penalty for damage (tools/armor)
        if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
            double durabilityPercent = 1.0 - ((double) stack.getDamageValue() / stack.getMaxDamage());
            totalValue *= durabilityPercent;
        }
        
        return Math.max(1.0, totalValue); // Minimum $1
    }
    
    /**
     * Check if an item is valuable enough to gamble with
     */
    public static boolean isItemGambleable(ItemStack stack) {
        return getItemValue(stack) >= GamblingManager.MIN_BET;
    }
    
    /**
     * Get a display string for item value
     */
    public static String getValueString(ItemStack stack) {
        double value = getItemValue(stack);
        return String.format("$%.2f", value);
    }
}
