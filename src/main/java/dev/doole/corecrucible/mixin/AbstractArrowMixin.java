package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.gameplay.ArrowPierceHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
//? if >=1.21.2 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
*///?}

/**
 * The armor pierce a tiered launcher gives its arrows. Kept in memory only: an arrow loaded from disk
 * reads it from its saved weapon instead ({@code LauncherShots.pierceOf}).
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements ArrowPierceHolder {
    // Adds a new field to every arrow. NaN means "not fired from a tiered launcher" (no extra pierce).
    @Unique
    private double dooles_core_crucible$pierce = Double.NaN;

    @Override
    public double dooles_core_crucible$pierce() {
        return dooles_core_crucible$pierce;
    }

    @Override
    public void dooles_core_crucible$setPierce(double pierce) {
        dooles_core_crucible$pierce = pierce;
    }

    //? if <1.21.2 {
    /*// 1.21.1 checks is(Items.CROSSBOW) for the crossbow-arrow behaviour (no bounce-off, pierce sounds); 26.x doesn't.
    @ModifyReturnValue(method = "shotFromCrossbow", at = @At("RETURN"))
    private boolean dooles_core_crucible$tieredCrossbow(boolean original) {
        ItemStack weapon = ((AbstractArrow) (Object) this).getWeaponItem();
        return original || weapon != null && weapon.getItem() instanceof CrossbowItem;
    }
    *///?}
}
