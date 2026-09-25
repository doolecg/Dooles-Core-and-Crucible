package dev.doole.corecrucible.item;

import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.item.FishingRodItem;
//? if <1.21.2 {
/*import net.minecraft.world.item.ItemStack;
*///?}

/**
 * A tiered fishing rod. It just carries its tier, so {@code RodStats} and the repair/enchant tags
 * can find it; the lure, luck, reel and cast bonuses are applied by {@code FishingHookMixin} on the hook it casts.
 */
public class TieredFishingRodItem extends FishingRodItem {

    private final ModTiers tier;

    public TieredFishingRodItem(ModTiers tier, Properties properties) {
        super(properties);
        this.tier = tier;
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
