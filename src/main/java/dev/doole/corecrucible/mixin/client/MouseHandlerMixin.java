package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.VeinMiningClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While the Vein Resonance key is held, the mouse wheel picks a shape instead of a hotbar slot.
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    // Cancelling the scroll stops vanilla from also changing the selected hotbar slot.
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$veinShapeScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (VeinMiningClient.scroll(yOffset)) ci.cancel();
    }
}
