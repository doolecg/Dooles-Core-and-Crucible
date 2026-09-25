package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {
import java.util.Set;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The smithing recipe book tightens vanilla's craftable check (SmithingRecipeBookComponent#selectMatchingRecipes).
@Mixin(RecipeCollection.class)
public interface RecipeCollectionAccessor {
    @Accessor("craftable")
    Set<RecipeDisplayId> dooles_core_crucible$craftable();
}
//?}
