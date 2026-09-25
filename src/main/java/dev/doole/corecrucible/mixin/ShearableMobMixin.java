package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.skeleton.Bogged;
//?} else {
/*import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.item.Item;
*///?}

/**
 * Vanilla only lets Items.SHEARS shear these mobs (or, on 1.21.1, pull a Wolf's armor off) in mobInteract, so our
 * shears also work. NeoForge already widens Sheep, SnowGolem and Bogged itself (through IShearable), and on 1.21.1
 * MushroomCow too, routing them through ShearsItem#interactLivingEntity instead, which our TieredShearsItem already
 * answers to just by being a ShearsItem subclass; those injections simply find nothing to wrap there (require = 0).
 * The rest (MushroomCow on 26.x, CopperGolem, SulfurCube, Wolf on 1.21.1) aren't patched by NeoForge and still need
 * this mixin on both loaders.
 */
//? if >=1.21.2 {
@Mixin({Sheep.class, MushroomCow.class, SnowGolem.class, CopperGolem.class, Bogged.class, SulfurCube.class})
//?} else {
/*@Mixin({Sheep.class, MushroomCow.class, SnowGolem.class, Bogged.class, Wolf.class})
*///?}
public abstract class ShearableMobMixin {

    //? if >=1.21.2 {
    @WrapOperation(method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Object item, Operation<Boolean> original) {
        // Don't touch the hook when the argument isn't Items.SHEARS: these methods check plenty of other items too.
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    //?} else {
    /*@WrapOperation(method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
    *///?}
}
