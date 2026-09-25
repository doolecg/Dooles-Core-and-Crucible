package dev.doole.corecrucible.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Accessor mixin for the smithing table (an ItemCombinerMenu): gives the mod its input slots (template, base, addition)
// and its result slot, which vanilla keeps protected.
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("inputSlots")
    Container dooles_core_crucible$inputSlots();

    @Accessor("resultSlots")
    ResultContainer dooles_core_crucible$resultSlots();
}
