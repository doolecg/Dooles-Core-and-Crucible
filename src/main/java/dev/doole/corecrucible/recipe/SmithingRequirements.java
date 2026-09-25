package dev.doole.corecrucible.recipe;

import dev.doole.corecrucible.gameplay.Catalysts;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
//? if >=1.21.2 {
import java.util.Optional;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
//?}

/**
 * What one smithing craft takes from the inventory: a template, a base, an addition and an optional
 * catalyst. The recipe book uses it to decide whether a recipe is craftable and to move the items in.
 * A null predicate means the slot stays empty (vanilla smithing recipes may leave the template or addition out).
 */
public record SmithingRequirements(Predicate<ItemStack> template, Predicate<ItemStack> base,
                                   Predicate<ItemStack> addition, Item catalyst) {

    /** Server side (and the 1.21.1 client, which has the recipes): read straight from the recipe. */
    public static SmithingRequirements of(Recipe<?> recipe) {
        if (recipe instanceof ModSmithingRecipe mod) {
            return new SmithingRequirements(mod.template()::test, mod.base()::test, mod.addition()::test, Catalysts.required(recipe));
        }
        if (!(recipe instanceof SmithingRecipe smithing)) return null;
        //? if >=1.21.2 {
        return new SmithingRequirements(asPredicate(smithing.templateIngredient()),
                smithing.baseIngredient()::test, asPredicate(smithing.additionIngredient()), null);
        //?} else {
        /*return new SmithingRequirements(smithing::isTemplateIngredient, smithing::isBaseIngredient, smithing::isAdditionIngredient, null);
        *///?}
    }

    //? if >=1.21.2 {
    /** Turns an optional ingredient (the template and addition slots can be empty) into a predicate, or null. */
    private static Predicate<ItemStack> asPredicate(Optional<Ingredient> ingredient) {
        return ingredient.isPresent() ? ingredient.get()::test : null;
    }

    /**
     * The 26.x client only has the recipe's display (the server keeps the recipes), so the catalyst is read back from
     * the result.
     */
    public static SmithingRequirements of(SmithingRecipeDisplay display, ContextMap context) {
        List<ItemStack> templates = display.template().resolveForStacks(context);
        List<ItemStack> bases = display.base().resolveForStacks(context);
        List<ItemStack> additions = display.addition().resolveForStacks(context);
        List<ItemStack> results = display.result().resolveForStacks(context);
        Item catalyst = null;
        if (!results.isEmpty()) {
            ItemStack result = results.get(0);
            ModTiers tier = ModItems.tierOf(result.getItem());
            if (tier != null) catalyst = Catalysts.forUpgrade(tier);
            // An alloy ingot as the result means the alloy recipe; its catalyst rule is different from gear upgrades.
            for (ModTiers alloy : ModItems.ALLOY_TIERS) {
                if (result.is(ModItems.alloyIngot(alloy))) catalyst = Catalysts.forAlloy(alloy);
            }
        }
        return new SmithingRequirements(templates.isEmpty() ? null : anyOf(templates), anyOf(bases),
                additions.isEmpty() ? null : anyOf(additions), catalyst);
    }

    /** True when the tested stack matches the item of any of the given stacks. */
    private static Predicate<ItemStack> anyOf(List<ItemStack> options) {
        return stack -> {
            for (ItemStack option : options) {
                if (stack.is(option.getItem())) return true;
            }
            return false;
        };
    }
    //?}
}
