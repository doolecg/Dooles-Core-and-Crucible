package dev.doole.corecrucible.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
//? if >=1.21.2 {
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import dev.doole.corecrucible.registry.ModRecipeBookCategories;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
//?} else {
/*import net.minecraft.core.HolderLookup;
*///?}

/**
 * Shared base of the mod's smithing recipes. Subclasses supply three ingredients plus their own
 * matching and assembly; this class adapts them to each version's {@link SmithingRecipe}.
 */
public abstract class ModSmithingRecipe implements SmithingRecipe {

    protected abstract Ingredient template();

    protected abstract Ingredient base();

    protected abstract Ingredient addition();

    /** Which smithing recipe-book tab the recipe sits in. */
    public enum BookTab { GEAR, ALLOYS }

    public abstract BookTab bookTab();

    protected abstract ItemStack result(SmithingRecipeInput input);

    /** The result shown in recipe viewers. */
    public abstract ItemStack resultPreview();

    @Override
    // Vanilla calls this whenever the smithing table's inputs change: true when all three slots fit this recipe.
    public boolean matches(SmithingRecipeInput input, Level level) {
        return template().test(input.template()) && base().test(input.base()) && addition().test(input.addition());
    }

    // 26.x: the rest of vanilla's SmithingRecipe interface, answered from the three ingredients above. display() is what
    // the recipe book and recipe viewers draw.
    //? if >=1.21.2 {
    private PlacementInfo placementInfo;

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return result(input);
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(template());
    }

    @Override
    public Ingredient baseIngredient() {
        return base();
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(addition());
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(List.of(template(), base(), addition()));
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipeBookCategories.of(bookTab());
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay result = new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(resultPreview()));
        return List.of(new SmithingRecipeDisplay(template().display(), base().display(), addition().display(), result,
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
    }
    //?} else {
    /*@Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return result(input);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultPreview();
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template().test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base().test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition().test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    /^* 1.21.1 recipe book: what the ghost recipe shows in the template, base, addition and result slots, in step. ^/
    public java.util.List<java.util.List<ItemStack>> ghostStacks() {
        return java.util.List.of(java.util.List.of(template().getItems()), java.util.List.of(base().getItems()),
                java.util.List.of(addition().getItems()), java.util.List.of(resultPreview()));
    }
    *///?}
}
