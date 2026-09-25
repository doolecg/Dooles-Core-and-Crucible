package dev.doole.corecrucible.mixin;

// 1.21.1 only. Returning a value from a HEAD inject skips vanilla's own check; items with no override (null tier)
// fall through to vanilla.
//? if <1.21.2 {

/*import dev.doole.corecrucible.registry.ModTiers;
import dev.doole.corecrucible.registry.VanillaOverrides;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla Iron, Diamond and Netherite gear repairs with the tier's alloy. 26.x uses REPAIRABLE instead.
@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin {

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$repairWithAlloy(ItemStack stack, ItemStack repairCandidate, CallbackInfoReturnable<Boolean> cir) {
        ModTiers tier = VanillaOverrides.repairOverride((Item) (Object) this);
        if (tier != null) cir.setReturnValue(repairCandidate.is(tier.repairItems()));
    }
}
*///?}
