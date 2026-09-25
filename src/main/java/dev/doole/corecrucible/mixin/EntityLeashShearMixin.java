package dev.doole.corecrucible.mixin;

//? if >=1.21.2 {

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * On 26.x, shearing a leashed mob or a leash knot cuts the leash, and shearing any entity wearing body armor pulls
 * it off; both check is(Items.SHEARS) directly in interact(). NeoForge already widens both to canPerformAction,
 * which ShearsItem (and so our TieredShearsItem) already answers, so require = 0 leaves that patch alone.
 */
@Mixin({Entity.class, LeashFenceKnotEntity.class})
// Wraps vanilla's "is this Items.SHEARS?" check: keep vanilla's answer, and also answer yes for our tiered shears.
public abstract class EntityLeashShearMixin {

    @WrapOperation(method = "interact",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShears(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHEARS && stack.getItem() instanceof TieredShearsItem);
    }
}
//?}
