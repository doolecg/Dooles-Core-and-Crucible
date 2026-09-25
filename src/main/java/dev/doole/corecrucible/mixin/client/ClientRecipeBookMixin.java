package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {
import dev.doole.corecrucible.registry.ModRecipeBookCategories;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The smithing recipe book's "All" tab collects every smithing category, like vanilla's search tabs.
@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {
    @Shadow
    @Mutable
    private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab;

    // After vanilla groups recipes into tabs, add an "All" tab holding the gear, alloy and vanilla smithing tabs combined.
    // The map is replaced rather than edited because vanilla's copy is immutable.
    @Inject(method = "rebuildCollections", at = @At("TAIL"))
    private void dooles_core_crucible$smithingAll(CallbackInfo ci) {
        List<RecipeCollection> all = new ArrayList<>();
        for (ExtendedRecipeBookCategory category : List.of(ModRecipeBookCategories.GEAR, ModRecipeBookCategories.ALLOYS,
                RecipeBookCategories.SMITHING)) {
            all.addAll(collectionsByTab.getOrDefault(category, List.of()));
        }
        Map<ExtendedRecipeBookCategory, List<RecipeCollection>> map = new HashMap<>(collectionsByTab);
        map.put(ModRecipeBookCategories.ALL, List.copyOf(all));
        collectionsByTab = Map.copyOf(map);
    }
}
//?}
