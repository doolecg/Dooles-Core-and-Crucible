package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.VeinMiningClient;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
*///?}

// Draws the Vein Resonance shape selector under the crosshair while its key is held.
//? if >=1.21.2 {
@Mixin(Hud.class)
//?} else {
/*@Mixin(Gui.class)
*///?}
public abstract class HudMixin {
    //? if >=1.21.2 {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dooles_core_crucible$veinMenu(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void dooles_core_crucible$veinMenu(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    *///?}
        // The chosen shape, then a row of pips and the block count, centred just under the crosshair. Empty when the key
        // isn't held or the tool has no Vein Resonance.
        List<Component> lines = VeinMiningClient.hudLines();
        if (lines.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        for (Component line : lines) {
            //? if >=1.21.2 {
            graphics.centeredText(font, line, x, y, 0xFFFFFFFF);
            //?} else {
            /*graphics.drawCenteredString(font, line, x, y, 0xFFFFFFFF);
            *///?}
            y += font.lineHeight + 2;
        }
    }
}
