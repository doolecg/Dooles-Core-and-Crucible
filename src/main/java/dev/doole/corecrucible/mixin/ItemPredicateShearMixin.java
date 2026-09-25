package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.world.item.ItemInstance;
//?} else {
/*import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.item.ItemStack;
*///?}

/**
 * Block loot (leaves, grass, cobweb, vines, a dead bush...) drops itself when broken with shears, via a match_tool
 * predicate whose items list is minecraft:shears (on 26.3 that list comes from a registered predicate instead of
 * being inlined, but it decodes to the same runtime ItemPredicate either way). ItemPredicate#test checks the stack
 * against that items list directly; we also pass when the list contains shears and the stack is our own.
 */
@Mixin(ItemPredicate.class)
public abstract class ItemPredicateShearMixin {

    //? if >=1.21.2 {
    @WrapOperation(method = "test",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemInstance;is(Lnet/minecraft/core/HolderSet;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemInstance instance, HolderSet<Item> items, Operation<Boolean> original) {
        return original.call(instance, items)
                || (items.contains(Items.SHEARS.builtInRegistryHolder()) && instance.typeHolder().value() instanceof TieredShearsItem);
    }
    //?} else {
    /*@WrapOperation(method = "test",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/HolderSet;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, HolderSet<Item> items, Operation<Boolean> original) {
        return original.call(stack, items)
                || (items.contains(Items.SHEARS.builtInRegistryHolder()) && stack.getItem() instanceof TieredShearsItem);
    }
    *///?}
}
