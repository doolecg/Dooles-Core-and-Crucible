package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <1.21.2 {
/*import net.minecraft.world.item.Item;
*///?}

/**
 * Vanilla only lets Items.SHEARS harvest a beehive's honeycomb or carve a pumpkin, checked in useItemOn; our shears
 * also work. NeoForge already widens both to canPerformAction, which our TieredShearsItem already answers to as a
 * ShearsItem subclass, so require = 0 leaves that patch alone.
 */
@Mixin({BeehiveBlock.class, PumpkinBlock.class})
// WrapOperation wraps vanilla's "is this Items.SHEARS?" call: run the original check, and also answer yes for our shears.
public abstract class BlockShearMixin {

    //? if >=1.21.2 {
    @WrapOperation(method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    //?} else {
    /*@WrapOperation(method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    *///?}
}
