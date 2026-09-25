package dev.doole.corecrucible.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/** Implemented by {@code SmithingMenu} through {@code SmithingMenuMixin}. */
public interface CatalystMenu {
    // The one-item container behind the catalyst slot.
    Container dooles_core_crucible$catalyst();

    Slot dooles_core_crucible$catalystSlot();
}
