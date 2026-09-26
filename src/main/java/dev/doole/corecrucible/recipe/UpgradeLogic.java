package dev.doole.corecrucible.recipe;

import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Builds the result of a tier upgrade. The new piece keeps the old one's name, lore, enchantments, repair cost and
 * trim ({@link #KEPT}). Wear is not carried over: the upgraded piece comes out at full durability.
 */
public final class UpgradeLogic {

    /** Components copied as-is from the old piece to the upgraded one. */
    private static final List<DataComponentType<?>> KEPT = List.of(
            DataComponents.CUSTOM_NAME,
            DataComponents.LORE,
            DataComponents.ENCHANTMENTS,
            DataComponents.REPAIR_COST,
            DataComponents.TRIM);

    private UpgradeLogic() {
    }

    /** Upgrades {@code base} into {@code result}. */
    public static ItemStack upgrade(ItemStack base, Item result) {
        ItemStack out = new ItemStack(result);
        for (DataComponentType<?> type : KEPT) copy(base, out, type);
        return out;
    }

    private static <T> void copy(ItemStack from, ItemStack to, DataComponentType<T> type) {
        T value = from.get(type);
        if (value != null) to.set(type, value);
    }
}
