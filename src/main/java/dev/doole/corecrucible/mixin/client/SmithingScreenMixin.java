package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.SmithingBook;
import dev.doole.corecrucible.inventory.CatalystMenu;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

// Draws the catalyst slot's frame while the slot is shown; the texture has no slot there.
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin {
    //? if >=1.21.2 {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void dooles_core_crucible$catalystSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "renderBg", at = @At("TAIL"))
    private void dooles_core_crucible$catalystSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
    *///?}
        SmithingScreen self = (SmithingScreen) (Object) this;
        if (!(self.getMenu() instanceof CatalystMenu menu)) return;
        Slot slot = menu.dooles_core_crucible$catalystSlot();
        if (slot == null || !slot.isActive()) return;
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        int x = screen.dooles_core_crucible$leftPos() + slot.x - 1;
        int y = screen.dooles_core_crucible$topPos() + slot.y - 1;
        // A vanilla-style slot: dark top-left edge, white bottom-right edge, grey inside.
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    // The smithing recipe book's tooltip, after the table's own tooltips. 26.x draws it in AbstractContainerScreenMixin.
    //? if <1.21.2 {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void dooles_core_crucible$bookTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SmithingBook.renderTooltip(this, graphics, mouseX, mouseY);
    }
    *///?}
}
