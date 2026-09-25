package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.enchant.EnchantmentHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// Runs before the pearl is thrown; setting a FAIL result cancels the throw. The two versions differ only in the return
// type of use().
//? if >=1.21.2 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
*///?}

// Soul Tether: a tethered player can't throw ender pearls.
@Mixin(EnderpearlItem.class)
public abstract class EnderpearlItemMixin {
    //? if >=1.21.2 {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$soulTether(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (EnchantmentHooks.isTethered(player)) cir.setReturnValue(InteractionResult.FAIL);
    }
    //?} else {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$soulTether(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (EnchantmentHooks.isTethered(player)) cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
    }
    *///?}
}
