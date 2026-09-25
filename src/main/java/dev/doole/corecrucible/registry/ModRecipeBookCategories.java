package dev.doole.corecrucible.registry;

//? if >=1.21.2 {
import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.recipe.ModSmithingRecipe;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeBookCategory;

/**
 * Recipe-book categories for the smithing recipe book. 26.x builds recipe-book tabs from registered
 * categories, so the mod's smithing recipes each sit in one of these; {@link #ALL} holds no recipes itself and is
 * filled on the client with every smithing collection ({@code ClientRecipeBookMixin}), like vanilla's search tabs.
 * 1.21.1 has a fixed client enum instead, so its recipe book sorts recipes by class.
 */
public final class ModRecipeBookCategories {
    public static final RecipeBookCategory ALL = new RecipeBookCategory();
    public static final RecipeBookCategory GEAR = new RecipeBookCategory();
    public static final RecipeBookCategory ALLOYS = new RecipeBookCategory();

    private ModRecipeBookCategories() {
    }

    // Which tab a recipe's BookTab maps to.
    public static RecipeBookCategory of(ModSmithingRecipe.BookTab tab) {
        return switch (tab) {
            case GEAR -> GEAR;
            case ALLOYS -> ALLOYS;
        };
    }

    /** Called by the loader-specific registration hook. */
    public static void register(BiConsumer<Identifier, RecipeBookCategory> registrar) {
        registrar.accept(CoreCrucible.id("smithing_all"), ALL);
        registrar.accept(CoreCrucible.id("smithing_gear"), GEAR);
        registrar.accept(CoreCrucible.id("smithing_alloys"), ALLOYS);
    }
}
//?}
