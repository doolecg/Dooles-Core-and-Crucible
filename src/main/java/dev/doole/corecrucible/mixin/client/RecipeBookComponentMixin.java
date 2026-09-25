package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {
import dev.doole.corecrucible.client.SmithingRecipeBookComponent;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The smithing recipe book borrows a vanilla book type; its open and filter state lives apart from it.
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
    // Each hook checks whether this is the smithing book (SmithingRecipeBookComponent.is). If so, the open/filter state
    // comes from the mod's own fields instead of the player's vanilla recipe book settings; otherwise vanilla runs.
    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$open(CallbackInfoReturnable<Boolean> cir) {
        if (SmithingRecipeBookComponent.is(this)) cir.setReturnValue(SmithingRecipeBookComponent.isOpen());
    }

    @Redirect(method = "setVisible", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/ClientRecipeBook;setOpen(Lnet/minecraft/world/inventory/RecipeBookType;Z)V"))
    private void dooles_core_crucible$setOpen(ClientRecipeBook book, RecipeBookType type, boolean open) {
        if (SmithingRecipeBookComponent.is(this)) SmithingRecipeBookComponent.setOpen(open);
        else book.setOpen(type, open);
    }

    @Inject(method = "isFiltering", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$filtering(CallbackInfoReturnable<Boolean> cir) {
        if (SmithingRecipeBookComponent.is(this)) cir.setReturnValue(SmithingRecipeBookComponent.isFiltering());
    }

    @Inject(method = "toggleFiltering", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$toggleFiltering(CallbackInfo ci) {
        if (SmithingRecipeBookComponent.is(this)) {
            SmithingRecipeBookComponent.setFiltering(!SmithingRecipeBookComponent.isFiltering());
            ci.cancel();
        }
    }

    // Vanilla would tell the server about the book's settings; the smithing book keeps them client-side only.
    @Inject(method = "sendUpdateSettings", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$noSettingsPacket(CallbackInfo ci) {
        if (SmithingRecipeBookComponent.is(this)) ci.cancel();
    }
}
//?}
