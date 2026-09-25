package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.gameplay.Catalysts;
import dev.doole.corecrucible.inventory.CatalystContainer;
import dev.doole.corecrucible.inventory.CatalystMenu;
import dev.doole.corecrucible.inventory.CatalystSlot;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the catalyst slot: recipes that need a catalyst give no result without it, and use one up when
 * taken.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin implements CatalystMenu {

    // The recipe of the item being taken out, saved before vanilla clears it (see captureRecipe).
    @Unique
    private Recipe<?> dooles_core_crucible$takenRecipe;
    // The catalyst slot's one-item container, and the slot itself.
    @Unique
    private CatalystContainer dooles_core_crucible$catalyst;
    @Unique
    private Slot dooles_core_crucible$catalystSlot;
    // Game tick of the last smithing sound played (see oneSoundPerTick).
    @Unique
    private long dooles_core_crucible$lastSoundTick = Long.MIN_VALUE;

    // Right after vanilla builds the smithing menu, add the catalyst slot. When the catalyst changes, the result is
    // recalculated (createResult), just like when a normal input slot changes. The slot only shows for a base item whose
    // next upgrade needs a catalyst, or while it still holds one.
    //? if >=1.21.2 {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    *///?}
    private void dooles_core_crucible$addCatalystSlot(CallbackInfo ci) {
        SmithingMenu self = (SmithingMenu) (Object) this;
        dooles_core_crucible$catalyst = new CatalystContainer(self::createResult);
        Container inputs = ((ItemCombinerMenuAccessor) this).dooles_core_crucible$inputSlots();
        dooles_core_crucible$catalystSlot = ((AbstractContainerMenuAccessor) this).dooles_core_crucible$addSlot(
                new CatalystSlot(dooles_core_crucible$catalyst, () -> inputs.getItem(SmithingMenu.BASE_SLOT)));
    }

    @Override
    public Container dooles_core_crucible$catalyst() {
        return dooles_core_crucible$catalyst;
    }

    @Override
    public Slot dooles_core_crucible$catalystSlot() {
        return dooles_core_crucible$catalystSlot;
    }

    @Unique
    // The catalyst the recipe needs but the slot doesn't hold, or null when nothing is missing.
    private Item dooles_core_crucible$missingCatalyst(RecipeHolder<?> used) {
        Item need = used == null ? null : Catalysts.required(used.value());
        return need != null && dooles_core_crucible$catalyst != null && !dooles_core_crucible$catalyst.getItem(0).is(need) ? need : null;
    }

    // After vanilla fills in the result slot: if the recipe needs a catalyst that isn't there, empty the result again.
    @Inject(method = "createResult", at = @At("TAIL"))
    private void dooles_core_crucible$requireCatalyst(CallbackInfo ci) {
        ResultContainer result = ((ItemCombinerMenuAccessor) this).dooles_core_crucible$resultSlots();
        if (dooles_core_crucible$missingCatalyst(result.getRecipeUsed()) != null) {
            result.setRecipeUsed(null);
            result.setItem(0, ItemStack.EMPTY);
        }
    }

    // Shift-clicking the result crafts a whole stack in one server tick: play the smithing sound once, not per craft.
    @Redirect(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"))
    private void dooles_core_crucible$oneSoundPerTick(ContainerLevelAccess access, BiConsumer<Level, BlockPos> sound, Player player, ItemStack carried) {
        long now = player.level().getGameTime();
        if (now == dooles_core_crucible$lastSoundTick) return;
        dooles_core_crucible$lastSoundTick = now;
        access.execute(sound);
    }

    // awardUsedRecipes clears the used recipe, so remember it before vanilla runs.
    @Inject(method = "onTake", at = @At("HEAD"))
    private void dooles_core_crucible$captureRecipe(Player player, ItemStack carried, CallbackInfo ci) {
        RecipeHolder<?> used = ((ItemCombinerMenuAccessor) this).dooles_core_crucible$resultSlots().getRecipeUsed();
        dooles_core_crucible$takenRecipe = used == null ? null : used.value();
    }

    // After the player takes the result: use up one catalyst if the recipe needed one.
    @Inject(method = "onTake", at = @At("TAIL"))
    private void dooles_core_crucible$takeCatalyst(Player player, ItemStack carried, CallbackInfo ci) {
        Recipe<?> taken = dooles_core_crucible$takenRecipe;
        dooles_core_crucible$takenRecipe = null;
        if (taken != null && Catalysts.required(taken) != null) {
            dooles_core_crucible$catalyst.removeItem(0, 1);
        }
    }
}
