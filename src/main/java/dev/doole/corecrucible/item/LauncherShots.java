package dev.doole.corecrucible.item;

import dev.doole.corecrucible.gameplay.ArrowPierceHolder;
import dev.doole.corecrucible.mixin.AbstractArrowAccessor;
import dev.doole.corecrucible.registry.LauncherStats;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.2 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

/** Applies a launcher's stats to the arrows it fires. */
public final class LauncherShots {

    private LauncherShots() {
    }

    // Called when a tiered bow or crossbow creates its arrow: multiply the arrow's base damage and store its armor pierce.
    // Non-arrow projectiles (firework rockets from a crossbow) pass through unchanged.
    public static Projectile boost(Projectile projectile, LauncherStats stats) {
        if (projectile instanceof AbstractArrow arrow && stats != null) {
            arrow.setBaseDamage(((AbstractArrowAccessor) arrow).dooles_core_crucible$baseDamage() * stats.damageMultiplier());
            ((ArrowPierceHolder) arrow).dooles_core_crucible$setPierce(stats.armorPierce());
        }
        return projectile;
    }

    /** The share of an arrow's damage that ignores armor: set at launch, or read from the saved weapon after a reload. */
    public static double pierceOf(AbstractArrow arrow) {
        double pierce = ((ArrowPierceHolder) arrow).dooles_core_crucible$pierce();
        if (!Double.isNaN(pierce)) return pierce;
        ItemStack weapon = arrow.getWeaponItem();
        LauncherStats stats = weapon == null ? null : LauncherStats.of(weapon);
        return stats == null ? 0.0 : stats.armorPierce();
    }
}
