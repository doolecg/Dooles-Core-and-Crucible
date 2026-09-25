package dev.doole.corecrucible.item;

import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.item.ShieldItem;
//? if <1.21.2 {
/*import net.minecraft.world.item.ItemStack;
*///?}

/**
 * A shield in the progression chain. Vanilla's {@code ShieldItem} does the blocking, banner name and
 * (on 1.21.1) the dispenser behaviour. On 26.x the tier's raise time and axe disable time live in the item's
 * {@code blocks_attacks} component; on 1.21.1 they're applied by {@code LivingEntityMixin} and {@code PlayerShieldMixin}.
 */
public class TieredShieldItem extends ShieldItem {
    private final ModTiers tier;

    public TieredShieldItem(ModTiers tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public ModTiers tier() {
        return tier;
    }

    //? if <1.21.2 {
    /*// 1.21.1's ShieldItem repairs with planks; 26.x uses the repairable component set in ModItems.
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return repairItem.is(tier.repairItems());
    }
    *///?}
}
