package dev.doole.corecrucible.mixin.client;

//? if <1.21.2 {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 1.21.1 picks which arm holds the line by checking is(Items.FISHING_ROD); our tiered rods are a
// different class. 26.x already uses instanceof FishingRodItem, and NeoForge 1.21.1 already widens this itself
// (to canPerformAction), so require = 0 lets this mixin compile for both loaders and just not match on NeoForge.
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    @WrapOperation(method = "getPlayerHandPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredRodHand(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof FishingRodItem;
    }
}
*///?}
