package dev.doole.corecrucible.item;

import dev.doole.corecrucible.registry.LauncherStats;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A tiered crossbow. Arrows leave it with the tier's damage multiplier and armor pierce ({@link LauncherShots});
 * the faster charge is applied to every crossbow by
 * {@code CrossbowItemMixin}. Firework rockets are left as they are.
 */
public class TieredCrossbowItem extends CrossbowItem {

    private final ModTiers tier;

    public TieredCrossbowItem(ModTiers tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    // Vanilla makes the projectile, then LauncherShots applies this tier's damage multiplier and armor pierce.
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
        return LauncherShots.boost(super.createProjectile(level, shooter, weapon, projectile, isCrit), LauncherStats.of(tier));
    }

    // 1.21.1 only: enchantability and repair material come from the tier. 26.x sets both as item components in ModItems.
    //? if <1.21.2 {
    /*@Override
    public int getEnchantmentValue() {
        return tier.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(tier.repairItems());
    }
    *///?}
}
