package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.gameplay.Catalysts;
import dev.doole.corecrucible.inventory.CatalystMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The smithing catalyst slot: hand the catalyst back on close, and shift-click catalysts into it.
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {
    @Shadow
    @Final
    protected ContainerLevelAccess access;

    // When the smithing table closes, give the catalyst back to the player (or drop it if their inventory is full),
    // the same way vanilla returns the other input slots.
    @Inject(method = "removed", at = @At("TAIL"))
    private void dooles_core_crucible$returnCatalyst(Player player, CallbackInfo ci) {
        if (this instanceof CatalystMenu menu) {
            access.execute((level, pos) -> ((AbstractContainerMenuAccessor) this).dooles_core_crucible$clearContainer(player, menu.dooles_core_crucible$catalyst()));
        }
    }

    // Shift-click handling. Only takes over for the catalyst slot's own moves; everything else falls through to vanilla.
    // Moves out of the catalyst slot go to the player's inventory; catalyst items shift-clicked from the inventory go
    // into the catalyst slot, if it's currently showing.
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$shiftClickCatalyst(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!(this instanceof CatalystMenu menu)) return;
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        Slot target = menu.dooles_core_crucible$catalystSlot();
        Slot slot = self.slots.get(index);
        if (!slot.hasItem()) return;
        ItemStack stack = slot.getItem();
        int start;
        int end;
        boolean backwards;
        if (slot == target) {
            // Out of the catalyst slot: into the player's inventory, hotbar first.
            start = -1;
            end = -1;
            for (Slot s : self.slots) {
                if (s.container instanceof Inventory) {
                    if (start < 0) start = s.index;
                    end = s.index + 1;
                }
            }
            if (start < 0) return;
            backwards = true;
        } else {
            if (!(slot.container instanceof Inventory) || !Catalysts.isCatalyst(stack) || !target.isActive()) return;
            start = target.index;
            end = target.index + 1;
            backwards = false;
        }
        // moveItemStackTo shrinks the stack as it moves it. Vanilla's contract: return EMPTY if nothing moved, or a copy
        // of the stack as it was before the move if something did.
        ItemStack original = stack.copy();
        if (!((AbstractContainerMenuAccessor) self).dooles_core_crucible$moveItemStackTo(stack, start, end, backwards)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        cir.setReturnValue(original);
    }
}
