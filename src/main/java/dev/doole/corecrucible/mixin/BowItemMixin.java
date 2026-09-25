package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doole.corecrucible.registry.LauncherStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Bow draw speed: the tier's draw time. The ticks held are scaled before vanilla
 * turns them into power, so the power curve, the NeoForge arrow-loose event and the crit check all see the faster draw.
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {

    // Vanilla computes "ticks held" as duration - remainingTime. We change remainingTime on the way in so that value
    // comes out as the held ticks scaled by the tier's draw speed (speed 1.0 = vanilla, nothing to do).
    @ModifyVariable(method = "releaseUsing", at = @At("HEAD"), argsOnly = true)
    private int dooles_core_crucible$drawSpeed(int remainingTime, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) LivingEntity entity) {
        double speed = LauncherStats.drawSpeed(stack);
        if (speed == 1.0) return remainingTime;
        int duration = ((BowItem) (Object) this).getUseDuration(stack, entity);
        return duration - LauncherStats.scaledHeldTicks(duration - remainingTime, speed);
    }
}
