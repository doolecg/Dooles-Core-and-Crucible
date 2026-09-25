package dev.doole.corecrucible.compat.jei;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.compat.ForgingDisplays;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@code dooles_core_crucible:crucible_smithing}: gear upgrades and alloys. The catalyst slot
 * appears after the addition slot on recipes that need one.
 */
public class ForgingSmithingCategory implements IRecipeCategory<ForgingDisplays.Smithing> {
    public static final RecipeType<ForgingDisplays.Smithing> TYPE =
            RecipeType.create(CoreCrucible.MOD_ID, "crucible_smithing", ForgingDisplays.Smithing.class);

    private final IDrawable icon;

    public ForgingSmithingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.SMITHING_TABLE));
    }

    @Override
    public RecipeType<ForgingDisplays.Smithing> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei." + CoreCrucible.MOD_ID + ".crucible_smithing");
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 38;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    // Slot positions in pixels: template, base, addition, then the catalyst (only if the recipe needs one), and the
    // result on the right.
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingDisplays.Smithing recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 20).setStandardSlotBackground().addItemStacks(recipe.template());
        builder.addInputSlot(19, 20).setStandardSlotBackground().addItemStacks(recipe.base());
        builder.addInputSlot(37, 20).setStandardSlotBackground().addItemStacks(recipe.addition());
        if (!recipe.catalyst().isEmpty()) {
            builder.addInputSlot(55, 20).setStandardSlotBackground().addItemStack(recipe.catalyst());
        }
        builder.addOutputSlot(110, 20).setStandardSlotBackground().addItemStacks(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ForgingDisplays.Smithing recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(80, 20);
    }
}
