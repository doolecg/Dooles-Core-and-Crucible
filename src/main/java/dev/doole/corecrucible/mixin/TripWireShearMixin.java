package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.TripWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <1.21.2 {
/*import net.minecraft.world.item.Item;
*///?}

/**
 * Breaking a tripwire with shears drops the string instead of just breaking it, checked with is(Items.SHEARS) in
 * playerWillDestroy; our shears also work. NeoForge doesn't patch this one, so it's needed on both loaders.
 */
@Mixin(TripWireBlock.class)
public abstract class TripWireShearMixin {

    //? if >=1.21.2 {
    @WrapOperation(method = "playerWillDestroy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    //?} else {
    /*@WrapOperation(method = "playerWillDestroy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    *///?}
}
