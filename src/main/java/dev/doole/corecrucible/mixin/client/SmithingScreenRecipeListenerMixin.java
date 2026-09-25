package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {
import dev.doole.corecrucible.client.SmithingBook;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;

// Vanilla hands newly unlocked recipes and the server's ghost recipe to screens that are RecipeUpdateListeners.
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenRecipeListenerMixin implements RecipeUpdateListener {
    @Override
    // Called when the player unlocks new recipes: refresh the book's list.
    public void recipesUpdated() {
        SmithingBook.recipesUpdated(this);
    }

    @Override
    // Called when the server sends back a recipe the player can't fully craft: show its items as faded "ghost" items.
    public void fillGhostRecipe(RecipeDisplay display) {
        SmithingBook.fillGhostRecipe(this, display);
    }
}
//?}
