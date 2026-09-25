package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.SmithingBook;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The smithing recipe book sets up when the smithing screen initialises (SmithingScreen has no init).
@Mixin(ItemCombinerScreen.class)
public abstract class ItemCombinerScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void dooles_core_crucible$recipeBook(CallbackInfo ci) {
        if ((Object) this instanceof SmithingScreen smithing) SmithingBook.init(smithing);
    }
}
