package dev.doole.corecrucible.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Invoker mixin: calls protected methods of vanilla's container menu from outside it. The catalyst slot uses these to
// add itself to the smithing table, hand its item back when the table closes, and handle shift-clicks.
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addSlot")
    Slot dooles_core_crucible$addSlot(Slot slot);

    @Invoker("clearContainer")
    void dooles_core_crucible$clearContainer(Player player, Container container);

    @Invoker("moveItemStackTo")
    boolean dooles_core_crucible$moveItemStackTo(ItemStack stack, int start, int end, boolean backwards);
}
