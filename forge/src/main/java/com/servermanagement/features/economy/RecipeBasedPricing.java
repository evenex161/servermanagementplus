package com.servermanagement.features.economy;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.features.gambling.ItemValuation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates item prices based on crafting recipes. An item's value is derived
 * from the cost of its ingredients, so complex recipes naturally cost more.
 * <p>
 * For example, an oak fence (4 planks + 2 sticks) will be priced higher than a
 * single oak plank. Works for all vanilla and modded items that have registered recipes.
 * <p>
 * Algorithm:
 * <ol>
 *   <li>Seed anchor prices from {@link ItemValuation}'s hardcoded map (raw materials, rare drops)</li>
 *   <li>Collect all recipes (crafting, smelting, blasting, smoking, stonecutting, smithing)</li>
 *   <li>Iteratively resolve recipe costs until prices converge (worklist approach)</li>
 *   <li>For items with multiple recipes, use the cheapest one</li>
 *   <li>Anchor prices are never overridden upward ÔÇö if the recipe cost is higher than the
 *       anchor, the anchor wins (cheapest acquisition method)</li>
 * </ol>
 */
public class RecipeBasedPricing {
    private static RecipeBasedPricing instance;

    /** Anchor prices from ItemValuation ÔÇö raw materials, rare drops, unobtainables */
    private final Map<String, Double> anchorPrices = new HashMap<>();
    /** Prices derived from recipe ingredient costs */
    private final Map<String, Double> recipePrices = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    /** Small markup on crafted items to account for crafting effort */
    private static final double CRAFTING_MARKUP = 1.05;
    /** Slightly higher markup for smelting (fuel cost) */
    private static final double SMELTING_MARKUP = 1.08;
    /** Default price for items with no recipe and no hardcoded value */
    private static final double DEFAULT_PRICE = 1.0;
    /** Maximum iterations for the convergence loop */
    private static final int MAX_ITERATIONS = 100;
    /** Price difference threshold for convergence detection */
    private static final double CONVERGENCE_THRESHOLD = 0.001;

    private RecipeBasedPricing() {
    }

    public static RecipeBasedPricing getInstance() {
        if (instance == null) {
            instance = new RecipeBasedPricing();
        }
        return instance;
    }

    /**
     * Initialize recipe-based pricing by scanning all server recipes.
     * Must be called after the server has loaded datapacks (recipes available).
     */
    public void initialize(MinecraftServer server) {
        try {
            long startTime = System.currentTimeMillis();

            anchorPrices.clear();
            recipePrices.clear();

            // Step 1: Load anchor prices from ItemValuation's hardcoded map
            anchorPrices.putAll(ItemValuation.getHardcodedValues());

            // Step 2: Collect all recipes from every supported recipe type
            List<RecipeEntry> allRecipes = collectRecipes(server);

            // Step 3: Group recipes by output item
            Map<String, List<RecipeEntry>> recipesByOutput = new HashMap<>();
            for (RecipeEntry entry : allRecipes) {
                recipesByOutput.computeIfAbsent(entry.outputId, k -> new ArrayList<>()).add(entry);
            }

            // Step 4: Iteratively resolve recipe prices until convergence
            resolveRecipePrices(recipesByOutput);

            initialized = true;

            long elapsed = System.currentTimeMillis() - startTime;
            ServerManagementMod.LOGGER.debug(
                    "Recipe-based pricing initialized: {} anchor prices, {} recipe-derived prices, {} recipes processed in {} ms",
                    anchorPrices.size(), recipePrices.size(), allRecipes.size(), elapsed);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to initialize recipe-based pricing", e);
            initialized = false;
        }
    }

    /**
     * Get the recipe-aware base price for a single item (ignoring enchantments and durability).
     * Returns the cheapest acquisition method: recipe cost vs. anchor (hardcoded) price.
     */
    public double getItemBasePrice(String itemId) {
        Double anchor = anchorPrices.get(itemId);
        Double recipe = recipePrices.get(itemId);

        if (anchor != null && recipe != null) {
            return Math.min(anchor, recipe);
        }
        if (recipe != null) {
            return recipe;
        }
        if (anchor != null) {
            return anchor;
        }
        return DEFAULT_PRICE;
    }

    /**
     * Get the recipe-aware base price for an ItemStack (single item, count=1).
     */
    public double getItemBasePrice(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return getItemBasePrice(itemId);
    }

    /**
     * Returns all computed prices (merged anchor + recipe, using cheapest) for syncing to clients.
     */
    public Map<String, Double> getAllPrices() {
        Map<String, Double> merged = new HashMap<>(anchorPrices);
        for (Map.Entry<String, Double> entry : recipePrices.entrySet()) {
            String id = entry.getKey();
            double recipePrice = entry.getValue();
            Double existing = merged.get(id);
            if (existing == null || recipePrice < existing) {
                merged.put(id, recipePrice);
            }
        }
        return Collections.unmodifiableMap(merged);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void shutdown() {
        anchorPrices.clear();
        recipePrices.clear();
        initialized = false;
        instance = null;
    }

    // ==================== Recipe Collection ====================

    private List<RecipeEntry> collectRecipes(MinecraftServer server) {
        RecipeManager mgr = server.getRecipeManager();
        var registryAccess = server.registryAccess();
        List<RecipeEntry> result = new ArrayList<>();

        collectFromType(mgr, RecipeType.CRAFTING, registryAccess, CRAFTING_MARKUP, result);
        collectFromType(mgr, RecipeType.SMELTING, registryAccess, SMELTING_MARKUP, result);
        collectFromType(mgr, RecipeType.BLASTING, registryAccess, SMELTING_MARKUP, result);
        collectFromType(mgr, RecipeType.SMOKING, registryAccess, SMELTING_MARKUP, result);
        collectFromType(mgr, RecipeType.STONECUTTING, registryAccess, CRAFTING_MARKUP, result);
        collectSmithingRecipes(mgr, registryAccess, result);

        return result;
    }

    @SuppressWarnings("unchecked")
    private <I extends net.minecraft.world.item.crafting.RecipeInput, T extends Recipe<I>> void collectFromType(
            RecipeManager mgr, RecipeType<T> type,
            net.minecraft.core.RegistryAccess registryAccess,
            double markup, List<RecipeEntry> result) {
        try {
            for (RecipeHolder<T> holder : mgr.getAllRecipesFor(type)) {
                try {
                    Recipe<?> recipe = holder.value();
                    ItemStack output = recipe.getResultItem(registryAccess);
                    if (output.isEmpty()) continue;

                    var ingredients = recipe.getIngredients();
                    List<Ingredient> nonEmpty = new ArrayList<>();
                    for (Ingredient ing : ingredients) {
                        if (ing != null && !ing.isEmpty()) {
                            nonEmpty.add(ing);
                        }
                    }
                    if (nonEmpty.isEmpty()) continue;

                    String outputId = BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
                    result.add(new RecipeEntry(outputId, output.getCount(), nonEmpty, markup));
                } catch (Exception e) {
                    // Skip individual problematic recipes silently
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.debug("Could not process recipes for type: {}", type, e);
        }
    }

    private void collectSmithingRecipes(RecipeManager mgr,
            net.minecraft.core.RegistryAccess registryAccess, List<RecipeEntry> result) {
        try {
            for (RecipeHolder<SmithingRecipe> holder : mgr.getAllRecipesFor(RecipeType.SMITHING)) {
                try {
                    SmithingRecipe recipe = holder.value();
                    ItemStack output = recipe.getResultItem(registryAccess);
                    if (output.isEmpty()) continue;

                    // Try standard getIngredients() first
                    var ingredients = recipe.getIngredients();
                    List<Ingredient> nonEmpty = new ArrayList<>();
                    for (Ingredient ing : ingredients) {
                        if (ing != null && !ing.isEmpty()) {
                            nonEmpty.add(ing);
                        }
                    }

                    // If empty, try SmithingTransformRecipe-specific field access
                    if (nonEmpty.isEmpty() && recipe instanceof SmithingTransformRecipe transformRecipe) {
                        extractSmithingTransformIngredients(transformRecipe, nonEmpty);
                    }

                    if (nonEmpty.isEmpty()) continue;

                    String outputId = BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
                    result.add(new RecipeEntry(outputId, output.getCount(), nonEmpty, CRAFTING_MARKUP));
                } catch (Exception e) {
                    // Skip individual problematic recipes silently
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.debug("Could not process smithing recipes", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void extractSmithingTransformIngredients(SmithingTransformRecipe recipe, List<Ingredient> out) {
        try {
            for (java.lang.reflect.Field field : SmithingTransformRecipe.class.getDeclaredFields()) {
                if (field.getType() == Optional.class) {
                    field.setAccessible(true);
                    Optional<Ingredient> opt = (Optional<Ingredient>) field.get(recipe);
                    opt.ifPresent(ing -> {
                        if (!ing.isEmpty()) out.add(ing);
                    });
                } else if (field.getType() == Ingredient.class) {
                    field.setAccessible(true);
                    Ingredient ing = (Ingredient) field.get(recipe);
                    if (ing != null && !ing.isEmpty()) out.add(ing);
                }
            }
        } catch (Exception e) {
            // Reflection failed ÔÇö these items will fall back to anchor prices
        }
    }

    // ==================== Price Resolution ====================

    private void resolveRecipePrices(Map<String, List<RecipeEntry>> recipesByOutput) {
        boolean changed = true;
        int iteration = 0;

        while (changed && iteration < MAX_ITERATIONS) {
            changed = false;
            iteration++;

            for (Map.Entry<String, List<RecipeEntry>> entry : recipesByOutput.entrySet()) {
                String itemId = entry.getKey();
                List<RecipeEntry> recipes = entry.getValue();

                double cheapest = Double.MAX_VALUE;
                for (RecipeEntry recipe : recipes) {
                    double cost = computeRecipeCost(recipe);
                    if (cost > 0 && cost < cheapest) {
                        cheapest = cost;
                    }
                }

                if (cheapest < Double.MAX_VALUE) {
                    Double current = recipePrices.get(itemId);
                    if (current == null || Math.abs(current - cheapest) > CONVERGENCE_THRESHOLD) {
                        recipePrices.put(itemId, cheapest);
                        changed = true;
                    }
                }
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            ServerManagementMod.LOGGER.warn("Recipe pricing did not fully converge after {} iterations", MAX_ITERATIONS);
        } else {
            ServerManagementMod.LOGGER.debug("Recipe pricing converged in {} iterations", iteration);
        }
    }

    private double computeRecipeCost(RecipeEntry recipe) {
        double totalCost = 0;
        for (Ingredient ingredient : recipe.ingredients) {
            double ingredientCost = cheapestMatchingPrice(ingredient);
            totalCost += ingredientCost;
        }
        return (totalCost / recipe.outputCount) * recipe.markup;
    }

    /**
     * For a tag-based ingredient with multiple matching items, returns the cheapest option.
     */
    private double cheapestMatchingPrice(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        if (options.length == 0) return DEFAULT_PRICE;

        double cheapest = Double.MAX_VALUE;
        for (ItemStack option : options) {
            String id = BuiltInRegistries.ITEM.getKey(option.getItem()).toString();
            double price = lookupCurrentPrice(id);
            if (price < cheapest) {
                cheapest = price;
            }
        }
        return cheapest == Double.MAX_VALUE ? DEFAULT_PRICE : cheapest;
    }

    /**
     * Looks up the current best-known price for an item during resolution.
     * Prefers the cheaper of recipe price vs anchor price.
     */
    private double lookupCurrentPrice(String itemId) {
        Double recipePrice = recipePrices.get(itemId);
        Double anchorPrice = anchorPrices.get(itemId);

        if (recipePrice != null && anchorPrice != null) {
            return Math.min(recipePrice, anchorPrice);
        }
        if (recipePrice != null) return recipePrice;
        if (anchorPrice != null) return anchorPrice;
        return DEFAULT_PRICE;
    }

    // ==================== Inner Classes ====================

    private static class RecipeEntry {
        final String outputId;
        final int outputCount;
        final List<Ingredient> ingredients;
        final double markup;

        RecipeEntry(String outputId, int outputCount, List<Ingredient> ingredients, double markup) {
            this.outputId = outputId;
            this.outputCount = outputCount;
            this.ingredients = ingredients;
            this.markup = markup;
        }
    }
}
