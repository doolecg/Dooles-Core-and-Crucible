package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import net.minecraft.world.entity.animal.wolf.Wolf;
//?} else {
/*import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
*///?}

/**
 * Tiered wolf armor works like vanilla's. 26.x equips and repairs any wolf armor through its components,
 * but only lets Items.WOLF_ARMOR soak up hits. 1.21.1 names Items.WOLF_ARMOR for wearing it at all ({@code hasArmor},
 * which also gates rendering and damage) and for putting it on, and repairs only with armadillo scutes.
 */
@Mixin(Wolf.class)
public abstract class WolfArmorMixin {

    // True for every tier of wolf armor, vanilla's included.
    @Unique
    private static boolean dooles_core_crucible$isWolfArmor(ItemStack stack) {
        return ModItems.typeOf(stack.getItem()) == EquipType.WOLF_ARMOR;
    }

    //? if >=1.21.2 {
    @WrapOperation(method = "canArmorAbsorb",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private boolean dooles_core_crucible$tieredAbsorb(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.WOLF_ARMOR && dooles_core_crucible$isWolfArmor(stack));
    }
    //?} else {
    /*@WrapOperation(method = {"hasArmor", "mobInteract"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean dooles_core_crucible$tieredWolfArmor(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.WOLF_ARMOR && dooles_core_crucible$isWolfArmor(stack));
    }

    // Repairing: vanilla tests the armadillo material's ingredient; tiered armor takes its own tier's repair item.
    @WrapOperation(method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean dooles_core_crucible$tieredRepair(Ingredient ingredient, ItemStack stack, Operation<Boolean> original) {
        ItemStack armor = ((Wolf) (Object) this).getBodyArmorItem();
        if (!armor.is(Items.WOLF_ARMOR) && dooles_core_crucible$isWolfArmor(armor)) return armor.getItem().isValidRepairItem(armor, stack);
        return original.call(ingredient, stack);
    }
    *///?}
}
