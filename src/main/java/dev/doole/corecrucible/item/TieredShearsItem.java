package dev.doole.corecrucible.item;

import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.item.ShearsItem;
//? if <1.21.2 {
/*import net.minecraft.world.item.ItemStack;
*///?}

/**
 * A tiered pair of shears. It just carries its tier, so {@code ShearsStats} and the repair/enchant
 * tags can find it, and so the vanilla-shears widening mixins can recognise it as a real pair of shears. Its
 * scaled mining speed lives in the {@code TOOL} component set when the item is created (see {@code ModItems}),
 * not in this class. Extending {@code ShearsItem} (rather than plain {@code Item}) is also what makes NeoForge's
 * own shears widening (canPerformAction, IShearable) treat this like any other pair of shears for free.
 */
public class TieredShearsItem extends ShearsItem {

    private final ModTiers tier;

    public TieredShearsItem(ModTiers tier, Properties properties) {
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
