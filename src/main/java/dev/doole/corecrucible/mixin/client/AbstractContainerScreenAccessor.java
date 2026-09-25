package dev.doole.corecrucible.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Accessor mixin: exposes private fields of vanilla's container screen (its position, size and the slot under the
// mouse) so the smithing recipe book can lay itself out next to the smithing table and shift the table over.
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    // Screen x position (left edge of the GUI texture), readable and writable.
    @Accessor("leftPos")
    int dooles_core_crucible$leftPos();

    @Accessor("leftPos")
    void dooles_core_crucible$setLeftPos(int leftPos);

    // Screen y position (top edge of the GUI texture).
    @Accessor("topPos")
    int dooles_core_crucible$topPos();

    @Accessor("imageWidth")
    int dooles_core_crucible$imageWidth();

    @Accessor("imageHeight")
    int dooles_core_crucible$imageHeight();

    // The slot the mouse is over, or null.
    @Accessor("hoveredSlot")
    Slot dooles_core_crucible$hoveredSlot();
}
