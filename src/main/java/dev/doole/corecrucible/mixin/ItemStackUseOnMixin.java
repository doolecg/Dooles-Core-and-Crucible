package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.gameplay.HoeFarming;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Hoe till area. Hooked on the stack, not the item: tilling lives in HoeItem up to 26.2 and in the
// BLOCK_TRANSFORMER component on 26.3.
@Mixin(ItemStack.class)
public abstract class ItemStackUseOnMixin {
    // After a right-click on a block has been handled, HoeFarming checks whether a hoe just tilled and, if so, tills the
    // blocks around it too.
    @Inject(method = "useOn", at = @At("RETURN"))
    private void dooles_core_crucible$tillArea(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        HoeFarming.afterUseOn((ItemStack) (Object) this, context, cir.getReturnValue());
    }
}
