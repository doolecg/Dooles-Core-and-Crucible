package dev.doole.corecrucible.mixin;

//? if <1.21.2 {

/*import dev.doole.corecrucible.registry.ModTiers;
import dev.doole.corecrucible.registry.VanillaOverrides;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla Iron, Diamond and Netherite gear repairs with the tier's alloy. 26.x uses REPAIRABLE instead.
@Mixin(TieredItem.class)
public abstract class TieredItemMixin {

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$repairWithAlloy(ItemStack stack, ItemStack repairCandidate, CallbackInfoReturnable<Boolean> cir) {
        ModTiers tier = VanillaOverrides.repairOverride((Item) (Object) this);
        if (tier != null) cir.setReturnValue(repairCandidate.is(tier.repairItems()));
    }
}
*///?}
