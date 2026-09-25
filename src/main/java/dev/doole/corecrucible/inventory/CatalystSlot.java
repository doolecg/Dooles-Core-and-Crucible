package dev.doole.corecrucible.inventory;

import dev.doole.corecrucible.gameplay.Catalysts;
import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The smithing-table catalyst slot. Hidden unless the base item's next step needs a catalyst. */
public class CatalystSlot extends Slot {
    /** Position inside the smithing screen: above the addition slot. */
    public static final int X = 44;
    public static final int Y = 26;

    private final Supplier<ItemStack> base;

    public CatalystSlot(Container container, Supplier<ItemStack> base) {
        super(container, 0, X, Y);
        this.base = base;
    }

    @Override
    public boolean isActive() {
        // Stays visible while it holds something, so a catalyst is never stuck out of reach.
        return hasItem() || Catalysts.wanted(base.get());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return Catalysts.isCatalyst(stack) && isActive();
    }
}
