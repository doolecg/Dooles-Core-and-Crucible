package dev.doole.corecrucible.mixin;

//? if <1.21.2 {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.ShieldDecorationRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * 1.21.1's banner-on-shield recipe only takes Items.SHIELD; tiered shields take banners too. assemble
 * copies the shield it was given, so the tier is kept. 26.x's recipe is data: gen_data.py writes one per tier.
 ^/
@Mixin(ShieldDecorationRecipe.class)
public abstract class ShieldDecorationRecipeMixin {
    @WrapOperation(method = {"matches", "assemble"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean dooles_core_crucible$tieredShield(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHIELD && stack.getItem() instanceof ShieldItem);
    }
}
*///?}
