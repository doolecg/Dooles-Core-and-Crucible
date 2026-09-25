package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.SmithingBook;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.ClickType;
*///?}

/**
 * The smithing recipe book's hooks, mirroring what vanilla's {@code AbstractRecipeBookScreen} overrides:
 * drawing, ghost recipe, clicks, keys, hover and outside-click rules, slot clicks and ticking. Each one does nothing
 * unless this is the smithing screen that owns the book ({@link SmithingBook}).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    // Every hook below asks SmithingBook first. SmithingBook returns false / null / does nothing for any screen that isn't
    // the smithing table, so vanilla behaviour is untouched everywhere else. Returning a value cancels vanilla's handling
    // (the book used the click or key).
    //? if >=1.21.2 {
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    // Draw the book just before the item held on the cursor, so the cursor item stays on top.
    private void dooles_core_crucible$renderBook(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SmithingBook.render(this, graphics, mouseX, mouseY, a);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dooles_core_crucible$bookTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SmithingBook.renderTooltip(this, graphics, mouseX, mouseY);
    }

    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    // On a narrow window the open book covers the table: draw only the background, not the table's contents.
    private void dooles_core_crucible$narrowBook(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (SmithingBook.hidesContents(this)) {
            ((Screen) (Object) this).extractBackground(graphics, mouseX, mouseY, a);
            ci.cancel();
        }
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    // Faded "ghost" items in the table's slots, showing what a picked recipe still needs.
    private void dooles_core_crucible$ghostRecipe(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        SmithingBook.renderGhost(this, graphics);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$bookClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.mouseClicked(this, event, doubleClick)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$bookDrag(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.mouseDragged(this, event, dx, dy)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$bookKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.keyPressed(this, event)) cir.setReturnValue(true);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$outside(double mouseX, double mouseY, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
        Boolean outside = SmithingBook.hasClickedOutside(this, mouseX, mouseY);
        if (outside != null) cir.setReturnValue(outside);
    }

    // No arguments: the button parameter differs between Fabric (MouseButtonEvent) and NeoForge (int). The clicked
    // slot is the hovered one.
    @Inject(method = "slotClicked", at = @At("TAIL"))
    private void dooles_core_crucible$bookSlotClicked(CallbackInfo ci) {
        SmithingBook.slotClicked(this, ((AbstractContainerScreenAccessor) this).dooles_core_crucible$hoveredSlot());
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void dooles_core_crucible$bookTick(CallbackInfo ci) {
        SmithingBook.tick(this);
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$narrowBook(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (SmithingBook.hidesContents(this)) {
            ((Screen) (Object) this).renderBackground(graphics, mouseX, mouseY, partialTick);
            SmithingBook.render(this, graphics, mouseX, mouseY, partialTick);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dooles_core_crucible$renderBook(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SmithingBook.render(this, graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$bookClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.mouseClicked(this, mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$bookKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.keyPressed(this, keyCode, scanCode, modifiers)) cir.setReturnValue(true);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$outside(double mouseX, double mouseY, int xo, int yo, int button, CallbackInfoReturnable<Boolean> cir) {
        Boolean outside = SmithingBook.hasClickedOutside(this, mouseX, mouseY);
        if (outside != null) cir.setReturnValue(outside);
    }

    @Inject(method = "slotClicked", at = @At("TAIL"))
    private void dooles_core_crucible$bookSlotClicked(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        SmithingBook.slotClicked(this, slot);
    }
    *///?}

    // A narrow window shows the open book over the table, so the table's slots don't react to the mouse.
    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$noHoverUnderBook(int left, int top, int w, int h, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingBook.hidesContents(this)) cir.setReturnValue(false);
    }
}
