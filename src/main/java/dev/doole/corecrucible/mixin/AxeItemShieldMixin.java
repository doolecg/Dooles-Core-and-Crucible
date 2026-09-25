package dev.doole.corecrucible.mixin;

// 1.21.1 only (the whole class is commented out on 26.x by the version markers below).
//? if <1.21.2 {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * On 1.21.1 an axe doesn't strip a log while the off hand holds a shield (you meant to raise it), but only for
 * Items.SHIELD. Tiered shields count too. 26.x checks the blocks_attacks component instead.
 ^/
@Mixin(AxeItem.class)
public abstract class AxeItemShieldMixin {
    @WrapOperation(method = "playerHasShieldUseIntent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean dooles_core_crucible$tieredShield(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHIELD && stack.getItem() instanceof ShieldItem);
    }
}
*///?}
