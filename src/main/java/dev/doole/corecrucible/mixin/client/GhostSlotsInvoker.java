package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// The smithing recipe book fills vanilla's ghost slots from outside its package (SmithingRecipeBookComponent).
@Mixin(GhostSlots.class)
public interface GhostSlotsInvoker {
    @Invoker("setInput")
    void dooles_core_crucible$setInput(Slot slot, ContextMap context, SlotDisplay contents);

    @Invoker("setResult")
    void dooles_core_crucible$setResult(Slot slot, ContextMap context, SlotDisplay contents);
}
//?}
