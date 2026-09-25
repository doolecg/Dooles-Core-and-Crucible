package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doole.corecrucible.registry.LauncherStats;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Crossbow charge time: vanilla's (Quick Charge included) scaled by the tier's draw time. Everything reads it through this method: loading, the charge sounds, the pull model and mob AI.
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    // Takes the charge time vanilla returns and scales it by the crossbow tier's draw speed.
    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int dooles_core_crucible$chargeSpeed(int ticks, @Local(argsOnly = true) ItemStack crossbow) {
        double speed = LauncherStats.drawSpeed(crossbow);
        return speed == 1.0 ? ticks : LauncherStats.scaledChargeTicks(ticks, speed);
    }
}
